package ru.model.enums;

import java.util.Arrays;

public enum CallbackType {
    DATE("date_"),
    TIME("time_"),
    BACK_TO_MENU("back_to_menu"),
    BOOK_APPOINTMENT("book_appointment"),
    MY_APPOINTMENTS("my_appointments"),
    CONTACTS("contacts"),
    CANCEL("cancel_"),
    HISTORY_PAGE("history_page"),
    HISTORY("history"),
    BACK_TO_DATES("back_to_dates"),
    UNKNOWN("unknown"),
    HISTORY_NEXT("history_next"),
    HISTORY_PREV("history_prev");

    private final String prefix;

    CallbackType(String prefix) {
        this.prefix = prefix;
    }

    public static CallbackType fromString(String data) {
        if (data == null) return UNKNOWN;

        // Сортируем по длине префикса (сначала самые длинные)
        return Arrays.stream(values())
                .filter(type -> type.prefix.length() > 0)
                .sorted((a, b) -> Integer.compare(b.prefix.length(), a.prefix.length()))
                .filter(type -> data.startsWith(type.prefix))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
