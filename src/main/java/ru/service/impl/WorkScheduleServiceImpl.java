package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.model.WorkDaysOverride;
import ru.model.WorkSchedule;
import ru.model.enums.StatusAppointment;
import ru.repository.AppointmentRepository;
import ru.repository.WorkDaysOverrideRepository;
import ru.repository.WorkScheduleRepository;
import ru.service.WorkScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final WorkDaysOverrideRepository workDaysOverrideRepository;
    private final AppointmentRepository appointmentRepository;

    // Проверить, рабочий ли день
    @Override
    public boolean isWorkingDay(LocalDate date) {
        log.info("isWorkDay");
        // Проверяем исключения
        var override = workDaysOverrideRepository.findByDate(date);
        if (override.isPresent()) {
            return override.get().getIsWorkingDay();
        }
        // Проверяем по дню недели
        int dayOfWeek = date.getDayOfWeek().getValue();
        var schedule = workScheduleRepository.findByDayOfWeek(dayOfWeek);
        return schedule != null && schedule.getIsWorkingDay();
    }

    // Получить рабочее время для даты
    public LocalTime[] getWorkTimeForDate(LocalDate date) {
        var override = workDaysOverrideRepository.findByDate(date);
        if (override.isPresent() && override.get().getIsWorkingDay()) {
            return new LocalTime[]{
                    override.get().getStartTime(),
                    override.get().getEndTime()
            };
        }

        int dayOfWeek = date.getDayOfWeek().getValue();
        var schedule = workScheduleRepository.findByDayOfWeek(dayOfWeek);
        if (schedule != null && schedule.getIsWorkingDay()) {
            return new LocalTime[]{
                    schedule.getStartTime(),
                    schedule.getEndTime()
            };
        }

        return null; // не рабочий день
    }

    // Получить доступные слоты на дату (по 1 часу)
    @Override
    public List<LocalDateTime> getAvailableTimeSlots(LocalDate date) {
        if (!isWorkingDay(date)) {
            return List.of();
        }

        LocalTime[] workTime = getWorkTimeForDate(date);
        if (workTime == null) return List.of();

        LocalTime start = workTime[0];
        LocalTime end = workTime[1];

        LocalDateTime current = date.atTime(start);
        LocalDateTime endTime = date.atTime(end);
        List<LocalDateTime> slots = new ArrayList<>();

        while (current.isBefore(endTime)) {
            if (current.isAfter(LocalDateTime.now()) && isTimeSlotAvailable(current)) {
                slots.add(current);
            }
            current = current.plusHours(1);
        }
        return slots;
    }

    private boolean isTimeSlotAvailable(LocalDateTime dateTime) {
        return appointmentRepository.findByDateTime(dateTime).stream()
                .noneMatch(appointment -> appointment.getStatus() != StatusAppointment.CANCELED);
    }

    @Transactional
    @Override
    public void updateWorkDay(Integer dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isWorking) {
        var schedule = workScheduleRepository.findByDayOfWeek(dayOfWeek);
        if (schedule == null) {
            throw new IllegalArgumentException("График для дня недели " + dayOfWeek + " не найден");
        }
        schedule.setIsWorkingDay(isWorking);
        if (isWorking) {
            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
        } else {
            schedule.setStartTime(null);
            schedule.setEndTime(null);
        }
    }

    // Установить исключение (например, выходной или сокращённый день)
    @Transactional
    @Override
    public void setWorkDayOverride(LocalDate date, LocalTime start, LocalTime end,
                                   boolean isWorking, String reason) {
        var override = workDaysOverrideRepository.findByDate(date).orElse(null);

        if (override == null && !isWorking && (start == null || end == null)) {
            return; // не создаём запись для нерабочего дня без причины
        }

        if (override == null) {
            override = WorkDaysOverride.builder()
                    .date(date)
                    .startTime(start)
                    .endTime(end)
                    .isWorkingDay(isWorking)
                    .reason(reason)
                    .build();
        } else {
            override.setStartTime(start);
            override.setEndTime(end);
            override.setIsWorkingDay(isWorking);
            override.setReason(reason);
        }
        workDaysOverrideRepository.save(override);
    }

    @Transactional(readOnly = true)
    @Override
    public List<WorkSchedule> getAllWorkSchedules() {
        return workScheduleRepository.findAllByOrderByDayOfWeekAsc();
    }

    @Transactional(readOnly = true)
    @Override
    public List<WorkSchedule> getAllWorkSchedule() {
        return workScheduleRepository.findAll();
    }
}
