package ru.util;

import java.time.format.DateTimeFormatter;

public class BotConstants {

    public static final String CMD_START = "/start";
    public static final String CMD_BEGIN = "начать";
    public static final String CMD_BOOK = "💇 Записаться";
    public static final String CMD_MY_APPOINTMENTS = "📋 Мои записи";
    public static final String CMD_CONTACTS = "📞 Контакты";
    public static final String CMD_HISTORY = "📝 История записей";

    public static final String CMD_ADMIN = "/admin";
    public static final String CMD_ALL_APPOINTMENTS = "📝 Все активные записи";
    public static final String CMD_CREATE_APPOINTMENT_ADMIN = "📝 Создать запись";
    public static final String CMD_ALL_USERS = "👥 Пользователи";
    public static final String CMD_WORKING_HOURS = "📅 График работы";
    public static final String CMD_BLOCKED_USER = "🚫 Заблокировать";
    public static final String CMD_UNBLOCKED_USER = "✅ Разблокировать";
    public static final String CMD_ADMIN_BACK = "🔙 Назад";
    public static final String CMD_SHOW_STATS = "📊 Статистика";

    public static final String STATE_AWAITING_NAME = "AWAITING_NAME";
    public static final String STATE_AWAITING_PHONE = "AWAITING_PHONE";
    public static final String STATE_AWAITING_DATE = "AWAITING_DATE";

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static final String PREFIX = "session:";

}
