package ru.bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.model.Appointment;
import ru.model.enums.StatusAppointment;
import ru.scheduler.AppointmentNotificationScheduler;
import ru.service.AppointmentService;
import ru.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextMessageHandler {

    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final AppointmentNotificationScheduler notificationScheduler;

    public void handleTextMessage(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();

        // Проверяем состояние пользователя
        String state = appointmentService.getUserState(chatId);

        if ("/start".equalsIgnoreCase(text)) {
            sendWelcome(chatId);
            return;
        }

        if ("AWAITING_NAME".equals(state)) {
            handleUserName(chatId, text, message.getMessageId());
            return;
        }

        if ("AWAITING_PHONE".equals(state)) {
            handleUserPhone(chatId, text, message.getMessageId());
            return;
        }

        // Неизвестная команда → показать главное меню
        notificationService.sendMainMenu(chatId, "Выберите действие:");
    }

    private void sendWelcome(Long chatId) {
        String welcome = """
                👋 Добро пожаловать в салон красоты *AURA*!
                
                Вы можете:
                • Записаться на стрижку
                • Посмотреть свои записи
                • Узнать контакты
                """;
        notificationService.sendMainMenu(chatId, welcome);
    }

    private void handleUserName(Long chatId, String name, Integer messageId) {
        // Удаляем сообщение с именем
        notificationService.deleteMessage(chatId, messageId);

        appointmentService.setPendingName(chatId, name);
        appointmentService.setUserState(chatId, "AWAITING_PHONE");

        notificationService.sendOrEditMessage(chatId, null,
                "Спасибо, *%s*! Теперь введите ваш номер телефона 📱".formatted(name),
                null
        );
    }

    private void handleUserPhone(Long chatId, String phone, Integer messageId) {
        // Удаляем сообщение с номером
        notificationService.deleteMessage(chatId, messageId);

        LocalDateTime dateTime = appointmentService.getPendingDate(chatId);
        if (dateTime == null) {
            notificationService.sendOrEditMessage(chatId, null,
                    "Произошла ошибка. Попробуйте снова.", null);
            appointmentService.clearUserState(chatId);
            return;
        }

        try {
            Appointment appointment = Appointment.builder()
                    .clientChatId(chatId)
                    .clientName(appointmentService.getPendingName(chatId))
                    .clientPhoneNumber(phone)
                    .dateTime(dateTime)
                    .status(StatusAppointment.PENDING)
                    .build();

            appointmentService.createAppointment(appointment);
            appointmentService.setUserState(chatId, null);


            notificationService.sendOrEditMessage(chatId, null, """
                            ✅ Вы записаны на %s!
                            
                            Вам придёт уведомление:
                            • за день до записи
                            • за 2 часа до начала
                            
                            """.formatted(dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'в' HH:mm"))),
                    null
            );
        } catch (IllegalStateException e) {
            notificationService.sendOrEditMessage(chatId, null,
                    "❌ Это время уже занято. Пожалуйста, выберите другое.",
                    null
            );
            startAppointmentProcess(chatId, null);
        }

        // Возвращаем в меню
        notificationService.sendMainMenu(chatId, "Выберите действие:");
    }

    public void startAppointmentProcess(Long chatId, Integer messageId) {
        if (messageId != null) {
            notificationService.deleteMessage(chatId, messageId);
        }

        // Устанавливаем состояние пользователя
        appointmentService.setUserState(chatId, "AWAITING_DATE");

        // Показываем даты на неделю
        sendDateSelection(chatId, null);
    }

    public void sendDateSelection(Long chatId, Integer messageId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM (E)");

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            if (!appointmentService.getAvailableTimeSlots(date.atStartOfDay()).isEmpty()) {
                InlineKeyboardButton button = InlineKeyboardButton.builder()
                        .text(date.format(dateFormat))
                        .callbackData("date_" + date)
                        .build();
                rows.add(new InlineKeyboardRow(button));
            }
        }

        // Кнопка "Назад" к меню
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("⬅️ Назад")
                        .callbackData("back_to_menu")
                        .build()
        ));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        notificationService.sendOrEditMessage(chatId, messageId, "Выберите дату записи:", markup);
    }



}
