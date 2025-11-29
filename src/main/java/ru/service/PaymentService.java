package ru.service;

import ru.dto.PaymentRequestDto;
import ru.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    Payment createPayment(PaymentRequestDto paymentRequestDto, Integer adminId);

    BigDecimal getTodayIncome();

    BigDecimal getYesterdayIncome();

    Map<Integer, BigDecimal> getTodayHourlyIncome();

    Map<Integer, BigDecimal> getYesterdayHourlyIncome();

    List<Payment> getTodayPayments();

    List<Payment> getYesterdayPayments();

    BigDecimal getWeekIncome();

    BigDecimal getMonthIncome();

    BigDecimal getAllIncome();

    BigDecimal getIncomeForPeriod(LocalDate startDate, LocalDate endDate);

    Map<LocalDate, BigDecimal> getDetailedIncomeForPeriod(LocalDate startDate, LocalDate endDate);
}
