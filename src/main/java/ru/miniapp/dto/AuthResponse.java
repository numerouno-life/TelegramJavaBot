package ru.miniapp.dto;

/**
 * Ответ на POST /api/auth/telegram и GET /api/auth/me.
 */
public record AuthResponse(
        String token,
        UserResponse user
) {}
