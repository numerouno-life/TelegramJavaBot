package ru.util;

import java.time.format.DateTimeFormatter;

public class BotConstants {

    public static final String ICON_BOOK = "💇";
    public static final String ICON_LIST = "📋";
    public static final String ICON_PHONE = "📞";
    public static final String ICON_HISTORY = "📝";
    public static final String ICON_USERS = "👥";
    public static final String ICON_BLOCK = "🚫";
    public static final String ICON_UNBLOCK = "✅";
    public static final String ICON_BACK = "🔙";
    public static final String ICON_STATS = "📊";
    public static final String ICON_SETTINGS = "⚙️";
    public static final String ICON_CALENDAR = "🗓";

    // Команды пользователя
    public static final String CMD_START = "/start";
    public static final String CMD_BEGIN = "начать";
    public static final String CMD_BOOK = ICON_BOOK + " Записаться";
    public static final String CMD_MY_APPOINTMENTS = ICON_LIST + " Мои записи";
    public static final String CMD_CONTACTS = ICON_PHONE + " Контакты";
    public static final String CMD_HISTORY = ICON_HISTORY + " История записей";

    // Команды админа
    public static final String CMD_ADMIN = "/admin";
    public static final String CMD_ALL_APPOINTMENTS = ICON_LIST + " Все активные записи";
    public static final String CMD_CREATE_APPOINTMENT_ADMIN = ICON_BOOK + " Создать запись";
    public static final String CMD_ALL_USERS = ICON_USERS + " Пользователи";
    public static final String CMD_WORKING_HOURS = ICON_CALENDAR + " График работы";
    public static final String CMD_BLOCKED_USER = ICON_BLOCK + " Заблокировать";
    public static final String CMD_UNBLOCKED_USER = ICON_UNBLOCK + " Разблокировать";
    public static final String CMD_ADMIN_BACK = ICON_BACK + " Назад";
    public static final String CMD_SHOW_STATS = ICON_STATS + " Статистика";
    public static final String CMD_ADMIN_EDIT_WORK_SCHEDULE = ICON_SETTINGS + " Редактировать расписание";
    public static final String CMD_ADMIN_SCHEDULE_MENU = ICON_CALENDAR + " Расписание";

    // Состояния
    public static final String STATE_AWAITING_NAME = "AWAITING_NAME";
    public static final String STATE_AWAITING_PHONE = "AWAITING_PHONE";
    public static final String STATE_AWAITING_DATE = "AWAITING_DATE";


    // Форматы
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Redis-префикс
    public static final String PREFIX = "session:";

}
