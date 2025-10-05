package ru.service;

import ru.model.enums.AdminAppointmentState;
import ru.model.enums.UserAppointmentState;

import java.time.LocalDateTime;
import java.time.LocalTime;

public interface UserSessionService {

    void setUserState(Long chatId, UserAppointmentState state);

    UserAppointmentState getUserState(Long chatId);

    void setPendingDate(Long chatId, LocalDateTime dateTime);

    LocalDateTime getPendingDate(Long chatId);

    void setPendingMessageId(Long chatId, Integer messageId);

    Integer getPendingMessageId(Long chatId);

    void clearPendingMessageId(Long chatId);

    void setHistoryPage(Long chatId, Integer page);

    Integer getHistoryPage(Long chatId);

    void clearHistoryPage(Long chatId);

    void clearUserState(Long chatId);

    void setRole(Long chatId, String role);

    String getRole(Long chatId);

    void clearRole(Long chatId);

    void setPendingName(Long chatId, String name);

    String getPendingName(Long chatId);

    void clearPendingName(Long chatId);

    void setAdminState(Long chatId, AdminAppointmentState state);

    AdminAppointmentState getAdminState(Long chatId);

    void clearAdminState(Long chatId);

    void clearPendingDate(Long chatId);

    void setPendingStartTime(Long chatId, LocalTime time);

    LocalTime getPendingStartTime(Long chatId);

    void clearPendingStartTime(Long chatId);

    void setPendingEndTime(Long chatId, LocalTime time);

    LocalTime getPendingEndTime(Long chatId);

    void clearPendingEndTime(Long chatId);

    void clearAllSessions(Long chatId);
}
