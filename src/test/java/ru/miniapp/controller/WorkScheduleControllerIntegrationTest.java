package ru.miniapp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.conf.BotProperties;
import ru.miniapp.auth.JwtAuthFilter;
import ru.miniapp.auth.JwtService;
import ru.miniapp.config.MiniAppProperties;
import ru.miniapp.config.SecurityConfig;
import ru.miniapp.error.GlobalExceptionHandler;
import ru.model.LunchBreak;
import ru.model.WorkSchedule;
import ru.model.enums.UserRole;
import ru.service.LunchBreakService;
import ru.service.WorkScheduleService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkScheduleController.class)
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtService.class,
        MiniAppProperties.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class WorkScheduleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private WorkScheduleService workScheduleService;

    @MockBean
    private LunchBreakService lunchBreakService;

    @MockBean
    private BotProperties botProperties;

    private String userToken(Long telegramId) {
        return jwtService.generateToken(telegramId, UserRole.USER);
    }

    @Test
    @DisplayName("GET /api/schedule/work-days — возвращает список рабочих дней недели")
    void testGetWorkDaysSuccess() throws Exception {
        String token = userToken(123456L);

        WorkSchedule monday = WorkSchedule.builder()
                .id(1L)
                .dayOfWeek(1)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(20, 0))
                .isWorkingDay(true)
                .build();

        WorkSchedule sunday = WorkSchedule.builder()
                .id(7L)
                .dayOfWeek(7)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .isWorkingDay(false)
                .build();

        when(workScheduleService.getAllWorkSchedules()).thenReturn(List.of(monday, sunday));

        mockMvc.perform(get("/api/schedule/work-days")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$[0].dayName").value("Понедельник"))
                .andExpect(jsonPath("$[0].isWorkingDay").value(true))
                .andExpect(jsonPath("$[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$[1].dayOfWeek").value(7))
                .andExpect(jsonPath("$[1].dayName").value("Воскресенье"))
                .andExpect(jsonPath("$[1].isWorkingDay").value(false));
    }

    @Test
    @DisplayName("GET /api/schedule/work-days — 401 Unauthorized без токена")
    void testGetWorkDaysUnauthorized() throws Exception {
        mockMvc.perform(get("/api/schedule/work-days"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/schedule/lunch-breaks — возвращает список обеденных перерывов")
    void testGetLunchBreaksSuccess() throws Exception {
        String token = userToken(123456L);

        LunchBreak breakMon = LunchBreak.builder()
                .id(1L)
                .dayOfWeek(1)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 0))
                .isActive(true)
                .build();

        when(lunchBreakService.getAllLunchBreaks()).thenReturn(List.of(breakMon));

        mockMvc.perform(get("/api/schedule/lunch-breaks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$[0].startTime").value("13:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("14:00:00"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }
}
