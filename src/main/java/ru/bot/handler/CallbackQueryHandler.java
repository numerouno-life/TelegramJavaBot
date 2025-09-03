package ru.bot.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.model.Appointment;
import ru.service.AppointmentService;
import ru.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class CallbackQueryHandler {

    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final TextMessageHandler textMessageHandler;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM (E)");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

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
            appointmentService.setUserState(chatId, "AWAITING_NAME");

            notificationService.sendOrEditMessage(chatId, messageId,
                    "Вы выбрали время: " + selectedTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                            + "\n\nВведите ваше имя:", null
            );

        } else if (data.equals("back_to_menu")) {
            List<Appointment> appointments = appointmentService.getUserAppointments(chatId);
            String text = appointments.isEmpty() ? "Добро пожаловать!" : "Выберите действие:";
            notificationService.sendMainMenu(chatId, text);

        } else if (data.equals("book_appointment")) {
            textMessageHandler.startAppointmentProcess(chatId, messageId);

        } else if (data.equals("my_appointments")) {
            showUserAppointments(chatId);

        } else if (data.equals("contacts")) {
            notificationService.sendContacts(chatId);

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
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

        InlineKeyboardRow row = new InlineKeyboardRow();

        while (start.isBefore(end)) {
            if (appointmentService.isTimeSlotAvailable(start)) {
                InlineKeyboardButton button = InlineKeyboardButton.builder()
                        .text("🟢 " + start.toLocalTime().format(timeFormat))
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


    private void showUserAppointments(Long chatId) {
        List<Appointment> appointments = appointmentService.getUserAppointments(chatId);
        if (appointments.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, null, "У вас нет активных записей.", null);
        } else {
            StringBuilder sb = new StringBuilder("Ваши записи:\n\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            for (Appointment app : appointments) {
                sb.append("📅 ").append(app.getDateTime().format(formatter))
                        .append("\n💇 Мужская стрижка")
                        .append("\n📞 ").append(app.getClientPhoneNumber())
                        .append("\n\n");
            }
            notificationService.sendOrEditMessage(chatId, null, sb.toString(), null);
        }
    }
}
