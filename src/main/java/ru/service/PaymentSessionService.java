package ru.service;

import ru.model.enums.PaymentState;
import ru.model.enums.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface PaymentSessionService {

    void setPaymentState(Long chatId, PaymentState state);

    PaymentState getPaymentState(Long chatId);

    void setAmount(Long chatId, BigDecimal amount);

    BigDecimal getAmount(Long chatId);

    void setServiceDate(Long chatId, LocalDateTime date);

    LocalDateTime getServiceDate(Long chatId);

    void setServiceType(Long chatId, ServiceType type);

    ServiceType getServiceType(Long chatId);

    void setClientPhone(Long chatId, String phoneNumber);

    String getClientPhone(Long chatId);

    void setClientName(Long chatId, String name);

    String getClientName(Long chatId);

    void setStatsStartDate(Long chatId, LocalDate date);

    LocalDate getStatsStartDate(Long chatId);

    void setStatsEndDate(Long chatId, LocalDate date);

    LocalDate getStatsEndDate(Long chatId);

    void clearPaymentState(Long chatId);

}
