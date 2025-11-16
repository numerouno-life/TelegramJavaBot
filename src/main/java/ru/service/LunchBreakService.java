package ru.service;

import ru.model.LunchBreak;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface LunchBreakService {

    boolean isLunchTime(LocalDateTime dateTime);

    LunchBreak getLunchBreakByDayOfWeek(Integer dayOfWeek);

    List<LunchBreak> getAllLunchBreaks();

    void updateLunchBreak(Integer dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isActive);

}
