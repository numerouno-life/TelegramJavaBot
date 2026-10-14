package ru.miniapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.miniapp.dto.AppointmentResponse;
import ru.miniapp.dto.DtoMapper;
import ru.miniapp.dto.UserResponse;
import ru.model.User;
import ru.model.enums.UserRole;
import ru.service.AppointmentService;
import ru.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Контроллер административных функций Mini App.
 * Доступен только пользователям с ролью ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AppointmentService appointmentService;
    private final UserService userService;

    /**
     * GET /api/admin/appointments?date=YYYY-MM-DD
     * Все записи на указанную дату (все статусы), сортировка по времени.
     */
    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AppointmentResponse> result = appointmentService.getAppointmentsByDate(date)
                .stream()
                .map(DtoMapper::toAppointmentResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/users
     * Список всех пользователей.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> result = userService.getAllUsers()
                .stream()
                .map(DtoMapper::toUserResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/admin/users/{id}/role
     * Изменить роль пользователя. Body: { "role": "ADMIN" | "USER" }
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String roleStr = body.get("role");
        if (roleStr == null) {
            throw new IllegalArgumentException("Поле role обязательно");
        }
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестная роль: " + roleStr +
                    ". Допустимые значения: USER, ADMIN");
        }

        User updated = userService.updateRole(id, newRole);
        log.info("Admin API: изменена роль пользователя id={} → {}", id, newRole);
        return ResponseEntity.ok(DtoMapper.toUserResponse(updated));
    }

    /**
     * PATCH /api/admin/users/{id}/block
     * Заблокировать/разблокировать пользователя. Body: { "blocked": true | false }
     */
    @PatchMapping("/users/{id}/block")
    public ResponseEntity<UserResponse> updateUserBlock(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        Boolean blocked = body.get("blocked");
        if (blocked == null) {
            throw new IllegalArgumentException("Поле blocked обязательно");
        }

        User updated = userService.updateBlockStatus(id, blocked);
        log.info("Admin API: пользователь id={} {}",
                id, blocked ? "заблокирован" : "разблокирован");
        return ResponseEntity.ok(DtoMapper.toUserResponse(updated));
    }
}
