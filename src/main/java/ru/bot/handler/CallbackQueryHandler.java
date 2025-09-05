package ru.bot.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.model.Appointment;
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

    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final TextMessageHandler textMessageHandler;
    private final KeyboardFactory keyboardFactory;

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("date_")) {
            // Пользователь выбрал дату → показываем время
            LocalDate selectedDate = LocalDate.parse(data.substring(5));
            sendTimeSelection(chatId, messageId, selectedDate);
        } else if (data.startsWith("time_")) {
            // Пользователь выбрал время → сохраняем и запрашиваем имя
            LocalDateTime selectedTime = LocalDateTime.parse(data.substring(5));
            appointmentService.setPendingDate(chatId, selectedTime);
            appointmentService.setUserState(chatId, STATE_AWAITING_NAME);

            notificationService.sendOrEditMessage(chatId, messageId,
                    "Вы выбрали время: " + selectedTime.format(DATE_FORMAT)
                            + "\n\nВведите ваше имя:", null
            );

        } else if (data.equals("back_to_menu")) {
            List<Appointment> appointments = appointmentService.getUserAppointments(chatId);
            String text = appointments.isEmpty() ? "Добро пожаловать!" : "Выберите действие:";
            notificationService.sendMainMenu(chatId, text);

        } else if (data.equals("book_appointment")) {
            textMessageHandler.startAppointmentProcess(chatId, messageId);

        } else if (data.equals("my_appointments")) {
            showActiveAppointments(chatId);

        } else if (data.equals("contacts")) {
            notificationService.sendContacts(chatId);

        } else if (data.startsWith("cancel_")) {
            Long appointmentId = Long.parseLong(data.substring(7));
            Appointment app = appointmentService.findById(appointmentId);

            if (app != null) {
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
            } else {
                notificationService.sendOrEditMessage(chatId, messageId, "Запись не найдена", null);

            }

        } else if (data.equals("history")) {
            showPastAppointments(chatId);

        } else if (data.equals("back_to_dates")) {
            // Назад к выбору даты
            textMessageHandler.sendDateSelection(chatId, messageId);
        } else {
            log.warn("Unknown callback: {}", data);
        }
    }

    private void sendTimeSelection(Long chatId, Integer messageId, LocalDate date) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        LocalDateTime start = date.atStartOfDay().withHour(10);
        LocalDateTime end = date.atStartOfDay().withHour(21);

        InlineKeyboardRow row = new InlineKeyboardRow();

        while (start.isBefore(end)) {
            if (appointmentService.isTimeSlotAvailable(start)) {
                InlineKeyboardButton button = InlineKeyboardButton.builder()
                        .text("🟢 " + start.toLocalTime().format(TIME_FORMAT))
                        .callbackData("time_" + start)
                        .build();
                row.add(button);

                if (row.size() == 3) {
                    rows.add(row);
                    row = new InlineKeyboardRow();
                }
            }
            start = start.plusHours(1);
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }

        // Кнопка "Назад" к датам
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("⬅️ Назад")
                        .callbackData("back_to_dates")
                        .build()
        ));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        notificationService.sendOrEditMessage(chatId, messageId,
                "Доступное время на " + date.format(DateTimeFormatter.ofPattern("dd.MM (E)")) + ":\n🟢 - свободно",
                markup
        );
    }


    private void showActiveAppointments(Long chatId) {
        List<Appointment> active = appointmentService.getActiveAppointments(chatId);
        if (active.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, null, "У вас нет активных записей.", null);
            return;
        } else {
            for (Appointment app : active) {
                String text = "📅 " + app.getDateTime().format(DATE_FORMAT)
                        + " - " + app.getDateTime().format(TIME_FORMAT)
                        + "\n💇 Мужская стрижка"
                        + "\n📞 " + app.getClientPhoneNumber();

                InlineKeyboardButton cancelBtn = InlineKeyboardButton.builder()
                        .text("❌ Отменить запись на " + app.getDateTime().format(DATE_FORMAT))
                        .callbackData("cancel_" + app.getId())
                        .build();
                InlineKeyboardRow row = new InlineKeyboardRow(cancelBtn);
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(row));

                notificationService.sendOrEditMessage(chatId, null, text, markup);
            }
        }
    }

    private void showPastAppointments(Long chatId) {
        List<Appointment> past = appointmentService.getPastAppointments(chatId);
        if (past.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, null, "У вас нет прошлых записей.", null);
            return;
        }
        for (Appointment app : past) {
            String status = app.getStatus() == StatusAppointment.CANCELED ? "❌ Отменена" : "✅ Завершена";
            String text = "📅 " + app.getDateTime().format(DATE_FORMAT)
                    + " - " + app.getDateTime().format(TIME_FORMAT)
                    + "\n\n" + status
                    + "\n📞 " + app.getClientPhoneNumber()
                    + "\n\n";
            notificationService.sendMessage(chatId, text);
        }
    }
}
