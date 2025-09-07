package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.service.NotificationService;
import ru.util.KeyboardFactory;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final TelegramClient telegramClient;
    private final KeyboardFactory keyboardFactory;

    @Override
    public void sendMessage(Long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        execute(msg);
    }

    @Override
    public void sendMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(replyMarkup)
                .build();
        execute(msg);
    }

    @Override
    public void sendOrEditMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup replyMarkup) {
        if (messageId == null) {
            sendMessage(chatId, text, replyMarkup);
        } else {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(replyMarkup)
                    .build();
            execute(edit);
        }
    }

    @Override
    public Message sendMessageAndReturn(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(replyMarkup)
                .build();
        try {
            return telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    //Универсальный метод "с кнопкой внизу"
    public void sendMessageWithBackButton(Long chatId, String text, String buttonText, String callbackData) {
        InlineKeyboardMarkup markup = keyboardFactory.backButton(buttonText, callbackData);
        sendMessage(chatId, text, markup);
    }

    @Override
    public void deleteMessage(Long chatId, Integer messageId) {
        try {
            telegramClient.execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (Exception e) {
            log.warn("Не удалось удалить сообщение {} в чате {}", messageId, chatId, e);
        }
    }

    @Override
    public void sendContacts(Long chatId) {
        String contacts = """
                📍 Наш адрес: ул. Славского, д. 6
                📞 Телефон: +7 (986) 736-70-77
                🕒 Часы работы: 10:00 - 20:00
                💻 Сайт: https://yandex.ru/maps/org/aura/137741913962/?ll=49.560800%2C54.236732&z=16
                """;
        sendOrEditMessage(chatId, null,
                contacts, keyboardFactory.backButton("🏠 В меню", "back_to_menu"));
    }

    @Override
    public void sendMainMenu(Long chatId, String text) {
        sendMessage(chatId, text, keyboardFactory.mainMenu());
    }

    // Универсальный execute
    private <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) {
        try {
            return telegramClient.execute(method);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
