package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dto.PaymentRequestDto;
import ru.model.Payment;
import ru.model.User;
import ru.repository.PaymentRepository;
import ru.service.PaymentService;
import ru.service.UserService;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserService userService;

    @Override
    @Transactional
    public Payment createPayment(PaymentRequestDto request, Integer adminId) {
        log.info("Создание платежа для пользователя {}, администратором с id: {}",
                request.getClientName(), adminId);
        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .serviceType(request.getServiceType())
                .serviceDate(request.getServiceDate())
                .comment(request.getComment())
                .createdBy(adminId)
                .build();

        User client = userService.findByPhoneNumber(request.getClientPhone()).orElse(null);
        if (request.getClientPhone() != null) {
            payment.setClientPhoneNumber(request.getClientPhone());
            payment.setClientName(request.getClientName());
        }
        if (client != null) {
            payment.setUser(client);
        }
        return paymentRepository.save(payment);
    }

    @Override
    public BigDecimal getTodayIncome() {
        log.info("Получение дохода за сегодня");
        LocalDate today = LocalDate.now();
        return paymentRepository.sumAmountByDate(today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
    }

    @Override
    public BigDecimal getYesterdayIncome() {
        log.info("Получение дохода за вчерашний день");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return paymentRepository.sumAmountByDate(yesterday.atStartOfDay(),
                yesterday.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> getTodayHourlyIncome() {
        log.info("Получение подробной статистики дохода за сегодня");
        LocalDate today = LocalDate.now();
        List<Object[]> result = paymentRepository.findHourlySumsByDate(today);
        return getMapWithZeroValueForAllHour(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> getYesterdayHourlyIncome() {
        log.info("Получение подробной статистики дохода за вчерашний день");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Object[]> result = paymentRepository.findHourlySumsByDate(yesterday);
        return getMapWithZeroValueForAllHour(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getTodayPayments() {
        LocalDate today = LocalDate.now();
        return paymentRepository.findByServiceDateBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getYesterdayPayments() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return paymentRepository.findByServiceDateBetween(
                yesterday.atStartOfDay(),
                yesterday.plusDays(1).atStartOfDay()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getWeekIncome() {
        log.info("Получение дохода за неделю");
        LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = LocalDate.now().plusDays(1);
        return paymentRepository.sumAmountByDate(startOfWeek.atStartOfDay(), endOfWeek.atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMonthIncome() {
        log.info("Получение дохода за месяц");
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().plusDays(1);
        return paymentRepository.sumAmountByDate(startOfMonth.atStartOfDay(), endOfMonth.atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAllIncome() {
        log.info("Получение дохода за все время");
        return paymentRepository.sumAllAmounts();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getIncomeForPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("Получение дохода за период с {} по {}", startDate, endDate);
        return paymentRepository.sumAmountByDate(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay() // +1 чтобы включить endDate
        );
    }

    // Получение подробной статистики по дням
    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, BigDecimal> getDetailedIncomeForPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("Получение подробной статистики за период с {} по {}", startDate, endDate);
        List<Object[]> result = paymentRepository.findDailySumsByPeriod(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        return result.stream()
                .collect(Collectors.toMap(
                        arr -> ((Date) arr[0]).toLocalDate(),
                        arr -> (BigDecimal) arr[1]
                ));
    }

    @NotNull
    private static Map<Integer, BigDecimal> getMapWithZeroValueForAllHour(List<Object[]> result) {
        Map<Integer, BigDecimal> hourlyIncome = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyIncome.put(hour, BigDecimal.ZERO);
        }
        for (Object[] row : result) {
            Integer hour = ((Number) row[0]).intValue();
            BigDecimal amount = (BigDecimal) row[1];
            hourlyIncome.put(hour, amount);
        }
        return hourlyIncome;
    }

}
