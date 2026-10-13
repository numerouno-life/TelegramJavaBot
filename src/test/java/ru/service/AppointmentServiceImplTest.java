package ru.service;

import org.junit.jupiter.api.BeforeEach;
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
import ru.scheduler.AppointmentNotificationScheduler;
import ru.service.impl.AppointmentServiceImpl;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentNotificationScheduler notificationScheduler;

    @Mock
    private WorkScheduleService workScheduleService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeyboardFactory keyboardFactory;

    @Mock
    private LunchBreakService lunchBreakService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(2); // In the future
    }

    @Test
    void getAvailableTimeSlots_ReturnsAvailableSlots_WhenWorkingDay() {
        LocalTime[] workTime = new LocalTime[]{LocalTime.of(10, 0), LocalTime.of(12, 0)};

        when(workScheduleService.isWorkingDay(testDate)).thenReturn(true);
        when(workScheduleService.getWorkTimeForDate(testDate)).thenReturn(workTime);

        // One slot at 11:00 is booked
        Appointment booked = Appointment.builder()
                .dateTime(testDate.atTime(11, 0))
                .status(StatusAppointment.ACTIVE)
                .build();

        when(appointmentRepository.findByDateTimeBetweenAndStatusOrderByDateTimeAsc(
                eq(testDate.atStartOfDay()), eq(testDate.atTime(23, 59, 59)), eq(StatusAppointment.ACTIVE)))
                .thenReturn(List.of(booked));

        when(lunchBreakService.isLunchTime(any(LocalDateTime.class))).thenReturn(false);

        List<LocalDateTime> slots = appointmentService.getAvailableTimeSlots(testDate.atStartOfDay());

        // Slots should be 10:00 and 12:00, with 11:00 excluded
        assertEquals(2, slots.size());
        assertTrue(slots.contains(testDate.atTime(10, 0)));
        assertFalse(slots.contains(testDate.atTime(11, 0)));
        assertTrue(slots.contains(testDate.atTime(12, 0)));

        // Verify only 1 batch query was made to repository
        verify(appointmentRepository, times(1))
                .findByDateTimeBetweenAndStatusOrderByDateTimeAsc(any(), any(), any());
    }

    @Test
    void getAvailableDates_ReturnsDatesWithAvailableSlots_BatchQuerySingleCall() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        int daysCount = 3;

        when(workScheduleService.isWorkingDay(any(LocalDate.class))).thenReturn(true);
        when(workScheduleService.getWorkTimeForDate(any(LocalDate.class)))
                .thenReturn(new LocalTime[]{LocalTime.of(10, 0), LocalTime.of(18, 0)});
        when(lunchBreakService.isLunchTime(any(LocalDateTime.class))).thenReturn(false);

        when(appointmentRepository.findByDateTimeBetweenAndStatusOrderByDateTimeAsc(
                eq(startDate.atStartOfDay()), eq(startDate.plusDays(daysCount).atTime(23, 59, 59)), eq(StatusAppointment.ACTIVE)))
                .thenReturn(List.of());

        List<LocalDate> availableDates = appointmentService.getAvailableDates(startDate, daysCount);

        assertEquals(3, availableDates.size());
        // Verify only 1 query across all days
        verify(appointmentRepository, times(1))
                .findByDateTimeBetweenAndStatusOrderByDateTimeAsc(any(), any(), any());
    }

    @Test
    void cancellationNoticeForAdmins_SendsAsynchronously() throws Exception {
        User admin = User.builder()
                .telegramId(12345L)
                .role(UserRole.ADMIN)
                .isBlocked(false)
                .build();

        when(userRepository.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

        User client = User.builder()
                .firstName("Иван")
                .clientPhoneNumber("+79991234567")
                .build();

        Appointment appointment = Appointment.builder()
                .id(1L)
                .dateTime(LocalDateTime.now().plusDays(1))
                .user(client)
                .build();

        appointmentService.cancellationNoticeForAdmins(appointment);

        // Allow async thread to run
        verify(userRepository, timeout(1000).times(1)).findAllByRole(UserRole.ADMIN);
        verify(notificationService, timeout(1000).times(1))
                .sendOrEditMessage(eq(12345L), isNull(), anyString(), any());
    }
}
