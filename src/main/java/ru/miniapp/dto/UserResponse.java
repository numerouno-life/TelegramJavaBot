package ru.miniapp.dto;

/**
 * Представление пользователя в API Mini App.
 * Не содержит внутренний id БД — только telegram_id и публичные поля.
 */
public record UserResponse(
        Long telegramId,
        String username,
        String firstName,
        String lastName,
        String clientPhoneNumber,
        String role,
        boolean isBlocked
) {}
