package ru.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface WorkScheduleService {

    boolean isWorkingDay(LocalDate date);

    LocalTime[] getWorkTimeForDate(LocalDate date);

    List<LocalDateTime> getAvailableTimeSlots(LocalDate date);

    void updateWorkDay(Integer dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isWorking);

    void setWorkDayOverride(LocalDate date, LocalTime start, LocalTime end, boolean isWorking, String reason);
}
