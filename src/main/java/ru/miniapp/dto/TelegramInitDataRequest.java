package ru.miniapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Тело запроса POST /api/auth/telegram.
 * initData — строка window.Telegram.WebApp.initData.
 */
public record TelegramInitDataRequest(
        @NotBlank(message = "initData не может быть пустым")
        String initData
) {}
