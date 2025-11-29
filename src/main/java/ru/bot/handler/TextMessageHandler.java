package ru.bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.*;
import ru.service.*;
import ru.util.AdminKeyboard;
import ru.util.KeyboardFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final FloodProtectionService floodProtectionService;
    private final PaymentSessionService paymentSessionService;
    private final PaymentService paymentService;

    public void handleTextMessage(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();
        Long userId = message.getFrom().getId();
        String role = userSessionService.getRole(chatId);
        if (floodProtectionService.isFloodDetected(userId, text)) {
            log.warn("Флуд защита сработала для пользователя {} в текстовом сообщении", userId);
            notificationService.sendMessage(chatId, "❌ Слишком много запросов. Попробуйте позже.");
            return;
        }

        if (userService.isBlocked(chatId)) {
            notificationService.sendMessage(chatId, "❌ Ваш аккаунт заблокирован. Обратитесь к администратору.");
            return;
        }

        if (CMD_ADMIN.equalsIgnoreCase(text) || CMD_ADMIN_MENU.equalsIgnoreCase(text)) {
            if (userService.isAdmin(chatId)) {
                userSessionService.setRole(chatId, "ADMIN");
                notificationService.sendAdminMenu(chatId, "🔐 *Админ-панель*");
            } else {
                notificationService.sendMessage(chatId, "❌ У вас нет доступа к админ-панели.");
            }
            return;
        }

        PaymentState paymentState = paymentSessionService.getPaymentState(chatId);
        if (paymentState != null) {
            handlePaymentState(chatId, text, paymentState);
            return;
        }
        UserAppointmentState userState = appointmentService.getUserState(chatId);
        AdminAppointmentState adminState = appointmentService.getAdminState(chatId);
        log.debug("👑 Admin mode: chatId={}, adminState={}", chatId, adminState);
        log.debug("👤 User state: {}", userState);

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

        // Обработка обычного пользователя
        if (UserAppointmentState.STATE_AWAITING_NAME.equals(userState)) {
            handleUserName(chatId, text, message.getMessageId());
            return;
        }
        if (UserAppointmentState.STATE_AWAITING_PHONE.equals(userState)) {
            handleUserPhone(chatId, text, message.getMessageId(), false, message.getFrom().getUserName());
            return;
        }

        if (CMD_START.equalsIgnoreCase(text) || CMD_BEGIN.equalsIgnoreCase(text)) {
            sendWelcome(chatId);
            return;
        }

        // Неизвестная команда → показать главное меню
        notificationService.sendMainMenu(chatId, "Выберите действие:");
    }

    private void handlePaymentState(Long chatId, String text, PaymentState state) {
        log.info("Обработка платежного состояния: {}", state);

        switch (state) {
            case AWAITING_AMOUNT -> handlePaymentAmount(chatId, text);
            case AWAITING_CLIENT_PHONE -> handlePaymentClientPhone(chatId, text);
            case AWAITING_CLIENT_NAME -> handlePaymentClientName(chatId, text);
            case AWAITING_CONFIRMATION -> showPaymentConfirmation(chatId);
            case AWAITING_STATS_START_DATE -> handleStatsStartDateInput(chatId, text);
            case AWAITING_STATS_END_DATE -> handleStatsEndDateInput(chatId, text);
            default -> log.warn("Неизвестное платежное состояние: {}", state);
        }
    }

    private void handleStatsStartDateInput(Long chatId, String text) {
        if (text.equalsIgnoreCase("/cancel")) {
            paymentSessionService.clearPaymentState(chatId);
            notificationService.sendMessage(chatId, "❌ Операция отменена", adminKeyboard.getStatisticsMenu());
            return;
        }
        try {
            log.info("Обработка даты начала периода: {}", text);
            LocalDate startDate = parseDateInput(text);
            paymentSessionService.setStatsStartDate(chatId, startDate);
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_STATS_END_DATE);
            String message = "📅 *Ввод конечной даты периода для статистики*\n\n" +
                    "Начальная дата: *%s*\n".formatted(startDate.format(DATE_FORMAT)) +
                    "Введите *конечную дату* в формате *ДД.ММ.ГГГГ*";
            notificationService.sendMessage(chatId, message, keyboardFactory.cancelStatsButton());
        } catch (DateTimeParseException e) {
            log.error("Ошибка при обработке даты выбора начала периода", e);
            paymentSessionService.clearPaymentState(chatId);
            notificationService.sendMessage(chatId, "❌ Неверный формат даты. Введите дату в формате ДД.ММ.ГГГГ");
        }
    }

    private void handleStatsEndDateInput(Long chatId, String text) {
        if (text.equalsIgnoreCase("/cancel")) {
            paymentSessionService.clearPaymentState(chatId);
            notificationService.sendMessage(chatId, "❌ Операция отменена", adminKeyboard.getStatisticsMenu());
            return;
        }
        try {
            log.info("Обработка даты конца периода: {}", text);
            LocalDate startDate = paymentSessionService.getStatsStartDate(chatId);
            LocalDate endDate = parseDateInput(text);
            if (endDate.isBefore(startDate)) {
                notificationService.sendMessage(chatId,
                        "❌ Конечная дата не может быть раньше начальной.\n" +
                                "Введите корректную дату или /cancel.");
                return;
            }
                paymentSessionService.setStatsEndDate(chatId, endDate);
                paymentSessionService.clearPaymentState(chatId);
                showCustomPeriodStats(chatId, null, startDate, endDate);
        } catch (DateTimeParseException e) {
            log.error("Ошибка при обработке даты выбора конца периода", e);
            paymentSessionService.clearPaymentState(chatId);
            notificationService.sendMessage(chatId, "❌ Неверный формат даты. Введите дату в формате ДД.ММ.ГГГГ");
        }
    }

    private LocalDate parseDateInput(String text) {
        if (text.equalsIgnoreCase("сегодня")) {
            return LocalDate.now();
        }
        return LocalDate.parse(text.trim(), DATE_FORMAT);
    }

    private void handlePaymentAmount(Long chatId, String text) {
        try {
            String normalized = text.trim().replace(',', '.').replaceAll("\\s+", "");
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                notificationService.sendMessage(chatId, "❌ Сумма должна быть больше 0");
                return;
            }
            paymentSessionService.setAmount(chatId, amount);
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_SERVICE_TYPE);
            String displayAmount = amount.stripTrailingZeros().toPlainString();

            notificationService.sendMessage(chatId,
                    "🎯 *Выбор услуги*\n\nСумма: " + displayAmount + " руб.\nВыберите тип услуги:",
                    adminKeyboard.getServiceTypesKeyboard());

        } catch (NumberFormatException | ArithmeticException e) {
            notificationService.sendMessage(chatId, "❌ Введите корректную сумму (например: 1500 или 1200.50)");
        }
    }

    private void handlePaymentClientPhone(Long chatId, String text) {
        if ("/skip".equalsIgnoreCase(text)) {
            paymentSessionService.setClientPhone(chatId, null);
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_CLIENT_NAME);
            notificationService.sendMessage(chatId, "👤 Введите имя клиента:");
            return;
        }
        if (isValidPhone(text)) {
            paymentSessionService.setClientPhone(chatId, text);
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_CLIENT_NAME);
            notificationService.sendMessage(chatId, "👤 Введите имя клиента:");
        } else {
            notificationService.sendMessage(chatId,
                    "❌ Неверный формат номера телефона. Введите номер в формате +79991234567 или /skip:",
                    adminKeyboard.getCancelPaymentKeyboard());
        }
    }

    private void handlePaymentClientName(Long chatId, String text) {
        paymentSessionService.setClientName(chatId, text);
        paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_CONFIRMATION);
        showPaymentConfirmation(chatId);
    }

    private void showPaymentConfirmation(Long chatId) {
        BigDecimal amount = paymentSessionService.getAmount(chatId);
        ServiceType serviceType = paymentSessionService.getServiceType(chatId);
        LocalDateTime serviceDateTime = paymentSessionService.getServiceDate(chatId);
        String clientPhone = paymentSessionService.getClientPhone(chatId);
        String clientName = paymentSessionService.getClientName(chatId);

        StringBuilder summary = new StringBuilder();
        summary.append("✅ *Подтверждение платежа*\n\n");
        summary.append("💵 Сумма: ").append(amount).append(" руб.\n");
        summary.append("🎯 Услуга: ").append(serviceType.getDescription()).append("\n");
        summary.append("📅 Дата и время: ").append(serviceDateTime.format(DATE_FORMAT))
                .append(" ").append(serviceDateTime.format(TIME_FORMAT)).append("\n");
        if (clientPhone != null) {
            summary.append("📞 Телефон: ").append(clientPhone).append("\n");
        }
        if (clientName != null) {
            summary.append("👤 Имя: ").append(clientName).append("\n");
        }
        summary.append("\n✅ Подтвердите платеж?");
        notificationService.sendMessage(chatId, summary.toString(),
                adminKeyboard.getConfirmPaymentKeyboard());
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
        userSessionService.clearAdminState(chatId);
        userSessionService.clearPendingName(chatId);
        userSessionService.clearPendingDate(chatId);
        notificationService.sendMessage(chatId, "✅ Исключение добавлено!");
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
                Привет! 👋 Рады тебя видеть в нашем барбершопе!
                
                Вы можете:
                • 🗓️ Записаться на стрижку
                • 📲 Глянуть мои ближайшие записи
                • 📜 Посмотреть историю записей
                • 📍 Узнать, где мы находимся
                • 🎮 Залететь в FiFa-26 за скидкой! 🏆 (Готов проиграть?)
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

        if (!isAdminFlow && !isValidPhone(phone)) {
            notificationService.sendMessage(chatId, "❌ Неверный формат номера телефона. " +
                    "Введите номер в формате +71234567890 или 89123456789");
            appointmentService.setUserState(chatId, UserAppointmentState.STATE_AWAITING_PHONE);
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

    private void showCustomPeriodStats(Long chatId, Integer messageId, LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Показ статистики за выбранный период");
            BigDecimal totalIncome = paymentService.getIncomeForPeriod(startDate, endDate);
            Map<LocalDate, BigDecimal> dailySums = paymentService.getDetailedIncomeForPeriod(startDate, endDate);
            String statsText = buildCustomPeriodStats(totalIncome, dailySums, startDate, endDate);
            paymentSessionService.clearPaymentState(chatId);
            notificationService.sendOrEditMessage(chatId, messageId, statsText, adminKeyboard.getStatisticsMenu());
        } catch (Exception e) {
            log.error("Ошибка при получении статистики за период: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при получении статистики", adminKeyboard.getStatisticsMenu());
        }
    }

    private String buildCustomPeriodStats(BigDecimal totalIncome, Map<LocalDate, BigDecimal> dailySums,
                                          LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();

        sb.append("📊 *Статистика за период*\n\n");
        sb.append("💵 Общая сумма: *").append(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .append(" руб.*\n");
        sb.append("📅 Период: ").append(startDate.format(DATE_FORMAT))
                .append(" - ").append(endDate.format(DATE_FORMAT)).append("\n\n");

        sb.append("*Детализация по дням:*\n");

        if (dailySums == null || dailySums.isEmpty()) {
            sb.append("   └── Платежей нет\n");
        } else {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                BigDecimal dayAmount = dailySums.get(current);
                String dayName = adminKeyboard.getShortDayName(current.getDayOfWeek().getValue());
                String amountStr = (dayAmount != null && dayAmount.compareTo(BigDecimal.ZERO) > 0)
                        ? String.format("%.2f руб.", dayAmount)
                        : "—";

                sb.append(String.format("   %s %s | *%s*\n",
                        dayName, current.format(DateTimeFormatter.ofPattern("dd.MM")), amountStr));

                current = current.plusDays(1);
            }
        }

        long daysWithPayments = dailySums != null ?
                dailySums.values().stream().filter(amount -> amount != null &&
                        amount.compareTo(BigDecimal.ZERO) > 0).count() : 0;

        sb.append("\n📈 *Итого:* ").append(daysWithPayments)
                .append(" дней с платежами из ").append(startDate.until(endDate).getDays() + 1);

        return sb.toString();
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
        Message message = notificationService.sendMessageAndReturn(chatId, "Выберите дату записи:", markup);
        appointmentService.setPendingMessageId(chatId, message.getMessageId());
    }

    // Чистка состояния при ошибках
    private void cleanupAfterError(Long chatId, boolean isAdminFlow) {
        appointmentService.clearUserState(chatId);
        if (isAdminFlow) appointmentService.clearAdminState(chatId);
        userSessionService.clearRole(chatId);
        userSessionService.clearPendingName(chatId);
        appointmentService.clearPendingDate(chatId);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^(\\+7|8)\\d{10}$");
    }
}