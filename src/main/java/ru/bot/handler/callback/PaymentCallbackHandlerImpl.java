package ru.bot.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bot.handler.PaymentCallbackHandler;
import ru.dto.PaymentRequestDto;
import ru.model.Payment;
import ru.model.enums.*;
import ru.service.NotificationService;
import ru.service.PaymentService;
import ru.service.PaymentSessionService;
import ru.service.UserSessionService;
import ru.util.AdminKeyboard;
import ru.util.KeyboardFactory;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ru.util.BotConstants.DATE_FORMAT;
import static ru.util.BotConstants.TIME_FORMAT;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackHandlerImpl implements PaymentCallbackHandler {
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final PaymentSessionService paymentSessionService;
    private final AdminKeyboard adminKeyboard;
    private final UserSessionService userSessionService;
    private final KeyboardFactory keyboardFactory;

    @Override
    public void handlePaymentCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.debug("Обработка callback платежа: {}, type: {}", data, CallbackType.fromString(data));
        handlePaymentCallback(chatId, messageId, data);
    }

    private void handlePaymentCallback(Long chatId, Integer messageId, String data) {
        try {
            log.debug("🔍 Обработка callback: {}", data);
            CallbackPaymentType callbackPaymentType = CallbackPaymentType.fromString(data);

            log.debug("🔍 Определен тип: {}", callbackPaymentType);
            switch (callbackPaymentType) {
                case PAYMENT_MENU -> notificationService.sendOrEditMessage(chatId, messageId,
                        "💰 *Управление платежами*", adminKeyboard.getPaymentMenu());
                case PAYMENT_STATISTICS -> notificationService.sendOrEditMessage(chatId, messageId,
                        "📊 *Статистика платежей*", adminKeyboard.getStatisticsMenu());
                case PAYMENT_CANCEL_STATS -> cancelStats(chatId, messageId);
                case PAYMENT_CREATE_NEW -> createNewPayment(chatId, messageId);
                case PAYMENT_SERVICE_TYPE -> handleServiceTypeSelection(chatId, messageId, data);
                case PAYMENT_SELECT_DATE -> handlePaymentDateSelection(chatId, messageId, data);
                case PAYMENT_SELECT_TIME -> handlePaymentTimeSelection(chatId, messageId, data);
                case PAYMENT_CONFIRM -> confirmPayment(chatId, messageId);
                case PAYMENT_CANCEL -> cancelPayment(chatId, messageId);
                case PAYMENT_TODAY_STATS -> showTodayStats(chatId, messageId);
                case PAYMENT_YESTERDAY_STATS -> showYesterdayStats(chatId, messageId);
                case PAYMENT_CURRENT_WEEK_STATS -> showCurrentWeekStats(chatId, messageId);
                case PAYMENT_CURRENT_MONTH_STATS -> showCurrentMonthStats(chatId, messageId);
                case PAYMENT_TOTAL_STATS -> showTotalIncome(chatId, messageId);
                case PAYMENT_CUSTOM_PERIOD -> showCustomPeriodForm(chatId, messageId);
                case UNKNOWN -> log.warn("Неизвестный тип callback в PaymentCallBack: {}", data);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке платежа: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при обработке платежа. Попробуйте снова.", null);
        }
    }

    private void cancelStats(Long chatId, Integer messageId) {
        log.info("Отмена ввода периода для статистики");
        paymentSessionService.clearPaymentState(chatId);
        notificationService.sendOrEditMessage(chatId, messageId,
                "❌ Ввод периода отменен", adminKeyboard.getStatisticsMenu());
    }

    private void createNewPayment(Long chatId, Integer messageId) {
        log.info("Создание нового платежа");
        paymentSessionService.clearPaymentState(chatId);
        userSessionService.clearAdminState(chatId);
        paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_AMOUNT);
        log.info("Установлено состояние: {}", PaymentState.AWAITING_AMOUNT);
        notificationService.sendOrEditMessage(chatId, messageId,
                "💵 *Новый платеж*\n\nВведите сумму оплаты:",
                adminKeyboard.getCancelPaymentKeyboard());
    }

    private void cancelPayment(Long chatId, Integer messageId) {
        log.info("Отмена платежа");
        paymentSessionService.clearPaymentState(chatId);
        notificationService.sendOrEditMessage(chatId, messageId,
                "❌ Платеж отменен", adminKeyboard.getStatisticsMenu());
    }

    private void handleServiceTypeSelection(Long chatId, Integer messageId, String data) {
        log.info("Выбор типа услуги");
        try {
            String serviceTypeStr = data.substring("payment:service:".length());
            ServiceType serviceType = ServiceType.valueOf(serviceTypeStr);
            paymentSessionService.setServiceType(chatId, serviceType);
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_SERVICE_DATE);
            BigDecimal amount = paymentSessionService.getAmount(chatId);
            String serviceName = serviceType.getDescription();
            sendDateSelectionForPayment(chatId, messageId, serviceName, amount);

            log.info("Выбрана услуга: {}, состояние изменено на AWAITING_SERVICE_DATE", serviceType);
        } catch (Exception e) {
            log.error("Ошибка при выборе услуги: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при выборе услуги", null);
        }
    }

    private void handlePaymentDateSelection(Long chatId, Integer messageId, String data) {
        log.info("Выбор даты");
        try {
            String dateStr = data.substring("payment:date_".length());
            LocalDate selectedDate = LocalDate.parse(dateStr);
            paymentSessionService.setServiceDate(chatId, selectedDate.atStartOfDay());
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_SERVICE_TIME);
            sendTimeSelectionForPayment(chatId, messageId, selectedDate);

            log.info("Выбрана дата: {}, состояние изменено на AWAITING_CLIENT_PHONE", selectedDate);
        } catch (Exception e) {
            log.error("Ошибка при выборе даты: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при выборе даты", null);
        }
    }

    private void handlePaymentTimeSelection(Long chatId, Integer messageId, String data) {
        log.info("Выбор времени");
        try {
            String timeStr = data.substring("payment:time_".length());
            String[] parts = timeStr.split("_");
            LocalDate date = LocalDate.parse(parts[0]);
            LocalTime time = LocalTime.parse(parts[1]);

            LocalDateTime serviceDateTime = LocalDateTime.of(date, time);
            paymentSessionService.setServiceDate(chatId, serviceDateTime);
            paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_CLIENT_PHONE);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "📞 *Данные клиента*\n\nВведите номер телефона клиента (или /skip чтобы пропустить):",
                    adminKeyboard.getCancelPaymentKeyboard());
        } catch (Exception e) {
            log.error("Ошибка при выборе времени: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при выборе времени", null);
        }
    }

    private void confirmPayment(Long chatId, Integer messageId) {
        log.info("Подтверждение платежа");
        try {
            PaymentRequestDto request = new PaymentRequestDto();
            request.setAmount(paymentSessionService.getAmount(chatId));
            request.setServiceType(paymentSessionService.getServiceType(chatId));
            request.setServiceDate(paymentSessionService.getServiceDate(chatId));
            request.setClientPhone(paymentSessionService.getClientPhone(chatId));
            request.setClientPhone(paymentSessionService.getClientPhone(chatId));
            Payment payment = paymentService.createPayment(request, messageId);
            paymentSessionService.clearPaymentState(chatId);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "✅ Платеж успешно сохранен!\nID: " + payment.getId(),
                    adminKeyboard.getStatisticsMenu());
        } catch (Exception e) {
            log.error("Ошибка при подтверждении платежа: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при сохранении платежа", null);
        }
    }

    private void showCustomPeriodForm(Long chatId, Integer messageId) {
        log.info("Показ формы ввода периода для статистики");
        paymentSessionService.setPaymentState(chatId, PaymentState.AWAITING_STATS_START_DATE);
        notificationService.sendOrEditMessage(chatId, messageId,
                """
                        📅 *Ввод начальной даты для периода статистики*
                        
                        Введите *начальную дату* в формате *ДД.ММ.ГГГГ*
                        Например: *25.10.2023*
                        
                        Или введите *"сегодня"* для текущей даты""",
                keyboardFactory.cancelStatsButton());
    }

    private void showTotalIncome(Long chatId, Integer messageId) {
        log.info("Показ общего дохода");
        BigDecimal income = paymentService.getAllIncome();
        String text = String.format("""
                📊 *Общий доход*
                
                💵 Сумма: *%.2f руб.*
                """, income);
        notificationService.sendOrEditMessage(chatId, messageId, text, adminKeyboard.getStatisticsMenu());
    }

    private void showTodayStats(Long chatId, Integer messageId) {
        log.info("Показ статистики за сегодня");
        BigDecimal income = paymentService.getTodayIncome();
        Map<Integer, BigDecimal> hourlyIncome = paymentService.getTodayHourlyIncome();
        List<Payment> payments = paymentService.getTodayPayments();
        String date = "Сегодня";
        String text = buildTodayDetailedStats(income, hourlyIncome, payments, date);
        notificationService.sendOrEditMessage(chatId, messageId, text, adminKeyboard.getStatisticsMenu());
    }

    private void showYesterdayStats(Long chatId, Integer messageId) {
        log.info("Показ статистики за вчера");
        BigDecimal income = paymentService.getYesterdayIncome();
        Map<Integer, BigDecimal> hourlyIncome = paymentService.getYesterdayHourlyIncome();
        List<Payment> payments = paymentService.getYesterdayPayments();
        String date = "Вчера";
        String text = buildTodayDetailedStats(income, hourlyIncome, payments, date);
        notificationService.sendOrEditMessage(chatId, messageId, text, adminKeyboard.getStatisticsMenu());
    }

    private void showCurrentWeekStats(Long chatId, Integer messageId) {
        log.info("Показ статистики за текущую неделю");
        BigDecimal income = paymentService.getWeekIncome();
        LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        Map<LocalDate, BigDecimal> dailySums = paymentService.getDetailedIncomeForPeriod(startOfWeek, LocalDate.now());
        String dailyDetails = buildWeekDetailedStats(dailySums, startOfWeek, LocalDate.now());
        String text = String.format("""
                        📊 *Доход за текущую неделю*
                        
                        💵 Общая сумма: *%.2f руб.*
                        📅 Период: %s - %s
                        
                        *Детализация по дням:*
                        %s
                        """,
                income,
                startOfWeek.format(DATE_FORMAT),
                LocalDate.now().format(DATE_FORMAT),
                dailyDetails
        );
        notificationService.sendOrEditMessage(chatId, messageId, text, adminKeyboard.getStatisticsMenu());
    }

    private void showCurrentMonthStats(Long chatId, Integer messageId) {
        log.info("Показ статистики за текущий месяц");
        BigDecimal income = paymentService.getMonthIncome();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        String text = String.format("""
                📆 *Доход за текущий месяц*
                
                💵 Сумма: *%.2f руб.*
                📅 Период: %s - %s
                """, income, startOfMonth.format(DATE_FORMAT), LocalDate.now().format(DATE_FORMAT));
        notificationService.sendOrEditMessage(chatId, messageId, text, adminKeyboard.getStatisticsMenu());
    }

    private void sendTimeSelectionForPayment(Long chatId, Integer messageId, LocalDate selectedDate) {
        log.info("Отправка выбора времени для даты: {}", selectedDate);
        List<LocalTime> availableTimes = Arrays.asList(
                LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), LocalTime.of(14, 0), LocalTime.of(15, 0),
                LocalTime.of(16, 0), LocalTime.of(17, 0), LocalTime.of(18, 0),
                LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0),
                LocalTime.of(22, 0), LocalTime.of(23, 0), LocalTime.of(0, 0)
        );
        String message = String.format("""
                ⏰ *Выбор времени*
                
                Дата: %s
                Выберите время оказания услуги:
                """, selectedDate.format(DATE_FORMAT));
        InlineKeyboardMarkup markup = keyboardFactory.timeSelectionKeyboardForPayment(selectedDate, availableTimes);
        notificationService.sendOrEditMessage(chatId, messageId, message, markup);
    }

    private void sendDateSelectionForPayment(Long chatId, Integer messageId, String serviceName, BigDecimal amount) {
        LocalDate day = LocalDate.now().minusDays(1);
        List<LocalDate> availableDates = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            LocalDate date = day.plusDays(i);
            log.info("Проверка даты: {}", date);
            availableDates.add(date);
        }

        log.info("Доступные даты для платежа: {}", availableDates);

        String message = String.format("""
                📅 *Выбор даты*
                
                Услуга: %s
                Сумма: %s руб.
                
                Выберите дату:
                """, serviceName, amount.stripTrailingZeros().toPlainString());

        InlineKeyboardMarkup markup = keyboardFactory.dateSelectionKeyboardForPayment(availableDates, UserRole.ADMIN);
        notificationService.sendOrEditMessage(chatId, messageId, message, markup);
    }

    private void sendDateSelectionForStats(Long chatId, Integer messageId, LocalDate startDate) {
        log.info("Отправка выбора конечной даты для статистики, начальная дата: {}", startDate);
        LocalDate today = LocalDate.now();
        LocalDate maxEndDate = startDate.plusDays(30);
        LocalDate actualEndDate = today.isBefore(maxEndDate) ? today : maxEndDate;

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(actualEndDate)) {
            availableDates.add(current);
            current = current.plusDays(1);
        }
        String message = String.format("""
                📅 *Выбор конечной даты*
                
                Начальная дата: %s
                Выберите конечную дату:
                """, startDate.format(DATE_FORMAT));
        InlineKeyboardMarkup markup = adminKeyboard.dateSelectionKeyboardForStats(availableDates, "end");
        notificationService.sendOrEditMessage(chatId, messageId, message, markup);
    }

    private String buildTodayDetailedStats(BigDecimal totalIncome,
                                           Map<Integer, BigDecimal> hourlyIncome,
                                           List<Payment> todayPayments, String date) {
        StringBuilder sb = new StringBuilder();

        sb.append("📊 *Доход за ").append(date).append("*\n\n");
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) <= 0) {
            sb.append("💵 Общая сумма: *Платежей нет").append(" руб.*\n");
        } else {
            sb.append("💵 Общая сумма: *").append(totalIncome).append(" руб.*\n");
        }
        sb.append("📅 Дата: ").append(LocalDate.now().format(DATE_FORMAT)).append("\n\n");

        // Почасовая статистика
        sb.append("⏰ *По часам:*\n");
        boolean hasPayments = false;
        for (int hour = 0; hour < 24; hour++) {
            BigDecimal hourAmount = hourlyIncome.get(hour);
            if (hourAmount != null && hourAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("   %2d:00 - %2d:59 │ %6.2f руб.\n",
                        hour, hour, hourAmount));
                hasPayments = true;
            }
        }
        if (!hasPayments) {
            sb.append("   └── Платежей нет\n");
        }

        // Последние платежи
        sb.append("\n🔄 *Последние платежи:*\n");
        if (todayPayments.isEmpty()) {
            sb.append("   └── Платежей нет\n");
        } else {
            int count = Math.min(todayPayments.size(), 5);
            for (int i = 0; i < count; i++) {
                Payment p = todayPayments.get(i);
                String time = p.getServiceDate().format(TIME_FORMAT);
                String service = p.getServiceType().getDescription();
                sb.append(String.format("   %s │ %s │ %5.0f руб.\n",
                        time, service, p.getAmount()));
            }
            if (todayPayments.size() > 5) {
                sb.append("   └── и ещё ").append(todayPayments.size() - 5).append(" платежей\n");
            }
        }

        return sb.toString();
    }

    private String buildWeekDetailedStats(Map<LocalDate, BigDecimal> dailySums,
                                          LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

}
