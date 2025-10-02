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
    public static final String ICON_BACK = "⬅️";
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
    public static final String CMD_ALL_APPOINTMENTS = ICON_LIST + " Все активные записи/отмена записи";
    public static final String CMD_ADMIN_APPOINTMENTS_TODAY = ICON_LIST + " Записи на сегодня";
    public static final String CMD_ADMIN_APPOINTMENTS_TOMORROW = ICON_LIST + " Записи на завтра";
    public static final String CMD_CREATE_APPOINTMENT_ADMIN = ICON_BOOK + " Создать запись";
    public static final String CMD_ALL_USERS = ICON_USERS + " Пользователи";
    public static final String CMD_WORKING_HOURS = ICON_CALENDAR + " График работы";
    public static final String CMD_BLOCKED_USER = ICON_BLOCK + " Заблокировать";
    public static final String CMD_UNBLOCKED_USER = ICON_UNBLOCK + " Разблокировать";
    public static final String CMD_ADMIN_BACK = ICON_BACK + " Назад в Админку";
    public static final String CMD_SHOW_STATS = ICON_STATS + " Статистика";
    public static final String CMD_ADMIN_EDIT_WORK_SCHEDULE = ICON_SETTINGS + " Редактировать расписание";
    public static final String CMD_ADMIN_SCHEDULE_MENU = ICON_CALENDAR + " Расписание работы";
    public static final String CMD_ADMIN_MANAGE_OVERRIDES = ICON_SETTINGS + " Меню исключений";
    public static final String CMD_ADMIN_ALL_OVERRIDES = ICON_LIST + " Исключения";
    public static final String CMD_ADMIN_ADD_OVERRIDE = ICON_BOOK + " Добавить исключение";
    public static final String CMD_ADMIN_DELETE_OVERRIDE = ICON_HISTORY + " Удалить исключение";
    public static final String CMD_ADMIN_BACK_TO_OVERRIDES = ICON_BACK + " Назад к списку исключений";
    public static final String CMD_ADMIN_MANAGEMENT = ICON_SETTINGS + " Меню назначения админов";

    // Форматы
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Redis-префикс
    public static final String PREFIX = "session:";

}
