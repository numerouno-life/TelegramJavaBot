package ru.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import ru.error.exception.AccessDeniedException;
import ru.miniapp.auth.JwtAuthFilter;
import ru.miniapp.auth.JwtService;
import ru.miniapp.auth.TelegramInitDataValidator;
import ru.miniapp.config.MiniAppProperties;
import ru.miniapp.config.SecurityConfig;
import ru.miniapp.dto.TelegramInitDataRequest;
import ru.miniapp.error.GlobalExceptionHandler;
import ru.miniapp.support.TestInitDataGenerator;
import ru.model.User;
import ru.model.enums.UserRole;
import ru.service.UserService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtService.class,
        MiniAppProperties.class,
        TelegramInitDataValidator.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private BotProperties botProperties;

    private final String botToken = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz";

    @BeforeEach
    void setUp() {
        when(botProperties.getToken()).thenReturn(botToken);
    }

    @Test
    @DisplayName("POST /api/auth/telegram — успешный вход с валидным initData")
    void testAuthenticateSuccess() throws Exception {
        Long telegramId = 719564670L;
        String initData = TestInitDataGenerator.generateValid(botToken, telegramId, "test_user", "Иван");

        User user = User.builder()
                .telegramId(telegramId)
                .username("test_user")
                .firstName("Иван")
                .role(UserRole.USER)
                .isBlocked(false)
                .build();

        when(userService.getOrCreateUser(eq(telegramId), eq("test_user"), eq("Иван"), isNull()))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelegramInitDataRequest(initData))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.telegramId").value(telegramId))
                .andExpect(jsonPath("$.user.username").value("test_user"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/telegram — ошибка 400 при невалидной подписи hash")
    void testAuthenticateInvalidHash() throws Exception {
        Long telegramId = 719564670L;
        String initData = TestInitDataGenerator.generateWithInvalidHash(botToken, telegramId);

        mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelegramInitDataRequest(initData))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Невалидная подпись initData"));
    }

    @Test
    @DisplayName("POST /api/auth/telegram — ошибка 400 при устаревшем auth_date (> 24ч)")
    void testAuthenticateExpiredInitData() throws Exception {
        Long telegramId = 719564670L;
        String initData = TestInitDataGenerator.generateExpired(botToken, telegramId, 90_000L);

        mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelegramInitDataRequest(initData))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/telegram — ошибка 403 при заблокированном пользователе")
    void testAuthenticateBlockedUser() throws Exception {
        Long telegramId = 719564670L;
        String initData = TestInitDataGenerator.generateValid(botToken, telegramId, "blocked_user", "Block");

        User user = User.builder()
                .telegramId(telegramId)
                .username("blocked_user")
                .firstName("Block")
                .role(UserRole.USER)
                .isBlocked(true)
                .build();

        when(userService.getOrCreateUser(eq(telegramId), any(), any(), any()))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelegramInitDataRequest(initData))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Пользователь заблокирован"));
    }

    @Test
    @DisplayName("GET /api/auth/me — успешный возврат профиля с валидным Bearer JWT")
    void testGetMeSuccess() throws Exception {
        Long telegramId = 719564670L;
        String token = jwtService.generateToken(telegramId, UserRole.USER);

        User user = User.builder()
                .telegramId(telegramId)
                .username("ivan")
                .firstName("Иван")
                .role(UserRole.USER)
                .isBlocked(false)
                .build();

        when(userService.findByTelegramId(telegramId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramId").value(telegramId))
                .andExpect(jsonPath("$.username").value("ivan"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/auth/me — 401 Unauthorized без заголовка Authorization")
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Требуется авторизация (Bearer JWT)"));
    }

    @Test
    @DisplayName("GET /api/auth/me — 401 Unauthorized с невалидным / испорченным токеном")
    void testGetMeInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
