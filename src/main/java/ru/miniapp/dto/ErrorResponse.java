package ru.miniapp.dto;

import java.time.LocalDateTime;

/**
 * Единый формат ошибки для всех /api/** эндпоинтов.
 * Возвращается @RestControllerAdvice при любом исключении.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
