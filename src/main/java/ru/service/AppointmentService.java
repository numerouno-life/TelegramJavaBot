package ru.service;

import ru.model.Appointment;
import ru.model.enums.AdminAppointmentState;
import ru.model.enums.UserAppointmentState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentService {

    void setUserState(Long chatId, UserAppointmentState status);

    UserAppointmentState getUserState(Long chatId);

    void clearUserState(Long chatId);

    Appointment createAppointment(Appointment appointment);

    void cancelAppointment(Long appointmentId);

    List<Appointment> getUserAppointments(Long chatId);

    Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newDateTime);

    boolean isTimeSlotAvailable(LocalDateTime dateTime);

    List<LocalDateTime> getAvailableTimeSlots(LocalDateTime date);

    void setPendingDate(Long chatId, LocalDateTime dateTime);

    LocalDateTime getPendingDate(Long chatId);

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

    boolean isWorkingDay(LocalDate date);

    String getPendingName(Long chatId);

    void setAdminState(Long chatId, AdminAppointmentState state);

    AdminAppointmentState getAdminState(Long chatId);

    void clearAdminState(Long chatId);

    void clearPendingDate(Long chatId);

    void cancellationNoticeForAdmins(Appointment appointment);

    boolean hasAppointmentInLast6Days(Long chatId, LocalDateTime newDateTime);

    Appointment getLastAppointmentWithin6Days(Long chatId, LocalDateTime dateTime);

    Optional<Appointment> getLastAppointment(Long chatId);
}
