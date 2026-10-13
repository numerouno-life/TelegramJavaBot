package ru.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.model.LunchBreak;
import ru.repository.LunchBreakRepository;
import ru.service.impl.LunchBreakServiceImpl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LunchBreakServiceImplTest {

    @Mock
    private LunchBreakRepository lunchBreakRepository;

    /**
     * LunchBreakServiceImpl использует self-injection через @Lazy для кэширования.
     * В тестах мы мокируем интерфейс LunchBreakService и инжектируем его через ReflectionTestUtils,
     * чтобы обойти приватное поле self.
     */
    @Mock
    private LunchBreakService selfMock;

    @InjectMocks
    private LunchBreakServiceImpl lunchBreakService;

    @BeforeEach
    void injectSelf() {
        // self поле приватное (@Lazy @Autowired), инжектируем через рефлексию
        ReflectionTestUtils.setField(lunchBreakService, "self", selfMock);
    }

    // ─────────────────── isLunchTime ───────────────────

    @Test
    void isLunchTime_ReturnsTrue_WhenSlotDuringActiveLunch() {
        // Обед 13:00–14:00, слот 13:00 в понедельник (dayOfWeek=1)
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(1);
        lunch.setStartTime(LocalTime.of(13, 0));
        lunch.setEndTime(LocalTime.of(14, 0));
        lunch.setIsActive(true);

        LocalDateTime slot = LocalDateTime.of(2026, 10, 12, 13, 0); // Monday
        when(selfMock.getLunchBreakByDayOfWeek(slot.getDayOfWeek().getValue())).thenReturn(lunch);

        assertTrue(lunchBreakService.isLunchTime(slot));
    }

    @Test
    void isLunchTime_ReturnsFalse_WhenSlotBeforeLunch() {
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(1);
        lunch.setStartTime(LocalTime.of(13, 0));
        lunch.setEndTime(LocalTime.of(14, 0));
        lunch.setIsActive(true);

        LocalDateTime slot = LocalDateTime.of(2026, 10, 12, 12, 0);
        when(selfMock.getLunchBreakByDayOfWeek(slot.getDayOfWeek().getValue())).thenReturn(lunch);

        assertFalse(lunchBreakService.isLunchTime(slot));
    }

    @Test
    void isLunchTime_ReturnsFalse_WhenSlotAfterLunch() {
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(1);
        lunch.setStartTime(LocalTime.of(13, 0));
        lunch.setEndTime(LocalTime.of(14, 0));
        lunch.setIsActive(true);

        LocalDateTime slot = LocalDateTime.of(2026, 10, 12, 14, 0);
        when(selfMock.getLunchBreakByDayOfWeek(slot.getDayOfWeek().getValue())).thenReturn(lunch);

        assertFalse(lunchBreakService.isLunchTime(slot));
    }

    @Test
    void isLunchTime_ReturnsFalse_WhenLunchInactive() {
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(1);
        lunch.setStartTime(LocalTime.of(13, 0));
        lunch.setEndTime(LocalTime.of(14, 0));
        lunch.setIsActive(false);

        LocalDateTime slot = LocalDateTime.of(2026, 10, 12, 13, 0);
        when(selfMock.getLunchBreakByDayOfWeek(slot.getDayOfWeek().getValue())).thenReturn(lunch);

        assertFalse(lunchBreakService.isLunchTime(slot));
    }

    @Test
    void isLunchTime_ReturnsFalse_WhenNullPassed() {
        assertFalse(lunchBreakService.isLunchTime(null));
    }

    @Test
    void isLunchTime_ReturnsFalse_WhenNoLunchBreakFound() {
        LocalDateTime slot = LocalDateTime.of(2026, 10, 12, 13, 0);
        when(selfMock.getLunchBreakByDayOfWeek(anyInt())).thenReturn(null);

        assertFalse(lunchBreakService.isLunchTime(slot));
    }

    // ─────────────────── getLunchBreakByDayOfWeek ───────────────────

    @Test
    void getLunchBreakByDayOfWeek_ReturnsLunchBreak() {
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(3);
        when(lunchBreakRepository.findByDayOfWeek(3)).thenReturn(lunch);

        LunchBreak result = lunchBreakService.getLunchBreakByDayOfWeek(3);

        assertEquals(3, result.getDayOfWeek());
    }

    @Test
    void getLunchBreakByDayOfWeek_ReturnsNull_WhenNotFound() {
        when(lunchBreakRepository.findByDayOfWeek(7)).thenReturn(null);

        assertNull(lunchBreakService.getLunchBreakByDayOfWeek(7));
    }

    // ─────────────────── getAllLunchBreaks ───────────────────

    @Test
    void getAllLunchBreaks_ReturnsAll() {
        List<LunchBreak> breaks = List.of(new LunchBreak(), new LunchBreak());
        when(lunchBreakRepository.findAllByOrderByDayOfWeekAsc()).thenReturn(breaks);

        assertEquals(2, lunchBreakService.getAllLunchBreaks().size());
    }

    // ─────────────────── updateLunchBreak ───────────────────

    @Test
    void updateLunchBreak_WhenActive_SetsTimesAndActiveFlag() {
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(2);
        when(lunchBreakRepository.findByDayOfWeek(2)).thenReturn(lunch);

        lunchBreakService.updateLunchBreak(2, LocalTime.of(12, 0), LocalTime.of(13, 0), true);

        assertTrue(lunch.getIsActive());
        assertEquals(LocalTime.of(12, 0), lunch.getStartTime());
        assertEquals(LocalTime.of(13, 0), lunch.getEndTime());
    }

    @Test
    void updateLunchBreak_WhenInactive_ClearsTimesAndSetsInactive() {
        LunchBreak lunch = new LunchBreak();
        lunch.setDayOfWeek(3);
        lunch.setStartTime(LocalTime.of(12, 0));
        lunch.setEndTime(LocalTime.of(13, 0));
        lunch.setIsActive(true);
        when(lunchBreakRepository.findByDayOfWeek(3)).thenReturn(lunch);

        lunchBreakService.updateLunchBreak(3, null, null, false);

        assertFalse(lunch.getIsActive());
        assertNull(lunch.getStartTime());
        assertNull(lunch.getEndTime());
    }

    @Test
    void updateLunchBreak_ThrowsIllegalArgument_WhenNotFound() {
        when(lunchBreakRepository.findByDayOfWeek(5)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> lunchBreakService.updateLunchBreak(5, null, null, false));
    }
}
