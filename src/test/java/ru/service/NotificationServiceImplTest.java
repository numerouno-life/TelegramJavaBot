package ru.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.service.impl.NotificationServiceImpl;
import ru.util.AdminKeyboard;
import ru.util.KeyboardFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private KeyboardFactory keyboardFactory;

    @Mock
    private AdminKeyboard adminKeyboard;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ─────────────────── sendMessage ───────────────────

    @Test
    void sendMessage_WithoutMarkup_SendsCorrectMessage() throws TelegramApiException {
        notificationService.sendMessage(100L, "Hello");

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals("100", captor.getValue().getChatId());
        assertEquals("Hello", captor.getValue().getText());
    }

    @Test
    void sendMessage_WithMarkup_IncludesReplyMarkup() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);

        notificationService.sendMessage(200L, "Hi", markup);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals(markup, captor.getValue().getReplyMarkup());
    }

    // ─────────────────── sendOrEditMessage ───────────────────

    @Test
    void sendOrEditMessage_SendsNewMessage_WhenMessageIdIsNull() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);

        notificationService.sendOrEditMessage(300L, null, "New text", markup);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals("New text", captor.getValue().getText());
    }

    @Test
    void sendOrEditMessage_EditsMessage_WhenMessageIdIsProvided() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);

        notificationService.sendOrEditMessage(400L, 42, "Edited", markup);

        ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals("Edited", captor.getValue().getText());
        assertEquals(42, captor.getValue().getMessageId());
    }

    @Test
    void sendOrEditMessage_SendsNewMessage_WhenEditFails() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);
        when(telegramClient.execute(any(EditMessageText.class)))
                .thenThrow(new TelegramApiException("some error"));

        notificationService.sendOrEditMessage(500L, 99, "Fallback", markup);

        // Falls back to SendMessage
        verify(telegramClient).execute(any(SendMessage.class));
    }

    @Test
    void sendOrEditMessage_DoesNotSendNew_WhenMessageNotModified() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);
        when(telegramClient.execute(any(EditMessageText.class)))
                .thenThrow(new TelegramApiException("message is not modified"));

        notificationService.sendOrEditMessage(600L, 10, "Same text", markup);

        // Should NOT fall back to send; only edit attempted
        verify(telegramClient, times(1)).execute(any(EditMessageText.class));
        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    // ─────────────────── sendMessageAndReturn ───────────────────

    @Test
    void sendMessageAndReturn_ReturnsMessage() throws TelegramApiException {
        Message msg = mock(Message.class);
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(msg);

        Message result = notificationService.sendMessageAndReturn(700L, "Text", markup);

        assertNotNull(result);
        assertEquals(msg, result);
    }

    @Test
    void sendMessageAndReturn_ThrowsRuntime_OnTelegramException() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);
        when(telegramClient.execute(any(SendMessage.class)))
                .thenThrow(new TelegramApiException("error"));

        assertThrows(RuntimeException.class,
                () -> notificationService.sendMessageAndReturn(800L, "Text", markup));
    }

    // ─────────────────── deleteMessage ───────────────────

    @Test
    void deleteMessage_ExecutesDeleteMessage() throws TelegramApiException {
        notificationService.deleteMessage(900L, 55);

        ArgumentCaptor<DeleteMessage> captor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals("900", captor.getValue().getChatId());
        assertEquals(55, captor.getValue().getMessageId());
    }

    @Test
    void deleteMessage_DoesNothing_WhenChatIdNull() throws TelegramApiException {
        notificationService.deleteMessage(null, 1);

        verify(telegramClient, never()).execute(any(DeleteMessage.class));
    }

    @Test
    void deleteMessage_DoesNothing_WhenMessageIdNull() throws TelegramApiException {
        notificationService.deleteMessage(100L, null);

        verify(telegramClient, never()).execute(any(DeleteMessage.class));
    }

    @Test
    void deleteMessage_DoesNotThrow_WhenTelegramFails() throws TelegramApiException {
        when(telegramClient.execute(any(DeleteMessage.class)))
                .thenThrow(new TelegramApiException("cannot delete"));

        assertDoesNotThrow(() -> notificationService.deleteMessage(100L, 1));
    }

    // ─────────────────── answerCallbackQuery ───────────────────

    @Test
    void answerCallbackQuery_Simple_ExecutesCorrectly() throws TelegramApiException {
        notificationService.answerCallbackQuery("cb_id_123");

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals("cb_id_123", captor.getValue().getCallbackQueryId());
    }

    @Test
    void answerCallbackQuery_DoesNothing_WhenIdIsBlank() throws TelegramApiException {
        notificationService.answerCallbackQuery("   ");

        verify(telegramClient, never()).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void answerCallbackQuery_DoesNothing_WhenIdIsNull() throws TelegramApiException {
        notificationService.answerCallbackQuery(null);

        verify(telegramClient, never()).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void answerCallbackQuery_WithText_SetsTextAndAlert() throws TelegramApiException {
        notificationService.answerCallbackQuery("cb_456", "Alert text", true);

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals("Alert text", captor.getValue().getText());
        assertTrue(captor.getValue().getShowAlert());
    }

    @Test
    void answerCallbackQuery_WithText_DoesNotThrow_OnTelegramError() throws TelegramApiException {
        when(telegramClient.execute(any(AnswerCallbackQuery.class)))
                .thenThrow(new TelegramApiException("error"));

        assertDoesNotThrow(() -> notificationService.answerCallbackQuery("cb_789", "msg", false));
    }

    // ─────────────────── sendContacts ───────────────────

    @Test
    void sendContacts_WithoutMessageId_SendsNewMessage() throws TelegramApiException {
        InlineKeyboardMarkup markup = mock(InlineKeyboardMarkup.class);
        when(keyboardFactory.backButton(anyString(), anyString())).thenReturn(markup);

        notificationService.sendContacts(100L);

        verify(telegramClient).execute(any(SendMessage.class));
    }
}
