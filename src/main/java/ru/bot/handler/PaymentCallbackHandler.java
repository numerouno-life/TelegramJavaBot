package ru.bot.handler;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface PaymentCallbackHandler {
    void handlePaymentCallback(CallbackQuery callbackQuery);
}
