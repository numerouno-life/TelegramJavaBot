package ru.bot.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.model.Appointment;
import ru.model.enums.CallbackType;
import ru.model.enums.StatusAppointment;
import ru.service.AppointmentService;
import ru.service.NotificationService;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.util.BotConstants.*;

@Slf4j
@Component
@AllArgsConstructor
public class CallbackQueryHandler {

    @Autowired
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final TextMessageHandler textMessageHandler;
    private final KeyboardFactory keyboardFactory;

    public static final int PAGE_SIZE_FIVE = 5;

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        log.debug("Processing callback: data='{}', type={}", data, CallbackType.fromString(data));
        try {
            switch (CallbackType.fromString(data)) {
                case DATE -> handleDateSelection(chatId, messageId, data);
                case TIME -> handleTimeSelection(chatId, data);
                case BACK_TO_MENU -> handleBackToMenu(chatId);
                case BOOK_APPOINTMENT -> handleBookAppointment(chatId, messageId);
                case MY_APPOINTMENTS -> handleMyAppointments(chatId);
                case CONTACTS -> handleContacts(chatId);
                case CANCEL -> handleCancelAppointment(chatId, messageId, data);
                case BACK_TO_DATES -> handleBackToDates(chatId, messageId);

                case HISTORY -> {
                    showPastAppointments(chatId, messageId, 0);
                }
                case HISTORY_PAGE -> {
                    try {
                        int page = Integer.parseInt(data.substring("history_page_".length()));
                        showPastAppointments(chatId, messageId, page);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid page number in callback: {}", data);
                        notificationService.sendOrEditMessage(chatId, messageId, "❌ Ошибка при загрузке страницы.", null);
                    }
                }

                case UNKNOWN -> log.warn("Unknown callback: {}", data);
            }
        } catch (Exception e) {
            log.error("Error handling callback query: {}", data, e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Произошла ошибка. Попробуйте снова.", null);
        }
    }

    private void sendTimeSelection(Long chatId, Integer messageId, LocalDate date) {
        List<LocalDateTime> availableSlots = appointmentService.getAvailableTimeSlots(date.atStartOfDay())
                .stream()
                .filter(slot -> slot.isAfter(LocalDateTime.now()))
                .toList();

        InlineKeyboardMarkup markup = keyboardFactory.timeSelectionKeyboard(date, availableSlots);

        notificationService.sendOrEditMessage(chatId, messageId,
                "Доступное время на " + date.format(DateTimeFormatter.ofPattern("dd.MM (E)")) + ":\n🟢 - свободно",
                markup
        );
    }


    private void showActiveAppointments(Long chatId) {
        List<Appointment> active = appointmentService.getActiveAppointments(chatId);
        if (active.isEmpty()) {
            InlineKeyboardMarkup markup = keyboardFactory.backButton("⬅️ Назад", "back_to_menu");
            notificationService.sendOrEditMessage(chatId, null, "У вас нет активных записей.", markup);
            return;
        }

        for (Appointment app : active) {
            String text = "📅 " + app.getDateTime().format(DATE_FORMAT)
                    + " - " + app.getDateTime().format(TIME_FORMAT)
                    + "\n💇 Мужская стрижка"
                    + "\n📞 " + app.getUser().getClientPhoneNumber();

            log.debug("Creating cancel button for appointment id={}", app.getId());
            InlineKeyboardMarkup markup = keyboardFactory.cancelAppointmentButton(
                    app.getId(), app.getDateTime()
            );

            notificationService.sendOrEditMessage(chatId, null, text, markup);
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
        sendTimeSelection(chatId, messageId, selectedDate);
    }

    private void handleTimeSelection(Long chatId, String data) {
        LocalDateTime selectedTime = LocalDateTime.parse(data.substring(5));
        appointmentService.setPendingDate(chatId, selectedTime);
        appointmentService.setUserState(chatId, STATE_AWAITING_NAME);

        InlineKeyboardMarkup back = keyboardFactory.backButton("⬅️ Назад", "back_to_dates");
        Message sentMessage = notificationService.sendMessageAndReturn(chatId,
                "Вы выбрали: " + selectedTime.format(DATE_FORMAT) + " - "
                        + selectedTime.format(TIME_FORMAT) + "\n\nВведите ваше имя:", back
        );

        appointmentService.setPendingMessageId(chatId, sentMessage.getMessageId());
    }

    private void handleBackToMenu(Long chatId) {
        List<Appointment> appointments = appointmentService.getUserAppointments(chatId);
        String text = appointments.isEmpty() ? "Добро пожаловать!" : "Выберите действие:";
        notificationService.sendMainMenu(chatId, text);
    }

    private void handleBookAppointment(Long chatId, Integer messageId) {
        textMessageHandler.startAppointmentProcess(chatId, messageId);
    }

    private void handleMyAppointments(Long chatId) {
        showActiveAppointments(chatId);
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

            app.setStatus(StatusAppointment.CANCELED);
            appointmentService.cancelAppointment(app.getId());

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
}
