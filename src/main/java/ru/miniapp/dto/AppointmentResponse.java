package ru.miniapp.dto;

import java.time.LocalDateTime;

/**
 * Представление записи (Appointment) в API Mini App.
 * serviceType — nullable, т.к. записи, созданные через бот, не имеют типа услуги.
 * На фронте: если null — показывать "Услуга не указана".
 */
public record AppointmentResponse(
        Long id,
        LocalDateTime dateTime,
        String status,
        String serviceType,       // nullable — бот-флоу не передаёт тип услуги
        String clientName,
        String clientPhone,
        Long clientTelegramId
) {}
