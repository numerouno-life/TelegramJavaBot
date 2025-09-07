package ru.service;

import ru.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    void setUserState(Long chatId, String status);

    String getUserState(Long chatId);

    void clearUserState(Long chatId);

    Appointment createAppointment(Appointment appointment);

    void cancelAppointment(Long appointmentId);

    List<Appointment> getUserAppointments(Long chatId);

    Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newDateTime);

    boolean isTimeSlotAvailable(LocalDateTime dateTime);

    List<LocalDateTime> getAvailableTimeSlots(LocalDateTime date);

    void setPendingDate(Long chatId, LocalDateTime dateTime);

    LocalDateTime getPendingDate(Long chatId);

    void setPendingName(Long chatId, String name);

    String getPendingName(Long chatId);

    Appointment findById(Long appointmentId);

    List<Appointment> getActiveAppointments(Long chatId);

    List<Appointment> getPastAppointments(Long chatId);

    Appointment updateAppointment(Appointment appointment);

    void setPendingMessageId(Long chatId, Integer messageId);

    Integer getPendingMessageId(Long chatId);

    void clearPendingMessageId(Long chatId);

    int getHistoryPage(Long chatId);

    void setHistoryPage(Long chatId, int page);

    void clearHistoryPage(Long chatId);
}
