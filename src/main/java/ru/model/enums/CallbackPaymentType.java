package ru.model.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CallbackPaymentType {

    PAYMENT_MENU("payment:menu"),
    PAYMENT_STATISTICS("payment:statistics"),
    PAYMENT_CANCEL_STATS("payment:cancel:stats"),
    PAYMENT_CREATE_NEW("payment:create:new"),
    PAYMENT_SERVICE_TYPE("payment:service:"),
    PAYMENT_SELECT_DATE("payment:date_"),
    PAYMENT_SELECT_TIME("payment:time_"),
    PAYMENT_TODAY_STATS("payment:today:stats"),
    PAYMENT_YESTERDAY_STATS("payment:yesterday:stats"),
    PAYMENT_CURRENT_WEEK_STATS("payment:current:week:stats"),
    PAYMENT_CURRENT_MONTH_STATS("payment:current:month:stats"),
    PAYMENT_TOTAL_STATS("payment:total:stats"),
    PAYMENT_CUSTOM_PERIOD("payment:custom:period"),
    PAYMENT_SELECT_START_DATE("payment:select:start:date_"),
    PAYMENT_SELECT_END_DATE("payment:select:end:date_"),
    PAYMENT_CLIENT_STATS("payment:client:stats"),
    PAYMENT_CONFIRM("payment:confirm"),
    PAYMENT_CANCEL("payment:cancel"),
    UNKNOWN("unknown");

    private final String prefix;

    CallbackPaymentType(String prefix) {
        this.prefix = prefix;
    }

    public static CallbackPaymentType fromString(String data) {
        if (data == null) return UNKNOWN;

        // Сортируем по длине префикса (сначала самые длинные)
        return Arrays.stream(values())
                .filter(type -> !type.prefix.isEmpty())
                .sorted((a, b) -> Integer.compare(b.prefix.length(), a.prefix.length()))
                .filter(type -> data.startsWith(type.prefix))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
