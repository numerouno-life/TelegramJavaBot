package ru.miniapp.dto;

import jakarta.validation.constraints.NotNull;
import ru.model.enums.ServiceType;

import java.time.LocalDateTime;

/**
 * Тело запроса POST /api/appointments.
 * serviceType обязателен для записей через Mini App.
 */
public record CreateAppointmentRequest(
        @NotNull(message = "Дата и время записи обязательны")
        LocalDateTime dateTime,

        @NotNull(message = "Тип услуги обязателен")
        ServiceType serviceType
) {}
