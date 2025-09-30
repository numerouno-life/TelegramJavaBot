package ru.bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.AdminAppointmentState;
import ru.model.enums.StatusAppointment;
import ru.model.enums.UserAppointmentState;
import ru.model.enums.UserRole;
import ru.service.*;
import ru.util.AdminKeyboard;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.util.BotConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextMessageHandler {

    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final KeyboardFactory keyboardFactory;
    private final UserService userService;
    private final UserSessionService userSessionService;
    private final WorkScheduleService workScheduleService;
    private final AdminKeyboard adminKeyboard;
    private final AdminCallbackHandler adminCallbackHandler;

    public void handleTextMessage(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();
        String role = userSessionService.getRole(chatId);
        User from = userService.getOrCreateUser(
                chatId,
                message.getFrom().getUserName(),
                message.getFrom().getFirstName(),
                message.getFrom().getLastName()
        );

        if (CMD_ADMIN.equalsIgnoreCase(text)) {
            if (userService.isAdmin(chatId)) {
                userSessionService.setRole(chatId, "ADMIN");
                notificationService.sendAdminMenu(chatId, "🔐 *Админ-панель*");
            } else {
                notificationService.sendMessage(chatId, "❌ У вас нет доступа к админ-панели.");
            }
            return;
        }

        UserAppointmentState userState = appointmentService.getUserState(chatId);
        AdminAppointmentState adminState = appointmentService.getAdminState(chatId);
        log.debug("👑 Admin mode: chatId={}, adminState={}", chatId, adminState);
        log.debug("👤 User state: {}", userState);

        if (CMD_START.equalsIgnoreCase(text) || CMD_BEGIN.equalsIgnoreCase(text)) {
            sendWelcome(chatId);
            return;
        }

        // Обработка обычного пользователя
        if (UserAppointmentState.STATE_AWAITING_NAME.equals(userState)) {
            handleUserName(chatId, text, message.getMessageId());
            return;
        }
        if (UserAppointmentState.STATE_AWAITING_PHONE.equals(userState)) {
            handleUserPhone(chatId, text, message.getMessageId(), false, message.getFrom().getUserName());
            return;
        }

        // Обработка админа
        if ("ADMIN".equals(role)) {
            log.debug("👑 Admin mode: chatId={}, adminState={}", chatId, adminState);
            switch (adminState) {
                case ADM_AWAITING_NAME -> handleUserName(chatId, text, message.getMessageId());
                case ADM_AWAITING_PHONE -> handleUserPhone(chatId, text, message.getMessageId(),
                        true, message.getFrom().getUserName());
                case AWAITING_OVERRIDE_DATE -> handleAdminOverrideDate(chatId, text);
                case AWAITING_OVERRIDE_TIME -> handleAdminOverrideTime(chatId, text);
                case AWAITING_OVERRIDE_REASON -> handleAdminOverrideReason(chatId, text);
                default -> notificationService.sendAdminMenu(chatId, "🔐 *Админ-панель*");
            }
            return;
        }

        // Неизвестная команда → показать главное меню
        notificationService.sendMainMenu(chatId, "Выберите действие:");
    }

    private void handleAdminOverrideReason(Long chatId, String text) {
        LocalDate date = userSessionService.getPendingDate(chatId).toLocalDate();
        String reason = "-".equals(text.trim()) ? "" : text.trim();

        if ("false".equals(userSessionService.getPendingName(chatId))) {
            // выходной
            workScheduleService.setWorkDayOverride(date, null, null, false, reason);
        } else {
            // рабочий день
            LocalTime start = userSessionService.getPendingStartTime(chatId);
            LocalTime end = userSessionService.getPendingEndTime(chatId);
            workScheduleService.setWorkDayOverride(date, start, end, true, reason);
        }
        notificationService.sendMessage(chatId, "✅ Исключение добавлено!");
        userSessionService.clearAdminState(chatId);
        notificationService.sendOrEditMessage(chatId, null,
                "🔐 *Админ-панель*", adminKeyboard.getMainAdminMenu());
    }

    private void handleAdminOverrideTime(Long chatId, String text) {
        if ("выходной".equalsIgnoreCase(text.trim())) {
            userSessionService.setAdminState(chatId, AdminAppointmentState.AWAITING_OVERRIDE_REASON);
            userSessionService.setPendingName(chatId, "false");
            notificationService.sendMessage(chatId, "📝 Введите причину (или '-'):");
        } else {
            try {
                String[] parts = text.split("-");
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());
                userSessionService.setPendingStartTime(chatId, start);
                userSessionService.setPendingEndTime(chatId, end);
                userSessionService.setAdminState(chatId, AdminAppointmentState.AWAITING_OVERRIDE_REASON);
                notificationService.sendMessage(chatId, "📝 Введите причину (или '-'):");
            } catch (Exception e) {
                notificationService.sendMessage(chatId, "❌ Неверный формат времени. Попробуйте: 10:00-18:00");
            }
        }
    }

    private void handleAdminOverrideDate(Long chatId, String dateString) {
        try {
            LocalDate date = LocalDate.parse(dateString);
            userSessionService.setPendingDate(chatId, date.atStartOfDay());
            userSessionService.setAdminState(chatId, AdminAppointmentState.AWAITING_OVERRIDE_TIME);
            notificationService.sendMessage(chatId, "⏰ Введите время в формате ЧЧ:ММ-ЧЧ:ММ (например, 10:00-18:00)\n" +
                    "Или отправьте 'выходной'.");
        } catch (Exception e) {
            notificationService.sendMessage(chatId, "❌ Неверный формат даты. Попробуйте снова: ГГГГ-ММ-ДД");
        }

    }

    private void sendWelcome(Long chatId) {
        String welcome = """
                👋 Добро пожаловать в салон красоты *SH*!
                
                Вы можете:
                • Записаться на стрижку
                • Посмотреть свои актуальные записи
                • Посмотреть историю записей
                • Узнать контакты
                """;
        notificationService.sendMainMenu(chatId, welcome);
    }

    private void handleUserName(Long chatId, String name, Integer messageId) {
        deletePendingMessage(chatId, messageId);
        userSessionService.setPendingName(chatId, name);

        String role = userSessionService.getRole(chatId);
        if ("ADMIN".equals(role)) {
            deletePendingMessage(chatId, messageId);
            appointmentService.setAdminState(chatId, AdminAppointmentState.ADM_AWAITING_PHONE);
            Message sent = notificationService.sendMessageAndReturn(chatId,
                    "📞 Введите номер телефона клиента:", null);
            appointmentService.setPendingMessageId(chatId, sent.getMessageId());
        } else {
            appointmentService.setUserState(chatId, UserAppointmentState.STATE_AWAITING_PHONE);
            Message sentMessage = notificationService.sendMessageAndReturn(chatId,
                    "Спасибо, *%s*! Теперь введите номер телефона 📱".formatted(name),
                    keyboardFactory.backButton("⬅️ Назад", "back_to_dates")
            );
            appointmentService.setPendingMessageId(chatId, sentMessage.getMessageId());
        }
    }

    private void handleUserPhone(Long chatId, String phone, Integer messageId,
                                 boolean isAdminFlow, String telegramUsername) {
        log.info("📞 handleUserPhone вызван: chatId={}, isAdminFlow={}, adminState={}",
                chatId, isAdminFlow, appointmentService.getAdminState(chatId));
        deletePendingMessage(chatId, messageId);

        String name = userSessionService.getPendingName(chatId);
        LocalDateTime dateTime = appointmentService.getPendingDate(chatId);

        if (dateTime == null) {
            notificationService.sendMessage(chatId, "❌ Ошибка: дата не выбрана. Начните заново.");
            cleanupAfterError(chatId, isAdminFlow);
            return;
        }

        try {
            User user;
            if (isAdminFlow) {
                user = userService.findOrCreateByPhone(phone, name);
            } else {
                user = userService.updateUserDetails(chatId, name, phone); // привязывает к текущему chatId
            }

            Appointment appointment = Appointment.builder()
                    .user(user)
                    .dateTime(dateTime)
                    .status(StatusAppointment.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            log.info("Сохранённая запись: ID={}, Клиент={}, Дата={}, Статус={}",
                    appointment.getId(),
                    appointment.getUser().getFirstName(),
                    appointment.getDateTime(),
                    appointment.getStatus());

            appointmentService.createAppointment(appointment);

            // ✅ Отправляем уведомление клиенту (если известен его chatId)
            notifyClientIfPossible(user, appointment, chatId);


            // Завершаем сессию
            appointmentService.clearUserState(chatId);
            appointmentService.clearAdminState(chatId);
            userSessionService.clearRole(chatId);
            userSessionService.clearPendingName(chatId);
            appointmentService.clearPendingDate(chatId);

            if (isAdminFlow) {
                notificationService.sendAdminMenu(chatId, "✅ Клиент *%s* успешно записан на %s.".formatted(
                        name,
                        dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'в' HH:mm"))
                ));
            } else {
                notificationService.sendOrEditMessage(chatId, null,
                        "✅ Вы успешно записаны на %s!".formatted(
                                dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'в' HH:mm"))
                        ), null);
                notificationService.sendMainMenu(chatId, "Выберите действие:");
            }

        } catch (IllegalStateException e) {
            notificationService.sendMessage(chatId, "❌ Время уже занято. Выберите новое.");

            if (isAdminFlow) {
                appointmentService.setAdminState(chatId, AdminAppointmentState.ADM_AWAITING_DATE);
                sendDateSelection(chatId, null);
            } else {
                appointmentService.setUserState(chatId, UserAppointmentState.STATE_AWAITING_PHONE);
                sendDateSelection(chatId, null);
            }
        }
    }

    // Дополнительная функция: отправка уведомления клиенту, если он когда-то писал боту
    private void notifyClientIfPossible(User user, Appointment appointment, Long adminChatId) {
        if (user.getTelegramId() != null) {
            try {
                notificationService.sendMessage(user.getTelegramId(), """
                        📢 Администратор записал вас на %s.
                        
                        Стрижка состоится:
                        📅 %s
                        ⏰ %s
                        
                        Если не сможете прийти — отмените запись в меню.
                        """.formatted(
                        appointment.getDateTime().format(DATE_FORMAT),
                        appointment.getDateTime().format(DATE_FORMAT),
                        appointment.getDateTime().format(TIME_FORMAT)
                ));
            } catch (Exception e) {
                log.warn("Не удалось уведомить пользователя {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    private void deletePendingMessage(Long chatId, Integer messageId) {
        Integer pendingMessageId = appointmentService.getPendingMessageId(chatId);
        if (pendingMessageId != null) {
            notificationService.deleteMessage(chatId, pendingMessageId);
            appointmentService.clearPendingMessageId(chatId);
        }
        if (messageId != null) {
            notificationService.deleteMessage(chatId, messageId);
        }
    }

    public void startAppointmentProcess(Long chatId, Integer messageId) {
        if (messageId != null) {
            notificationService.deleteMessage(chatId, messageId);
        }
        appointmentService.setUserState(chatId, UserAppointmentState.STATE_AWAITING_DATE);
        sendDateSelection(chatId, null);
    }

    public void sendDateSelection(Long chatId, Integer messageId) {
        if (messageId != null) {
            notificationService.deleteMessage(chatId, messageId);
        }

        LocalDate today = LocalDate.now();
        List<LocalDate> availableDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            if (appointmentService.isWorkingDay(date)) {
                List<LocalDateTime> slots = appointmentService.getAvailableTimeSlots(date.atStartOfDay());
                if (!slots.isEmpty()) availableDates.add(date);
            }
        }

        InlineKeyboardMarkup markup = keyboardFactory.dateSelectionKeyboard(availableDates, UserRole.USER);
        notificationService.sendOrEditMessage(chatId, messageId, "Выберите дату записи:", markup);
    }

    // Чистка состояния при ошибках
    private void cleanupAfterError(Long chatId, boolean isAdminFlow) {
        appointmentService.clearUserState(chatId);
        if (isAdminFlow) appointmentService.clearAdminState(chatId);
        userSessionService.clearRole(chatId);
        userSessionService.clearPendingName(chatId);
        appointmentService.clearPendingDate(chatId);
    }
}
