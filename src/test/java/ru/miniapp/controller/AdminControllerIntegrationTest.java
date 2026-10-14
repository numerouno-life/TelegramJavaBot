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
import ru.miniapp.auth.JwtAuthFilter;
import ru.miniapp.auth.JwtService;
import ru.miniapp.config.MiniAppProperties;
import ru.miniapp.config.SecurityConfig;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtService.class,
        MiniAppProperties.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

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

    private String adminToken(Long telegramId) {
        return jwtService.generateToken(telegramId, UserRole.ADMIN);
    }

    private String userToken(Long telegramId) {
        return jwtService.generateToken(telegramId, UserRole.USER);
    }

    @Test
    @DisplayName("GET /api/admin/appointments — 403 Forbidden для пользователя с ролью USER")
    void testGetAppointmentsForbiddenForUser() throws Exception {
        String token = userToken(111L);

        mockMvc.perform(get("/api/admin/appointments")
                        .param("date", "2026-10-15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/admin/appointments — 401 Unauthorized без авторизации")
    void testGetAppointmentsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/appointments")
                        .param("date", "2026-10-15"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/admin/appointments — 200 OK для роли ADMIN")
    void testGetAppointmentsAdminSuccess() throws Exception {
        String token = adminToken(999L);
        LocalDate date = LocalDate.of(2026, 10, 15);

        User client = User.builder().id(1L).telegramId(12345L).firstName("Клиент").build();
        Appointment app = Appointment.builder()
                .id(1L)
                .user(client)
                .dateTime(date.atTime(12, 0))
                .serviceType(ServiceType.HAIRCUT)
                .status(StatusAppointment.ACTIVE)
                .build();

        when(appointmentService.getAppointmentsByDate(eq(date))).thenReturn(List.of(app));

        mockMvc.perform(get("/api/admin/appointments")
                        .param("date", "2026-10-15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].serviceType").value("HAIRCUT"))
                .andExpect(jsonPath("$[0].clientName").value("Клиент"))
                .andExpect(jsonPath("$[0].clientTelegramId").value(12345));
    }

    @Test
    @DisplayName("GET /api/admin/users — 200 OK возвращает список всех пользователей для ADMIN")
    void testGetAllUsersAdminSuccess() throws Exception {
        String token = adminToken(999L);

        User user1 = User.builder().id(1L).telegramId(101L).username("u1").role(UserRole.USER).build();
        User user2 = User.builder().id(2L).telegramId(102L).username("u2").role(UserRole.ADMIN).build();

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].telegramId").value(101))
                .andExpect(jsonPath("$[1].telegramId").value(102));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role — ADMIN успешно изменяет роль пользователя")
    void testUpdateUserRoleSuccess() throws Exception {
        String token = adminToken(999L);
        Long targetUserId = 42L;

        User updated = User.builder()
                .id(targetUserId)
                .telegramId(555L)
                .role(UserRole.ADMIN)
                .build();

        when(userService.updateRole(eq(targetUserId), eq(UserRole.ADMIN))).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/users/{id}/role", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(userService).updateRole(targetUserId, UserRole.ADMIN);
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role — 400 Bad Request при невалидном имени роли")
    void testUpdateUserRoleInvalidRole() throws Exception {
        String token = adminToken(999L);
        Long targetUserId = 42L;

        mockMvc.perform(patch("/api/admin/users/{id}/role", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SUPER_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Неизвестная роль: SUPER_ADMIN. Допустимые значения: USER, ADMIN"));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/block — ADMIN успешно блокирует пользователя")
    void testUpdateUserBlockSuccess() throws Exception {
        String token = adminToken(999L);
        Long targetUserId = 42L;

        User updated = User.builder()
                .id(targetUserId)
                .telegramId(555L)
                .isBlocked(true)
                .build();

        when(userService.updateBlockStatus(eq(targetUserId), eq(true))).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/users/{id}/block", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("blocked", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));

        verify(userService).updateBlockStatus(targetUserId, true);
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/block — 400 Bad Request при отсутствии поля blocked")
    void testUpdateUserBlockMissingField() throws Exception {
        String token = adminToken(999L);
        Long targetUserId = 42L;

        mockMvc.perform(patch("/api/admin/users/{id}/block", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Поле blocked обязательно"));
    }
}
