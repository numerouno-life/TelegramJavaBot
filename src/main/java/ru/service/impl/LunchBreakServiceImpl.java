package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
    @Autowired
    private LunchBreakService self;

    @Override
    public boolean isLunchTime(LocalDateTime dateTime) {
        if (dateTime == null) return false;

        LunchBreak lunchBreak = self.getLunchBreakByDayOfWeek(dateTime.getDayOfWeek().getValue());
        if (lunchBreak == null || !lunchBreak.getIsActive()) {
            return false;
        }

        LocalTime slotStart = dateTime.toLocalTime();

        // пропускаем только слот, который начинается во время обеда
        return !slotStart.isBefore(lunchBreak.getStartTime()) && slotStart.isBefore(lunchBreak.getEndTime());
    }

    @Override
    @Cacheable(value = "lunch_break_day", key = "#dayOfWeek")
    public LunchBreak getLunchBreakByDayOfWeek(Integer dayOfWeek) {
        log.info("Get lunch break by day of week: {}", dayOfWeek);
        return lunchBreakRepository.findByDayOfWeek(dayOfWeek);
    }

    @Override
    @Cacheable(value = "lunch_break_all")
    public List<LunchBreak> getAllLunchBreaks() {
        log.info("Get all lunch breaks");
        return lunchBreakRepository.findAllByOrderByDayOfWeekAsc();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"lunch_break_day", "lunch_break_all"}, allEntries = true)
    public void updateLunchBreak(Integer dayOfWeek, LocalTime startTime, LocalTime endTime, boolean isActive) {
        log.info("Update lunch break for day of week: {}", dayOfWeek);
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
