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
import ru.service.AppointmentService;
import ru.service.NotificationService;
import ru.service.UserService;
import ru.service.UserSessionService;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public void handleTextMessage(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();
        String role = userSessionService.getRole(chatId);

        if (CMD_ADMIN.equalsIgnoreCase(text)) {
            if (userService.isAdmin(chatId)) {
                notificationService.sendAdminMenu(chatId, "🔐 *Админ-панель*");
            } else {
                notificationService.sendMessage(chatId, "❌ У вас нет доступа к админ-панели.");
            }
            return;
        }

        String userState = appointmentService.getUserState(chatId);
        AdminAppointmentState adminState = appointmentService.getAdminState(chatId);

        if (CMD_START.equalsIgnoreCase(text) || CMD_BEGIN.equalsIgnoreCase(text)) {
            sendWelcome(chatId);
            return;
        }

        // Обработка обычного пользователя
        if (STATE_AWAITING_NAME.equals(userState)) {
            handleUserName(chatId, text, message.getMessageId());
            return;
        }
        if (STATE_AWAITING_PHONE.equals(userState)) {
            handleUserPhone(chatId, text, message.getMessageId(), false);
            return;
        }

        // Обработка админа
        if ("ADMIN".equals(role)) {
            switch (adminState) {
                case AWAITING_NAME -> handleUserName(chatId, text, message.getMessageId());
                case AWAITING_PHONE -> handleUserPhone(chatId, text, message.getMessageId(), true);
                default -> notificationService.sendAdminMenu(chatId, "🔐 *Админ-панель*");
            }
            return;
        }

        // Неизвестная команда → показать главное меню
        notificationService.sendMainMenu(chatId, "Выберите действие:");
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
            appointmentService.setAdminState(chatId, AdminAppointmentState.AWAITING_DATE);
            sendDateSelectionForAdmin(chatId); // ← новый метод
        } else {
            appointmentService.setUserState(chatId, STATE_AWAITING_PHONE);
            Message sentMessage = notificationService.sendMessageAndReturn(chatId,
                    "Спасибо, *%s*! Теперь введите номер телефона 📱".formatted(name),
                    keyboardFactory.backButton("⬅️ Назад", "back_to_dates")
            );
            appointmentService.setPendingMessageId(chatId, sentMessage.getMessageId());
        }
    }

    private void handleUserPhone(Long chatId, String phone, Integer messageId, boolean isAdminFlow) {
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
                user = userService.updateUserPhone(chatId, phone); // привязывает к текущему chatId
            }

            Appointment appointment = Appointment.builder()
                    .user(user)
                    .dateTime(dateTime)
                    .status(StatusAppointment.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

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
                appointmentService.setAdminState(chatId, AdminAppointmentState.AWAITING_DATE);
                sendDateSelection(chatId, null);
            } else {
                appointmentService.setUserState(chatId, STATE_AWAITING_DATE);
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
        appointmentService.setUserState(chatId, STATE_AWAITING_DATE);
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

        InlineKeyboardMarkup markup = keyboardFactory.dateSelectionKeyboard(availableDates);
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

    public void sendDateSelectionForAdmin(Long chatId) {
        LocalDate today = LocalDate.now();
        List<LocalDate> availableDates = new ArrayList<>();
        for (int i = 0; i < 14; i++) { // даём больше дней админу
            LocalDate date = today.plusDays(i);
            if (appointmentService.isWorkingDay(date)) {
                List<LocalDateTime> slots = appointmentService.getAvailableTimeSlots(date.atStartOfDay());
                if (!slots.isEmpty()) {
                    availableDates.add(date);
                }
            }
        }

        InlineKeyboardMarkup markup = keyboardFactory.dateSelectionKeyboard(availableDates);
        notificationService.sendOrEditMessage(chatId, null, "📅 Выберите дату для клиента:", markup);
    }
}
