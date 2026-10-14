package ru.miniapp.security;

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
import ru.miniapp.controller.AppointmentController;
import ru.miniapp.error.GlobalExceptionHandler;
import ru.model.enums.UserRole;
import ru.service.AppointmentService;
import ru.service.UserService;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
class SecurityAndCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private BotProperties botProperties;

    @Test
    @DisplayName("CORS: Preflight OPTIONS запрос с разрешённого Origin возвращает 200 и CORS-заголовки")
    void testCorsAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/appointments/my")
                        .header("Origin", "http://localhost:18081")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:18081"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("CORS: Preflight OPTIONS запрос с недоверенного Origin отклоняется (403 Forbidden)")
    void testCorsDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/appointments/my")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Проверка отсутствия создания сессий (Stateless) — Cookie JSESSIONID не создаётся")
    void testStatelessNoSessionCookie() throws Exception {
        String token = jwtService.generateToken(12345L, UserRole.USER);
        when(appointmentService.getUserAppointments(12345L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/appointments/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    @DisplayName("Security: Истекший токен возвращает 401 Unauthorized")
    void testExpiredTokenReturns401() throws Exception {
        // Создаем сервис с TTL = 0 (токен сразу протухает)
        MiniAppProperties expiredProps = new MiniAppProperties();
        expiredProps.setJwtSecret("test-jwt-secret-for-integration-tests-at-least-256-bits-long-32-bytes!");
        expiredProps.setJwtTtlHours(0);
        JwtService expiredJwtService = new JwtService(expiredProps);

        String expiredToken = expiredJwtService.generateToken(12345L, UserRole.USER);

        mockMvc.perform(get("/api/appointments/my")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Security: Токен, подписанный другим секретом, возвращает 401 Unauthorized")
    void testTamperedSecretTokenReturns401() throws Exception {
        MiniAppProperties rogueProps = new MiniAppProperties();
        rogueProps.setJwtSecret("completely-different-and-invalid-secret-key-at-least-32-chars-long!");
        rogueProps.setJwtTtlHours(24);
        JwtService rogueJwtService = new JwtService(rogueProps);

        String rogueToken = rogueJwtService.generateToken(12345L, UserRole.USER);

        mockMvc.perform(get("/api/appointments/my")
                        .header("Authorization", "Bearer " + rogueToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
