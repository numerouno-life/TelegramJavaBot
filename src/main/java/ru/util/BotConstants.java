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
    public static final String ICON_LUNCH = "🍽 ";
    public static final String ICON_MONEY = "💰";
    public static final String ICON_DOLLAR = "💵";
    public static final String ICON_CALENDARED = "📅";
    public static final String ICON_TOMORROW_CALENDARED = "📆";
    public static final String ICON_YESTERDAY_CALENDARED = "📖";

    // Команды пользователя
    public static final String CMD_START = "/start";
    public static final String CMD_BEGIN = "начать";
    public static final String CMD_BOOK = ICON_BOOK + " Записаться";
    public static final String CMD_MY_APPOINTMENTS = ICON_LIST + " Мои записи";
    public static final String CMD_CONTACTS = ICON_PHONE + " Контакты";
    public static final String CMD_HISTORY = ICON_HISTORY + " История записей";

    // Команды админа
    public static final String CMD_ADMIN = "/admin";
    public static final String CMD_ADMIN_MENU = "админ";
    public static final String CMD_USER_MENU = "/user";
    public static final String CMD_ALL_APPOINTMENTS = ICON_LIST + " Все активные записи/отмена записи";
    public static final String CMD_ADMIN_APPOINTMENTS_TODAY = ICON_LIST + " Записи на сегодня";
    public static final String CMD_ADMIN_APPOINTMENTS_TOMORROW = ICON_LIST + " Записи на завтра";
    public static final String CMD_CREATE_APPOINTMENT_ADMIN = ICON_BOOK + " Создать запись";
    public static final String CMD_ALL_USERS = ICON_USERS + " Пользователи";
    public static final String CMD_WORKING_HOURS = ICON_CALENDAR + " График работы";
    public static final String CMD_BLOCKED_USER = ICON_BLOCK + " Заблокировать";
    public static final String CMD_UNBLOCKED_USER = ICON_UNBLOCK + " Разблокировать";
    public static final String CMD_ADMIN_BACK = ICON_BACK + " Назад в Админку";
    public static final String CMD_SHOW_STATS = ICON_STATS + " Статистика пользователей";
    public static final String CMD_ADMIN_EDIT_WORK_SCHEDULE = ICON_SETTINGS + " Редактировать расписание";
    public static final String CMD_ADMIN_SCHEDULE_MENU = ICON_CALENDAR + " Расписание работы";
    public static final String CMD_ADMIN_MANAGE_OVERRIDES = ICON_SETTINGS + " Меню исключений";
    public static final String CMD_ADMIN_ALL_OVERRIDES = ICON_LIST + " Исключения";
    public static final String CMD_ADMIN_ADD_OVERRIDE = ICON_BOOK + " Добавить исключение";
    public static final String CMD_ADMIN_DELETE_OVERRIDE = ICON_HISTORY + " Удалить исключение";
    public static final String CMD_ADMIN_BACK_TO_OVERRIDES = ICON_BACK + " Назад к списку исключений";
    public static final String CMD_ADMIN_MANAGEMENT = ICON_SETTINGS + " Меню назначения админов";
    public static final String CMD_ADMIN_LUNCH_MENU = ICON_LUNCH + "Обеденные перерывы";
    public static final String CMD_ADD_NEW_PAYMENT = ICON_DOLLAR + " Внести новый платеж";
    public static final String CMD_ADMIN_PAYMENT_MENU = "💸 Меню платежей";
    public static final String CMD_PAYMENT_STATS_MENU = ICON_MONEY + " Статистика платежей";
    public static final String CMD_PAYMENT_TODAY = ICON_CALENDARED + " Сегодня";
    public static final String CMD_PAYMENT_TOMORROW = ICON_TOMORROW_CALENDARED + " Завтра";
    public static final String CMD_PAYMENT_YESTERDAY = ICON_YESTERDAY_CALENDARED + " Вчера";
    public static final String CMD_PAYMENT_WEEK = ICON_STATS + " Неделя";
    public static final String CMD_PAYMENT_MONTH = ICON_CALENDAR + " Месяц";
    public static final String CMD_TOTAL_PAYMENT_STATS = ICON_MONEY + " Все время";
    public static final String CMD_PAYMENT_CUSTOM_PERIOD = ICON_HISTORY + " Выбор периода";

    // Форматы
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Redis-префикс
    public static final String PREFIX = "session:";
    public static final String PREFIX_PAYMENT = "payment:";

}
