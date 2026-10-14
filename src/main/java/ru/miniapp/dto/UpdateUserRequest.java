package ru.miniapp.dto;

/**
 * Тело запроса PATCH /api/users/me.
 * Оба поля опциональны — null-значения не обновляются.
 */
public record UpdateUserRequest(
        String firstName,
        String clientPhoneNumber
) {}
