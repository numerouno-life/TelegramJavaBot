package ru.bot.handler;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface UserCallBackHandler {

    void handleUserCallback(CallbackQuery callbackQuery);
}
