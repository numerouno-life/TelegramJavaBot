package ru.service;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface NotificationService {
    void sendMessage(Long chatId, String text);

    void sendMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup);

    void sendOrEditMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup replyMarkup);

    void deleteMessage(Long chatId, Integer messageId);

    void sendContacts(Long chatId);

    void sendMainMenu(Long chatId, String text);

    Message sendMessageAndReturn(Long chatId, String text, InlineKeyboardMarkup replyMarkup);
}
