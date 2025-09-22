package ru.bot.handler;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface AdminCallbackHandler {
    void handleAdminCallback(CallbackQuery callbackQuery);
}
