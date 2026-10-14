package ru.miniapp.dto;

import java.time.LocalTime;

/**
 * Обеденный перерыв для публичного API расписания.
 * dayOfWeek: 1=Пн, ..., 7=Вс (ISO-8601). isActive=false — перерыв отключён.
 */
public record LunchBreakResponse(
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isActive
) {}
