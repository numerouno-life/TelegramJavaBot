package ru.service;

public interface FloodProtectionService {

    /**
     * Проверяет, не флудит ли пользователь
     */
    boolean isFloodDetected(Long userId, String textMessage);

    /**
     * Проверяет возможность создания записи
     */
    boolean canStartAppointmentProcess(Long userId);
}
