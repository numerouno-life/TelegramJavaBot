package ru.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.model.WorkDaysOverride;
import ru.model.WorkSchedule;
import ru.model.enums.StatusAppointment;
import ru.repository.AppointmentRepository;
import ru.repository.WorkDaysOverrideRepository;
import ru.repository.WorkScheduleRepository;
import ru.service.impl.WorkScheduleServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkScheduleServiceImplTest {

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private WorkDaysOverrideRepository workDaysOverrideRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private WorkScheduleServiceImpl workScheduleService;

    // ─────────────────── isWorkingDay ───────────────────

    @Test
    void isWorkingDay_ReturnsTrue_WhenScheduleSaysWorking() {
        LocalDate date = LocalDate.of(2026, 10, 12); // Monday (dayOfWeek=1)
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        WorkSchedule schedule = new WorkSchedule();
        schedule.setDayOfWeek(1);
        schedule.setIsWorkingDay(true);
        when(workScheduleRepository.findByDayOfWeek(1)).thenReturn(schedule);

        assertTrue(workScheduleService.isWorkingDay(date));
    }

    @Test
    void isWorkingDay_ReturnsFalse_WhenScheduleSaysNonWorking() {
        LocalDate date = LocalDate.of(2026, 10, 11); // Sunday (dayOfWeek=7)
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        WorkSchedule schedule = new WorkSchedule();
        schedule.setDayOfWeek(7);
        schedule.setIsWorkingDay(false);
        when(workScheduleRepository.findByDayOfWeek(7)).thenReturn(schedule);

        assertFalse(workScheduleService.isWorkingDay(date));
    }

    @Test
    void isWorkingDay_UsesOverride_WhenPresent() {
        LocalDate date = LocalDate.of(2026, 10, 12);
        WorkDaysOverride override = WorkDaysOverride.builder()
                .date(date)
                .isWorkingDay(false) // override: non-working even though normally working
                .build();
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.of(override));

        assertFalse(workScheduleService.isWorkingDay(date));
        // schedule should NOT be queried when override exists
        verify(workScheduleRepository, never()).findByDayOfWeek(anyInt());
    }

    @Test
    void isWorkingDay_ReturnsFalse_WhenScheduleNull() {
        LocalDate date = LocalDate.of(2026, 10, 12);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());
        when(workScheduleRepository.findByDayOfWeek(anyInt())).thenReturn(null);

        assertFalse(workScheduleService.isWorkingDay(date));
    }

    // ─────────────────── getWorkTimeForDate ───────────────────

    @Test
    void getWorkTimeForDate_ReturnsScheduleTime_WhenNoOverride() {
        LocalDate date = LocalDate.of(2026, 10, 12);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        WorkSchedule schedule = new WorkSchedule();
        schedule.setIsWorkingDay(true);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(18, 0));
        when(workScheduleRepository.findByDayOfWeek(1)).thenReturn(schedule);

        LocalTime[] result = workScheduleService.getWorkTimeForDate(date);

        assertNotNull(result);
        assertEquals(LocalTime.of(9, 0), result[0]);
        assertEquals(LocalTime.of(18, 0), result[1]);
    }

    @Test
    void getWorkTimeForDate_ReturnsOverrideTime_WhenOverrideExists() {
        LocalDate date = LocalDate.of(2026, 10, 12);
        WorkDaysOverride override = WorkDaysOverride.builder()
                .date(date)
                .isWorkingDay(true)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(15, 0))
                .build();
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.of(override));

        LocalTime[] result = workScheduleService.getWorkTimeForDate(date);

        assertNotNull(result);
        assertEquals(LocalTime.of(11, 0), result[0]);
        assertEquals(LocalTime.of(15, 0), result[1]);
    }

    @Test
    void getWorkTimeForDate_ReturnsNull_WhenNonWorkingDay() {
        LocalDate date = LocalDate.of(2026, 10, 11);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        WorkSchedule schedule = new WorkSchedule();
        schedule.setIsWorkingDay(false);
        when(workScheduleRepository.findByDayOfWeek(anyInt())).thenReturn(schedule);

        assertNull(workScheduleService.getWorkTimeForDate(date));
    }

    // ─────────────────── getAvailableTimeSlots ───────────────────

    @Test
    void getAvailableTimeSlots_ReturnsEmptyList_WhenNonWorkingDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());
        WorkSchedule schedule = new WorkSchedule();
        schedule.setIsWorkingDay(false);
        when(workScheduleRepository.findByDayOfWeek(anyInt())).thenReturn(schedule);

        List<LocalDateTime> slots = workScheduleService.getAvailableTimeSlots(date);

        assertTrue(slots.isEmpty());
    }

    @Test
    void getAvailableTimeSlots_ReturnsSlots_WhenWorkingDayWithNoBookings() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        WorkSchedule schedule = new WorkSchedule();
        schedule.setIsWorkingDay(true);
        schedule.setDayOfWeek(date.getDayOfWeek().getValue());
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        when(workScheduleRepository.findByDayOfWeek(date.getDayOfWeek().getValue())).thenReturn(schedule);

        // No booked appointments
        when(appointmentRepository.findByDateTime(any(LocalDateTime.class))).thenReturn(List.of());

        List<LocalDateTime> slots = workScheduleService.getAvailableTimeSlots(date);

        // 10:00, 11:00, 12:00 — but only future slots count
        assertFalse(slots.isEmpty());
        slots.forEach(slot -> assertTrue(slot.isAfter(LocalDateTime.now())));
    }

    @Test
    void getAvailableTimeSlots_ExcludesBookedSlots() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        WorkSchedule schedule = new WorkSchedule();
        schedule.setIsWorkingDay(true);
        schedule.setDayOfWeek(date.getDayOfWeek().getValue());
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(11, 0));
        when(workScheduleRepository.findByDayOfWeek(date.getDayOfWeek().getValue())).thenReturn(schedule);

        // Slot 10:00 is booked with ACTIVE status
        ru.model.Appointment booked = ru.model.Appointment.builder()
                .dateTime(date.atTime(10, 0))
                .status(StatusAppointment.ACTIVE)
                .build();
        lenient().when(appointmentRepository.findByDateTime(any(LocalDateTime.class))).thenReturn(List.of());
        when(appointmentRepository.findByDateTime(date.atTime(10, 0))).thenReturn(List.of(booked));

        List<LocalDateTime> slots = workScheduleService.getAvailableTimeSlots(date);

        assertFalse(slots.contains(date.atTime(10, 0)));
    }

    // ─────────────────── updateWorkDay ───────────────────

    @Test
    void updateWorkDay_UpdatesSchedule_WhenFound() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setDayOfWeek(2);
        schedule.setIsWorkingDay(false);
        when(workScheduleRepository.findByDayOfWeek(2)).thenReturn(schedule);

        workScheduleService.updateWorkDay(2, LocalTime.of(9, 0), LocalTime.of(18, 0), true);

        assertTrue(schedule.getIsWorkingDay());
        assertEquals(LocalTime.of(9, 0), schedule.getStartTime());
        assertEquals(LocalTime.of(18, 0), schedule.getEndTime());
    }

    @Test
    void updateWorkDay_ClearsTimes_WhenSetToNonWorking() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setDayOfWeek(6);
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(18, 0));
        schedule.setIsWorkingDay(true);
        when(workScheduleRepository.findByDayOfWeek(6)).thenReturn(schedule);

        workScheduleService.updateWorkDay(6, null, null, false);

        assertFalse(schedule.getIsWorkingDay());
        assertNull(schedule.getStartTime());
        assertNull(schedule.getEndTime());
    }

    @Test
    void updateWorkDay_ThrowsIllegalArgument_WhenScheduleNotFound() {
        when(workScheduleRepository.findByDayOfWeek(9)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> workScheduleService.updateWorkDay(9, null, null, false));
    }

    // ─────────────────── setWorkDayOverride ───────────────────

    @Test
    void setWorkDayOverride_CreatesNewOverride_WhenNotExists() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.empty());

        workScheduleService.setWorkDayOverride(date, LocalTime.of(10, 0), LocalTime.of(14, 0), true, "Holiday");

        verify(workDaysOverrideRepository).save(any(WorkDaysOverride.class));
    }

    @Test
    void setWorkDayOverride_UpdatesExistingOverride() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        WorkDaysOverride existing = WorkDaysOverride.builder()
                .date(date)
                .isWorkingDay(true)
                .build();
        when(workDaysOverrideRepository.findByDate(date)).thenReturn(Optional.of(existing));

        workScheduleService.setWorkDayOverride(date, null, null, false, "Christmas");

        assertFalse(existing.getIsWorkingDay());
        assertEquals("Christmas", existing.getReason());
        verify(workDaysOverrideRepository).save(existing);
    }

    // ─────────────────── getAllWorkSchedules / deleteOverrideByDate ───────────────────

    @Test
    void getAllWorkSchedules_ReturnsSortedList() {
        List<WorkSchedule> schedules = List.of(new WorkSchedule(), new WorkSchedule());
        when(workScheduleRepository.findAllByOrderByDayOfWeekAsc()).thenReturn(schedules);

        assertEquals(2, workScheduleService.getAllWorkSchedules().size());
    }

    @Test
    void deleteOverrideByDate_CallsRepository() {
        LocalDate date = LocalDate.of(2026, 11, 1);

        workScheduleService.deleteOverrideByDate(date);

        verify(workDaysOverrideRepository).deleteByDate(date);
    }
}
