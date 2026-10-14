package ru.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.conf.BotProperties;
import ru.error.exception.AppointmentNotFoundException;
import ru.miniapp.auth.JwtAuthFilter;
import ru.miniapp.auth.JwtService;
import ru.miniapp.config.MiniAppProperties;
import ru.miniapp.config.SecurityConfig;
import ru.miniapp.dto.CreateAppointmentRequest;
import ru.miniapp.error.GlobalExceptionHandler;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.ServiceType;
import ru.model.enums.StatusAppointment;
import ru.model.enums.UserRole;
import ru.service.AppointmentService;
import ru.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AppointmentController.class)
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtService.class,
        MiniAppProperties.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class AppointmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private BotProperties botProperties;

    private String userToken(Long telegramId) {
        return jwtService.generateToken(telegramId, UserRole.USER);
    }

    @Test
    @DisplayName("GET /api/appointments/my — возвращает список записей текущего пользователя")
    void testGetMyAppointments() throws Exception {
        Long telegramId = 111222333L;
        String token = userToken(telegramId);

        User user = User.builder().telegramId(telegramId).build();
        Appointment appointment = Appointment.builder()
                .id(10L)
                .user(user)
                .dateTime(LocalDateTime.of(2026, 10, 20, 14, 0))
                .serviceType(ServiceType.HAIRCUT)
                .status(StatusAppointment.ACTIVE)
                .build();

        when(appointmentService.getUserAppointments(telegramId)).thenReturn(List.of(appointment));

        mockMvc.perform(get("/api/appointments/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].serviceType").value("HAIRCUT"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].dateTime").value("2026-10-20T14:00:00"));
    }

    @Test
    @DisplayName("GET /api/appointments/available-dates — возвращает доступные даты")
    void testGetAvailableDates() throws Exception {
        String token = userToken(111222333L);
        LocalDate from = LocalDate.of(2026, 10, 15);

        when(appointmentService.getAvailableDates(eq(from), eq(7)))
                .thenReturn(List.of(from, from.plusDays(1), from.plusDays(2)));

        mockMvc.perform(get("/api/appointments/available-dates")
                        .param("from", "2026-10-15")
                        .param("days", "7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("2026-10-15"));
    }

    @Test
    @DisplayName("GET /api/appointments/available-dates — 400 Bad Request при отсутствии параметра from")
    void testGetAvailableDatesMissingParam() throws Exception {
        String token = userToken(111222333L);

        mockMvc.perform(get("/api/appointments/available-dates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Отсутствует обязательный параметр: from"));
    }

    @Test
    @DisplayName("GET /api/appointments/available-slots — возвращает доступные временные слоты на дату")
    void testGetAvailableSlots() throws Exception {
        String token = userToken(111222333L);
        LocalDate date = LocalDate.of(2026, 10, 15);
        LocalDateTime slot1 = date.atTime(10, 0);
        LocalDateTime slot2 = date.atTime(11, 0);

        when(appointmentService.getAvailableTimeSlots(date.atStartOfDay()))
                .thenReturn(List.of(slot1, slot2));

        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("date", "2026-10-15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("2026-10-15T10:00:00"))
                .andExpect(jsonPath("$[1]").value("2026-10-15T11:00:00"));
    }

    @Test
    @DisplayName("POST /api/appointments — успешное создание записи")
    void testCreateAppointmentSuccess() throws Exception {
        Long telegramId = 111222333L;
        String token = userToken(telegramId);
        LocalDateTime dateTime = LocalDateTime.of(2026, 10, 25, 15, 30);

        User user = User.builder().id(1L).telegramId(telegramId).build();
        when(userService.findByTelegramId(telegramId)).thenReturn(Optional.of(user));

        Appointment saved = Appointment.builder()
                .id(99L)
                .user(user)
                .dateTime(dateTime)
                .serviceType(ServiceType.BEARD)
                .status(StatusAppointment.ACTIVE)
                .build();

        when(appointmentService.createAppointment(any(Appointment.class))).thenReturn(saved);

        CreateAppointmentRequest request = new CreateAppointmentRequest(dateTime, ServiceType.BEARD);

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.serviceType").value("BEARD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/appointments — 400 Bad Request при отсутствии обязательного поля serviceType")
    void testCreateAppointmentValidationFailure() throws Exception {
        String token = userToken(111222333L);
        LocalDateTime dateTime = LocalDateTime.of(2026, 10, 25, 15, 30);

        String jsonWithoutServiceType = "{\"dateTime\":\"" + dateTime + "\"}";

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutServiceType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/appointments — 409 Conflict если слот уже занят (IllegalStateException)")
    void testCreateAppointmentConflict() throws Exception {
        Long telegramId = 111222333L;
        String token = userToken(telegramId);
        LocalDateTime dateTime = LocalDateTime.of(2026, 10, 25, 15, 30);

        User user = User.builder().id(1L).telegramId(telegramId).build();
        when(userService.findByTelegramId(telegramId)).thenReturn(Optional.of(user));

        when(appointmentService.createAppointment(any(Appointment.class)))
                .thenThrow(new IllegalStateException("Выбранное время уже занято"));

        CreateAppointmentRequest request = new CreateAppointmentRequest(dateTime, ServiceType.HAIRCUT);

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Выбранное время уже занято"));
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} — успешная отмена своей записи (204 No Content)")
    void testCancelOwnAppointment() throws Exception {
        Long telegramId = 111222333L;
        String token = userToken(telegramId);
        Long appointmentId = 55L;

        User owner = User.builder().id(1L).telegramId(telegramId).build();
        Appointment appointment = Appointment.builder().id(appointmentId).user(owner).build();

        when(appointmentService.findById(appointmentId)).thenReturn(appointment);

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancellationNoticeForAdmins(appointment);
        verify(appointmentService).cancelAppointment(appointmentId);
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} — 403 Forbidden при попытке отменить чужую запись")
    void testCancelAlienAppointmentForbidden() throws Exception {
        Long myTelegramId = 111222333L;
        Long otherTelegramId = 999888777L;
        String token = userToken(myTelegramId);
        Long appointmentId = 55L;

        User otherUser = User.builder().id(2L).telegramId(otherTelegramId).build();
        Appointment appointment = Appointment.builder().id(appointmentId).user(otherUser).build();

        when(appointmentService.findById(appointmentId)).thenReturn(appointment);

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Вы можете отменять только свои записи"));

        verify(appointmentService, never()).cancelAppointment(anyLong());
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} — 404 Not Found если запись не найдена")
    void testCancelAppointmentNotFound() throws Exception {
        String token = userToken(111222333L);
        Long appointmentId = 9999L;

        when(appointmentService.findById(appointmentId))
                .thenThrow(new AppointmentNotFoundException("Запись не найдена"));

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
