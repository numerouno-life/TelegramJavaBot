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

    ADMIN_SHOW_USERS("admin_show_users"),
    ADMIN_USERS_PAGE("admin_users_page_"),
    ADMIN_SEARCH_USER("admin_search_"),
    ADMIN_BLOCK_USER("admin_block_"),
    ADMIN_UNBLOCK_USER("admin_unblock_"),
    ADMIN_SHOW_STATS("admin_stats"),
    ADMIN_SHOW_APPOINTMENTS("admin_appointments"),
    ADMIN_ALL_TODAY_APP("all:today:app"),
    ADMIN_ALL_TOMORROW_APP("all:tomorrow:app"),
    ADMIN_CREATE_APPOINTMENT("admin_create_appointment"),
    ADMIN_CANCEL_APPOINTMENT("admin_cancel_"),
    ADMIN_BACK("admin_back"),
    ADMIN_EDIT_WORK_SCHEDULE("admin:edit:schedule"),
    ADMIN_EDIT_DAY("admin:edit:day_"),
    ADMIN_SAVE_DAY("admin:save:day_"),
    ADMIN_BACK_TO_SCHEDULE("admin:back:schedule"),
    ADMIN_SCHEDULE_MENU("admin:schedule:menu"),
    ADMIN_MANAGE_OVERRIDES("admin:overrides"),
    ADMIN_ADD_OVERRIDE("admin:override:add"),
    ADMIN_DELETE_OVERRIDE("admin:override:delete_"),
    ADMIN_MENU_APPOINTMENTS("admin:menu:appointments"),
    ADMIN_MENU_SCHEDULE("admin:menu:schedule"),
    ADMIN_APPOINTMENTS_PAGE("admin:appointments:page_"),
    ADMIN_ADD_NEW_ADMIN("admin:add:new_admin"),
    ADMIN_SET_NEW_ADMIN("admin:set:new_admin_"),
    ADMIN_DELETE_ADMIN("admin:delete:admin_"),
    ADMIN_ADMINS_PAGE("admin_admins_page_"),
    ADMIN_LUNCH_MENU("admin:lunch:menu"),
    ADMIN_EDIT_LUNCH("admin:edit:lunch_"),
    ADMIN_SAVE_LUNCH("admin:save:lunch_");


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
