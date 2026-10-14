package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.miniapp.config.MiniAppProperties;
import ru.service.NotificationService;
import ru.util.AdminKeyboard;
import ru.util.KeyboardFactory;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final TelegramClient telegramClient;
    private final KeyboardFactory keyboardFactory;
    private final AdminKeyboard adminKeyboard;
    private final MiniAppProperties miniAppProperties;

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
            try {
                EditMessageText edit = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(text)
                        .replyMarkup(replyMarkup)
                        .build();
                execute(edit);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("message is not modified")) {
                    log.debug("Сообщение {} не изменилось, повторная отправка не требуется", messageId);
                    return;
                }
                log.warn("Не удалось отредактировать сообщение {}, отправляю новое сообщение", messageId, e);
                sendMessage(chatId, text, replyMarkup);
            }
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
        if (chatId == null || messageId == null) {
            log.warn("Не удалось удалить сообщение: chatId или messageId равен null");
            return;
        }
        try {
            telegramClient.execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (Exception e) {
            log.warn("Не удалось удалить сообщение {} в чате {}", messageId, chatId, e);
        }
    }

    @Override
    public void sendContacts(Long chatId) {
        sendContacts(chatId, null);
    }

    @Override
    public void sendContacts(Long chatId, Integer messageId) {
        String contacts = """
                📍 Наш адрес: ул. Славского, д. 6
                📞 Телефон: +7 (986) 736-70-77
                🕒 Часы работы: 10:00 - 20:00
                💻 Сайт: https://yandex.ru/maps/org/aura/137741913962/?ll=49.560800%2C54.236732&z=16
                """;
        sendOrEditMessage(chatId, messageId,
                contacts, keyboardFactory.backButton("🏠 В меню", "back_to_menu"));
    }

    @Override
    public void sendMainMenu(Long chatId, String text) {
        // Если задан URL Mini App — добавляем кнопку открытия приложения
        String miniAppUrl = miniAppProperties.getUrl();
        if (miniAppUrl != null && !miniAppUrl.isBlank()) {
            sendMessage(chatId, text, keyboardFactory.mainMenuWithMiniApp(miniAppUrl));
        } else {
            sendMessage(chatId, text, keyboardFactory.mainMenu());
        }
    }

    @Override
    public void sendAdminMenu(Long chatId, String text) {
        sendMessage(chatId, text, adminKeyboard.getMainAdminMenu());
    }

    @Override
    public void getUserManagementMenu(Long chatId, String text) {
        sendMessage(chatId, text, adminKeyboard.getUserManagementMenu());
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .build());
        } catch (Exception e) {
            log.warn("Не удалось закрыть callback query {}: {}", callbackQueryId, e.getMessage());
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text(text)
                    .showAlert(showAlert)
                    .build());
        } catch (Exception e) {
            log.warn("Не удалось закрыть callback query {}: {}", callbackQueryId, e.getMessage());
        }
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
