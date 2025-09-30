package ru.service;

import ru.model.Appointment;
import ru.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminService {
    List<Appointment> getAllActiveAppointments();

    List<Appointment> getAllAppointments();

    List<Appointment> getAppointmentsToday();

    List<Appointment> getAppointmentsTomorrow();

    Map<LocalDateTime, List<Appointment>> getAppointmentsThisWeek();

    void blockUser(Long userId);

    void unblockUser(Long userId);

    List<User> getBlockedUsers();

    List<User> getAllUsers();

    void sendTimeSelectionForAdmin(Long chatId, Integer messageId, LocalDate date);


}
