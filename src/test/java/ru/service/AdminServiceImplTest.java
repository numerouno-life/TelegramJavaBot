package ru.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.StatusAppointment;
import ru.model.enums.UserRole;
import ru.repository.AppointmentRepository;
import ru.repository.UserRepository;
import ru.service.impl.AdminServiceImpl;
import ru.service.impl.NotificationServiceImpl;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private KeyboardFactory keyboardFactory;

    @Mock
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private AdminServiceImpl adminService;

    // ─────────────────── getAllActiveAppointments ───────────────────

    @Test
    void getAllActiveAppointments_ReturnsOnlyFutureActiveAppointments() {
        Appointment future = Appointment.builder()
                .status(StatusAppointment.ACTIVE)
                .dateTime(LocalDateTime.now().plusDays(1))
                .build();
        Appointment past = Appointment.builder()
                .status(StatusAppointment.ACTIVE)
                .dateTime(LocalDateTime.now().minusDays(1))
                .build();

        when(appointmentRepository.findAllByStatusOrderByDateTimeAsc(StatusAppointment.ACTIVE))
                .thenReturn(List.of(future, past));

        List<Appointment> result = adminService.getAllActiveAppointments();

        assertEquals(1, result.size());
        assertEquals(future, result.get(0));
    }

    @Test
    void getAllActiveAppointments_ReturnsEmpty_WhenNone() {
        when(appointmentRepository.findAllByStatusOrderByDateTimeAsc(StatusAppointment.ACTIVE))
                .thenReturn(List.of());

        assertTrue(adminService.getAllActiveAppointments().isEmpty());
    }

    // ─────────────────── getAppointmentsToday / Tomorrow ───────────────────

    @Test
    void getAppointmentsToday_ReturnsAppointmentsForToday() {
        LocalDateTime today = LocalDateTime.now();
        Appointment app = Appointment.builder()
                .dateTime(today)
                .status(StatusAppointment.ACTIVE)
                .build();

        when(appointmentRepository.findByDateTimeBetweenAndStatusOrderByDateTimeAsc(
                any(), any(), eq(StatusAppointment.ACTIVE)))
                .thenReturn(List.of(app));

        List<Appointment> result = adminService.getAppointmentsToday();

        assertEquals(1, result.size());
    }

    @Test
    void getAppointmentsTomorrow_ReturnsAppointmentsForTomorrow() {
        Appointment app = Appointment.builder()
                .dateTime(LocalDateTime.now().plusDays(1))
                .status(StatusAppointment.ACTIVE)
                .build();

        when(appointmentRepository.findByDateTimeBetweenAndStatusOrderByDateTimeAsc(
                any(), any(), eq(StatusAppointment.ACTIVE)))
                .thenReturn(List.of(app));

        List<Appointment> result = adminService.getAppointmentsTomorrow();

        assertEquals(1, result.size());
    }

    // ─────────────────── getAppointmentsThisWeek ───────────────────

    @Test
    void getAppointmentsThisWeek_Returns7DayMap() {
        when(appointmentRepository.findByDateTimeBetweenAndStatusOrderByDateTimeAsc(
                any(), any(), eq(StatusAppointment.ACTIVE)))
                .thenReturn(List.of());

        Map<LocalDateTime, List<Appointment>> week = adminService.getAppointmentsThisWeek();

        assertEquals(7, week.size());
    }

    // ─────────────────── blockUser / unblockUser ───────────────────

    @Test
    void blockUser_SetsIsBlockedTrue() {
        User user = User.builder().telegramId(1L).isBlocked(false).build();
        when(userRepository.findByTelegramId(1L)).thenReturn(Optional.of(user));

        adminService.blockUser(1L);

        assertTrue(user.getIsBlocked());
        verify(userRepository).save(user);
    }

    @Test
    void blockUser_ThrowsWhenUserNotFound() {
        when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> adminService.blockUser(999L));
    }

    @Test
    void unblockUser_SetsIsBlockedFalse() {
        User user = User.builder().telegramId(2L).isBlocked(true).build();
        when(userRepository.findByTelegramId(2L)).thenReturn(Optional.of(user));

        adminService.unblockUser(2L);

        assertFalse(user.getIsBlocked());
        verify(userRepository).save(user);
    }

    @Test
    void unblockUser_DoesNothing_WhenUserNotFound() {
        when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> adminService.unblockUser(999L));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────── getBlockedUsers / getAllUsers ───────────────────

    @Test
    void getBlockedUsers_ReturnsEmptyList_WhenNoneBlocked() {
        when(userRepository.findAllByIsBlockedTrue()).thenReturn(List.of());

        assertTrue(adminService.getBlockedUsers().isEmpty());
    }

    @Test
    void getBlockedUsers_ReturnsBlockedUsers() {
        User blocked = User.builder().isBlocked(true).build();
        when(userRepository.findAllByIsBlockedTrue()).thenReturn(List.of(blocked));

        assertEquals(1, adminService.getBlockedUsers().size());
    }

    @Test
    void getAllUsers_ReturnsEmptyList_WhenNone() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertTrue(adminService.getAllUsers().isEmpty());
    }

    @Test
    void getAllUsers_ReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(new User(), new User()));

        assertEquals(2, adminService.getAllUsers().size());
    }

    // ─────────────────── assignAdmin / removeAdmin ───────────────────

    @Test
    void assignAdmin_SetsRoleAdmin() {
        User user = User.builder().telegramId(3L).role(UserRole.USER).build();
        when(userRepository.findByTelegramId(3L)).thenReturn(Optional.of(user));

        adminService.assignAdmin(3L);

        assertEquals(UserRole.ADMIN, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void removeAdmin_SetsRoleUser() {
        User user = User.builder().telegramId(4L).role(UserRole.ADMIN).build();
        when(userRepository.findByTelegramId(4L)).thenReturn(Optional.of(user));

        adminService.removeAdmin(4L);

        assertEquals(UserRole.USER, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void assignAdmin_DoesNothing_WhenUserNotFound() {
        when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> adminService.assignAdmin(999L));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────── sendTimeSelectionForAdmin ───────────────────

    @Test
    void sendTimeSelectionForAdmin_CallsNotificationService() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(appointmentService.getAvailableTimeSlots(any(LocalDateTime.class))).thenReturn(List.of());
        when(keyboardFactory.timeSelectionKeyboard(any(), any(), any())).thenReturn(null);

        assertDoesNotThrow(() -> adminService.sendTimeSelectionForAdmin(123L, null, date));

        verify(notificationService).sendOrEditMessage(eq(123L), isNull(), anyString(), any());
    }
}
