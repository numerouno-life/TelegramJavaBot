package ru.miniapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.miniapp.dto.DtoMapper;
import ru.miniapp.dto.UpdateUserRequest;
import ru.miniapp.dto.UserResponse;
import ru.model.User;
import ru.service.UserService;

/**
 * REST-контроллер профиля пользователя в Mini App.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * Профиль текущего пользователя.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication auth) {
        Long telegramId = getTelegramId(auth);
        User user = userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Пользователь не найден: " + telegramId));
        return ResponseEntity.ok(DtoMapper.toUserResponse(user));
    }

    /**
     * PATCH /api/users/me
     * Обновление имени и/или телефона через Mini App.
     * Null-поля игнорируются (частичное обновление).
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @RequestBody UpdateUserRequest request,
            Authentication auth) {

        Long telegramId = getTelegramId(auth);

        // Загружаем текущие данные, подставляем новые если переданы
        User current = userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Пользователь не найден: " + telegramId));

        String newName = request.firstName() != null ? request.firstName() : current.getFirstName();
        String newPhone = request.clientPhoneNumber() != null
                ? request.clientPhoneNumber()
                : current.getClientPhoneNumber();

        User updated = userService.updateUserDetails(telegramId, newName, newPhone);
        log.info("Mini App: обновлён профиль telegramId={}, name={}, phone={}",
                telegramId, newName, newPhone);

        return ResponseEntity.ok(DtoMapper.toUserResponse(updated));
    }

    private Long getTelegramId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
