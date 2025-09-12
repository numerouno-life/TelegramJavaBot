package ru.service;

import java.time.LocalDateTime;

public interface UserSessionService {

    void setUserState(Long chatId, String state);

    String getUserState(Long chatId);

    void setPendingDate(Long chatId, LocalDateTime dateTime);

    LocalDateTime getPendingDate(Long chatId);

    void setPendingMessageId(Long chatId, Integer messageId);

    Integer getPendingMessageId(Long chatId);

    void clearPendingMessageId(Long chatId);

    void setHistoryPage(Long chatId, Integer page);

    Integer getHistoryPage(Long chatId);

    void clearHistoryPage(Long chatId);

    void clearUserState(Long chatId);
}
