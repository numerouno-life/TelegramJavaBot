package ru.util;

import java.time.format.DateTimeFormatter;

public class BotConstants {

    public static final String CMD_START = "/start";
    public static final String CMD_BEGIN = "начать";
    public static final String CMD_BOOK = "💇 Записаться";
    public static final String CMD_MY_APPOINTMENTS = "📋 Мои записи";
    public static final String CMD_CONTACTS = "📞 Контакты";
    public static final String CMD_HISTORY = "📝 История записей";

    public static final String STATE_AWAITING_NAME = "AWAITING_NAME";
    public static final String STATE_AWAITING_PHONE = "AWAITING_PHONE";

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static final String PREFIX = "session:";

}
