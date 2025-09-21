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
    HISTORY_PREV("history_prev"),

    ADMIN_SHOW_USERS("admin_show_users"),
    ADMIN_USERS_PAGE("admin_users_page_"),
    ADMIN_SEARCH_USER("admin_search_"),
    ADMIN_BLOCK_USER("admin_block_"),
    ADMIN_UNBLOCK_USER("admin_unblock_"),
    ADMIN_SHOW_STATS("admin_stats"),
    ADMIN_SHOW_APPOINTMENTS("admin_appointments"),
    ADMIN_CREATE_APPOINTMENT("admin_create_appointment"),
    ADMIN_CANCEL_APPOINTMENT("admin_cancel_"),
    ADMIN_BACK("admin_back"),
    ADMIN_DATE("admin_date_"),
    ADMIN_TIME("admin_time_");

    private final String prefix;

    CallbackType(String prefix) {
        this.prefix = prefix;
    }

    public static CallbackType fromString(String data) {
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
