package ru.bot.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bot.handler.TextMessageHandler;
import ru.bot.handler.UserCallBackHandler;
import ru.model.Appointment;
import ru.model.enums.*;
import ru.service.*;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.util.BotConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCallbackHandlerImpl implements UserCallBackHandler {
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final TextMessageHandler textMessageHandler;
    private final KeyboardFactory keyboardFactory;
    private final UserService userService;
    private final AdminService adminService;
    private final UserSessionService userSessionService;

    public static final int PAGE_SIZE_FIVE = 5;

    @Override
    public void handleUserCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        if (userService.isBlocked(chatId)) {
            notificationService.sendMessage(chatId, "❌ Ваш аккаунт заблокирован. Обратитесь к администратору.");
            return;
        }

        log.debug("Processing user callback: data='{}', type={}", data, CallbackType.fromString(data));

        try {
            CallbackType type = CallbackType.fromString(data);
            switch (type) {
                case DATE -> handleDateSelection(chatId, messageId, data);
                case TIME -> handleTimeSelection(chatId, messageId, data);
                case BACK_TO_MENU -> handleBackToMenu(chatId);
                case BOOK_APPOINTMENT -> handleBookAppointment(chatId, messageId);
                case MY_APPOINTMENTS -> handleMyAppointments(chatId);
                case CONTACTS -> handleContacts(chatId);
                case CANCEL -> handleCancelAppointment(chatId, messageId, data);
                case BACK_TO_DATES -> handleBackToDates(chatId, messageId);

                case HISTORY -> showPastAppointments(chatId, messageId, 0);
                case HISTORY_PAGE -> {
                    try {
                        int page = Integer.parseInt(data.substring("history_page_".length()));
                        showPastAppointments(chatId, messageId, page);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid page number in callback: {}", data);
                        notificationService.sendOrEditMessage(chatId, messageId, "❌ Ошибка при загрузке страницы.", null);
                    }
                }

                case UNKNOWN -> log.warn("Unknown user callback: {}", data);
                default -> log.debug("User callback not handled: {}", data);
            }
        } catch (Exception e) {
            log.error("Error handling user callback query: {}", data, e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Произошла ошибка. Попробуйте снова.", null);
        }
    }

    private void sendTimeSelection(Long chatId, Integer messageId, LocalDate date) {
        notificationService.deleteMessage(chatId, messageId);
        List<LocalDateTime> availableSlots = appointmentService.getAvailableTimeSlots(date.atStartOfDay())
                .stream()
                .filter(slot -> slot.isAfter(LocalDateTime.now()))
                .toList();

        InlineKeyboardMarkup markup = keyboardFactory.timeSelectionKeyboard(date, availableSlots, UserRole.USER);

        Message sentMessage = notificationService.sendMessageAndReturn(chatId,
                "Доступное время на " + date.format(DateTimeFormatter.ofPattern("dd.MM (E)")) + ":\n🟢 - свободно",
                markup);

        appointmentService.setPendingMessageId(chatId, sentMessage.getMessageId());
    }

    private void showLastAppointment(Long chatId) {
        Optional<Appointment> lastOptional = appointmentService.getLastAppointment(chatId);

        if (lastOptional.isEmpty()) {
            notificationService.sendMessage(chatId, "У вас пока нет записей.");
            return;
        }

        Appointment last = lastOptional.get();
        LocalDateTime lastDateTime = last.getDateTime();
        LocalDateTime now = LocalDateTime.now();
        InlineKeyboardMarkup backButton = keyboardFactory.backButton("🏠 В меню", "back_to_menu");

        if (lastDateTime.isBefore(now)) {
            // Прошедшая запись — кнопка отмены не нужна
            notificationService.sendOrEditMessage(chatId, null,
                    "❌ Вы уже были записаны на стрижку.\n" +
                            "📅 Последняя запись: " + lastDateTime.format(DATE_FORMAT) +
                            " в " + lastDateTime.format(TIME_FORMAT) +
                            "\nПовторная запись возможна только через 6 дней после предыдущей.",
                    backButton
            );
        } else {
            // Активная запись — кнопка отмены доступна
            InlineKeyboardMarkup markup = keyboardFactory.userCancelAppointmentButton(last.getId(), lastDateTime);
            notificationService.sendOrEditMessage(chatId, null,
                    "❌ Вы уже записаны на стрижку.\n" +
                            "📅 Последняя запись: " + lastDateTime.format(DATE_FORMAT) +
                            " в " + lastDateTime.format(TIME_FORMAT) +
                            "\nВы можете отменить текущую запись, если хотите перенести её.",
                    markup
            );
        }
    }

    private void showPastAppointments(Long chatId, Integer messageId, int page) {
        List<Appointment> past = appointmentService.getPastAppointments(chatId);
        if (past.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, messageId, "У вас нет прошлых записей.", null);
            return;
        }

        int totalPages = (int) Math.ceil((double) past.size() / PAGE_SIZE_FIVE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE_FIVE;
        int end = Math.min(start + PAGE_SIZE_FIVE, past.size());
        List<Appointment> subList = new ArrayList<>(past.subList(start, end));

        StringBuilder sb = new StringBuilder("📖 История записей (стр. ")
                .append(page + 1).append("/").append(totalPages).append("):\n\n");

        for (Appointment app : subList) {
            String status = app.getStatus() == StatusAppointment.CANCELED ? "❌ Отменена" : "✅ Завершена";
            sb.append("📅 ").append(app.getDateTime().format(DATE_FORMAT))
                    .append(" - ").append(app.getDateTime().format(TIME_FORMAT))
                    .append("\n").append(status)
                    .append("\n📞 ").append(app.getUser().getClientPhoneNumber())
                    .append("\n\n");
        }

        InlineKeyboardMarkup markup = keyboardFactory.historyPagination(page, totalPages, "history_page");
        notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);
    }

    private void handleDateSelection(Long chatId, Integer messageId, String data) {
        LocalDate selectedDate = LocalDate.parse(data.substring(5));

        String role = userSessionService.getRole(chatId);
        AdminAppointmentState adminState = appointmentService.getAdminState(chatId);

        boolean isAdminFlow = "ADMIN".equals(role) &&
                (adminState == AdminAppointmentState.ADM_AWAITING_DATE);

        if (isAdminFlow) {
            deletePendingMessage(chatId, messageId);
            adminService.sendTimeSelectionForAdmin(chatId, null, selectedDate);
        } else {
            sendTimeSelection(chatId, messageId, selectedDate);
        }
    }

    private void handleTimeSelection(Long chatId, Integer messageId, String data) {
        LocalDateTime selectedTime = LocalDateTime.parse(data.substring(5));
        notificationService.deleteMessage(chatId, messageId);
        appointmentService.setPendingDate(chatId, selectedTime);
        String role = userSessionService.getRole(chatId);
        AdminAppointmentState adminState = appointmentService.getAdminState(chatId);
        boolean isAdminFlow = "ADMIN".equals(role) &&
                (adminState == AdminAppointmentState.ADM_AWAITING_DATE);
        if (!isAdminFlow) {
            if (appointmentService.hasAppointmentInLast6Days(chatId, selectedTime)) {
                Appointment last = appointmentService.getLastAppointmentWithin6Days(chatId, selectedTime);
                String existingTime = last.getDateTime().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy 'в' HH:mm")
                );
                String message = """
                ❌ Вы уже были записаны на стрижку.
                
                📅 Последняя запись: %s
                
                Повторная запись возможна только через 6 дней после предыдущей.
                Отмените текущую запись, если хотите перенести её.
                """.formatted(existingTime);

                notificationService.sendOrEditMessage(chatId, null, message, null);
                userSessionService.clearUserState(chatId);
                appointmentService.clearPendingDate(chatId);
                notificationService.sendMainMenu(chatId, "Выберите действие:");
                return;
            }
        }

        if (isAdminFlow) {
            deletePendingMessage(chatId, messageId);
            appointmentService.setAdminState(chatId, AdminAppointmentState.ADM_AWAITING_NAME);
            log.info("✅ Админ-состояние изменено на ADM_AWAITING_NAME для chatId={}", chatId);
            var sent = notificationService.sendMessageAndReturn(chatId,
                    "👤 Введите имя клиента:", null);
            appointmentService.setPendingMessageId(chatId, sent.getMessageId());
        } else {
            appointmentService.setUserState(chatId, UserAppointmentState.STATE_AWAITING_NAME);
            var sentMessage = notificationService.sendMessageAndReturn(chatId,
                    "Вы выбрали: " + selectedTime.format(DATE_FORMAT) + " - "
                            + selectedTime.format(TIME_FORMAT) + "\n\nВведите ваше имя:",
                    keyboardFactory.backButton("⬅️ Назад", "back_to_dates")
            );
            appointmentService.setPendingMessageId(chatId, sentMessage.getMessageId());
        }
    }

    private void handleBackToMenu(Long chatId) {
        Integer messageId = appointmentService.getPendingMessageId(chatId);
        if (messageId != null) {
            notificationService.deleteMessage(chatId, messageId);
            appointmentService.clearPendingMessageId(chatId);
        }
        userSessionService.clearAllSessions(chatId);
        userSessionService.clearRole(chatId);
        List<Appointment> appointments = appointmentService.getUserAppointments(chatId);
        String text = appointments.isEmpty() ? "Добро пожаловать!" : "Выберите действие:";
        notificationService.sendMainMenu(chatId, text);
    }

    private void handleBookAppointment(Long chatId, Integer messageId) {
        textMessageHandler.startAppointmentProcess(chatId, messageId);
    }

    private void handleMyAppointments(Long chatId) {
        showLastAppointment(chatId);
    }

    private void handleContacts(Long chatId) {
        notificationService.sendContacts(chatId);
    }

    private void handleCancelAppointment(Long chatId, Integer messageId, String data) {
        Long appointmentId = Long.parseLong(data.substring(7));
        Appointment app = appointmentService.findById(appointmentId);

        if (app == null) {
            notificationService.sendOrEditMessage(chatId, messageId, "Запись не найдена", null);
            return;
        }

        if (app.getStatus() != StatusAppointment.CANCELED &&
                app.getDateTime().isAfter(LocalDateTime.now())) {

            appointmentService.cancelAppointment(app.getId());

            // Отправляем сообщение админу о том, что запись отменена
            appointmentService.cancellationNoticeForAdmins(app);

            notificationService.sendOrEditMessage(chatId, messageId,
                    "Запись на " + app.getDateTime().format(DATE_FORMAT) + " отменена ✅", null);
        } else {
            notificationService.sendOrEditMessage(chatId, messageId,
                    "Эту запись нельзя отменить", null);
        }
    }

    private void handleBackToDates(Long chatId, Integer messageId) {
        Integer pendingMessageId = appointmentService.getPendingMessageId(chatId);
        if (pendingMessageId != null) {
            notificationService.deleteMessage(chatId, pendingMessageId);
            appointmentService.clearPendingMessageId(chatId);
        }

        textMessageHandler.sendDateSelection(chatId, null);
        appointmentService.clearUserState(chatId);
    }

    private void deletePendingMessage(Long chatId, Integer messageId) {
        Integer pendingMessageId = appointmentService.getPendingMessageId(chatId);

        if (pendingMessageId != null && !pendingMessageId.equals(messageId)) {
            notificationService.deleteMessage(chatId, pendingMessageId);
            appointmentService.clearPendingMessageId(chatId);
        }

        if (messageId != null) {
            notificationService.deleteMessage(chatId, messageId);
        }
    }

}
