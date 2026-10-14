package ru.miniapp.dto;

import java.time.LocalTime;

/**
 * Рабочий день недели для публичного API расписания.
 * dayOfWeek: 1=Пн, 2=Вт, ..., 7=Вс (ISO-8601).
 */
public record WorkScheduleResponse(
        Integer dayOfWeek,
        String dayName,
        boolean isWorkingDay,
        LocalTime startTime,
        LocalTime endTime
) {}
