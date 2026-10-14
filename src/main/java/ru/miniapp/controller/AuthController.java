package ru.miniapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.conf.BotProperties;
import ru.miniapp.auth.JwtService;
import ru.miniapp.auth.TelegramInitDataValidator;
import ru.miniapp.dto.AuthResponse;
import ru.miniapp.dto.DtoMapper;
import ru.miniapp.dto.TelegramInitDataRequest;
import ru.miniapp.dto.UserResponse;
import ru.model.User;
import ru.service.UserService;

import java.util.Map;

/**
 * Контроллер аутентификации Mini App через Telegram initData.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TelegramInitDataValidator initDataValidator;
    private final JwtService jwtService;
    private final UserService userService;
    private final BotProperties botProperties;
    private final ObjectMapper objectMapper;

    /**
     * POST /api/auth/telegram
     * Принимает initData из window.Telegram.WebApp.initData,
     * валидирует подпись, создаёт/находит пользователя, возвращает JWT.
     */
    @PostMapping("/telegram")
    public ResponseEntity<AuthResponse> authenticateViaTelegram(
            @Valid @RequestBody TelegramInitDataRequest request) {

        // 1. Валидируем и парсим initData
        Map<String, String> params = initDataValidator.validateAndParse(
                request.initData(), botProperties.getToken());

        // 2. Извлекаем данные пользователя из поля "user" (JSON-строка)
        String userJson = params.get("user");
        if (userJson == null) {
            throw new IllegalArgumentException("initData не содержит поле user");
        }

        Long telegramId;
        String username;
        String firstName;
        String lastName;

        try {
            JsonNode userNode = objectMapper.readTree(userJson);
            telegramId = userNode.get("id").asLong();
            username = userNode.has("username") ? userNode.get("username").asText(null) : null;
            firstName = userNode.has("first_name") ? userNode.get("first_name").asText(null) : null;
            lastName = userNode.has("last_name") ? userNode.get("last_name").asText(null) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Некорректный формат поля user в initData: " + e.getMessage());
        }

        log.info("Аутентификация через Telegram Mini App: telegramId={}, username={}", telegramId, username);

        // 3. Получаем или создаём пользователя (переиспользуем существующий сервис)
        User user = userService.getOrCreateUser(telegramId, username, firstName, lastName);

        // 4. Проверяем блокировку
        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new ru.error.exception.AccessDeniedException("Пользователь заблокирован");
        }

        // 5. Выпускаем JWT
        String token = jwtService.generateToken(user.getTelegramId(), user.getRole());

        return ResponseEntity.ok(new AuthResponse(token, DtoMapper.toUserResponse(user)));
    }

    /**
     * GET /api/auth/me
     * Возвращает актуальные данные текущего пользователя из БД (не из JWT).
     * Используется для синхронизации роли после изменений администратором.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        Long telegramId = (Long) authentication.getPrincipal();
        User user = userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Пользователь не найден: " + telegramId));
        return ResponseEntity.ok(DtoMapper.toUserResponse(user));
    }
}
