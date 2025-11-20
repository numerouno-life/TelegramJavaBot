package ru.model.enums;

public enum PaymentState {
    AWAITING_AMOUNT,
    AWAITING_SERVICE_DATE,
    AWAITING_SERVICE_TYPE,
    AWAITING_CLIENT_PHONE,
    AWAITING_CLIENT_NAME,
    AWAITING_CONFIRMATION // ожидание подтверждения
}
