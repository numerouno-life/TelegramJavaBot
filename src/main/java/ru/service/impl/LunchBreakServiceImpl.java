package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.model.LunchBreak;
import ru.repository.LunchBreakRepository;
import ru.service.LunchBreakService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LunchBreakServiceImpl implements LunchBreakService {

    private final LunchBreakRepository lunchBreakRepository;

    @Override
    public boolean isLunchTime(LocalDateTime dateTime) {
        log.info("Check if lunch time");
        if (dateTime == null) return false;

        LunchBreak lunchBreak = getLunchBreakByDayOfWeek(dateTime.getDayOfWeek().getValue());
        if (lunchBreak == null || !lunchBreak.getIsActive()) {
            return false;
        }

        LocalTime slotStart = dateTime.toLocalTime();

        // пропускаем только слот, который начинается во время обеда
        // Слот 14:00-15:00 начинается в 14:00, что во время обеда 14:00-15:00 → пропускаем
        // Слот 13:00-14:00 начинается в 13:00, что ДО обеда → НЕ пропускаем
        // Слот 15:00-16:00 начинается в 15:00, что ПОСЛЕ обеда → НЕ пропускаем
        return !slotStart.isBefore(lunchBreak.getStartTime()) && slotStart.isBefore(lunchBreak.getEndTime());
    }

    @Override
    public LunchBreak getLunchBreakByDayOfWeek(Integer dayOfWeek) {
        log.info("Get lunch break by day of week");
        return lunchBreakRepository.findByDayOfWeek(dayOfWeek);
    }

    @Override
    public List<LunchBreak> getAllLunchBreaks() {
        log.info("Get all lunch breaks");
        return lunchBreakRepository.findAllByOrderByDayOfWeekAsc();
    }

    @Override
    @Transactional
    public void updateLunchBreak(Integer dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isActive) {
        log.info("Update lunch break");
        var lunchBreak = lunchBreakRepository.findByDayOfWeek(dayOfWeek);
        if (lunchBreak == null) {
            log.warn("Обеденный перерыв не найден");
            throw new IllegalArgumentException("Обеденный перерыв для дня недели " + dayOfWeek + " не найден");
        }
        lunchBreak.setIsActive(isActive);
        if (isActive) {
            lunchBreak.setStartTime(startTime);
            lunchBreak.setEndTime(endTime);
        } else {
            lunchBreak.setStartTime(null);
            lunchBreak.setEndTime(null);
            lunchBreak.setIsActive(false);
        }
    }
}
