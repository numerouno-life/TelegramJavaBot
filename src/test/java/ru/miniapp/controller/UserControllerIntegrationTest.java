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
import ru.miniapp.dto.UpdateUserRequest;
import ru.miniapp.error.GlobalExceptionHandler;
import ru.model.User;
import ru.model.enums.UserRole;
import ru.service.UserService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtService.class,
        MiniAppProperties.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class UserControllerIntegrationTest {

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

    private String userToken(Long telegramId) {
        return jwtService.generateToken(telegramId, UserRole.USER);
    }

    @Test
    @DisplayName("GET /api/users/me — возвращает данные профиля текущего пользователя")
    void testGetMeSuccess() throws Exception {
        Long telegramId = 555666777L;
        String token = userToken(telegramId);

        User user = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .username("john_doe")
                .firstName("John")
                .lastName("Doe")
                .clientPhoneNumber("+79991234567")
                .role(UserRole.USER)
                .isBlocked(false)
                .build();

        when(userService.findByTelegramId(telegramId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramId").value(telegramId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.clientPhoneNumber").value("+79991234567"));
    }

    @Test
    @DisplayName("GET /api/users/me — 401 Unauthorized без авторизации")
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("PATCH /api/users/me — успешное обновление имени и телефона")
    void testUpdateMeSuccess() throws Exception {
        Long telegramId = 555666777L;
        String token = userToken(telegramId);

        User current = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .firstName("OldName")
                .clientPhoneNumber("+70000000000")
                .role(UserRole.USER)
                .build();

        User updated = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .firstName("NewName")
                .clientPhoneNumber("+79998887766")
                .role(UserRole.USER)
                .build();

        when(userService.findByTelegramId(telegramId)).thenReturn(Optional.of(current));
        when(userService.updateUserDetails(eq(telegramId), eq("NewName"), eq("+79998887766")))
                .thenReturn(updated);

        UpdateUserRequest request = new UpdateUserRequest("NewName", "+79998887766");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewName"))
                .andExpect(jsonPath("$.clientPhoneNumber").value("+79998887766"));

        verify(userService).updateUserDetails(telegramId, "NewName", "+79998887766");
    }

    @Test
    @DisplayName("PATCH /api/users/me — частичное обновление (только телефон)")
    void testPartialUpdatePhoneOnly() throws Exception {
        Long telegramId = 555666777L;
        String token = userToken(telegramId);

        User current = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .firstName("ExistingName")
                .clientPhoneNumber("+70000000000")
                .role(UserRole.USER)
                .build();

        User updated = User.builder()
                .id(1L)
                .telegramId(telegramId)
                .firstName("ExistingName")
                .clientPhoneNumber("+79995554433")
                .role(UserRole.USER)
                .build();

        when(userService.findByTelegramId(telegramId)).thenReturn(Optional.of(current));
        when(userService.updateUserDetails(eq(telegramId), eq("ExistingName"), eq("+79995554433")))
                .thenReturn(updated);

        UpdateUserRequest request = new UpdateUserRequest(null, "+79995554433");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("ExistingName"))
                .andExpect(jsonPath("$.clientPhoneNumber").value("+79995554433"));

        verify(userService).updateUserDetails(telegramId, "ExistingName", "+79995554433");
    }
}
