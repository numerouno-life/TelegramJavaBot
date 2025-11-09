package ru.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.service.FloodProtectionService;
import ru.service.UserService;

import java.time.Duration;

@Slf4j
@Service
@AllArgsConstructor
public class FloodProtectionServiceImpl implements FloodProtectionService {
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 25;
    private static final int MAX_APPOINTMENT_STARTS = 5; // Лимит начала процесса записи
    public static final int APPOINTMENT_START_WINDOW_MINUTES = 2; // Окно для начала записи


    @Override
    public boolean isFloodDetected(Long userId, String messageText) {
        if (userService.isAdmin(userId)) {
            return false;
        }
        if (isCommand(messageText)) {
            return false;
        }
        String key = "flood:" + userId;
        Long count = redisTemplate.opsForValue().increment(key, 1L);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Обнаружена флудовая атака от пользователя {}: {} запрос/мин", userId, count);
            return true;
        }
        return false;
    }

    @Override
    public boolean canStartAppointmentProcess(Long userId) {
        if (userService.isAdmin(userId)) {
            return true;
        }

        String key = "appointment_start:" + userId;
        Long count = redisTemplate.opsForValue().increment(key, 1L);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(APPOINTMENT_START_WINDOW_MINUTES));
        }

        boolean allowed = count == null || count <= MAX_APPOINTMENT_STARTS;

        log.debug("Appointment start check - User: {}, Attempts: {}, Allowed: {}",
                userId, count, allowed);

        if (!allowed) {
            log.warn("📋 User {} exceeded appointment start limit: {}", userId, count);
        }
        return allowed;
    }

    private boolean isCommand(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        // Текстовые команды
        boolean isTextCommand = trimmed.startsWith("/") ||
                "start".equalsIgnoreCase(trimmed) ||
                "начать".equalsIgnoreCase(trimmed) ||
                "admin".equalsIgnoreCase(trimmed) ||
                "админ".equalsIgnoreCase(trimmed);

        // Коллбэки навигации
        boolean isNavigationCallback = trimmed.startsWith("back_") ||
                trimmed.equals("back_to_menu") ||
                trimmed.equals("back_to_dates") ||
                trimmed.startsWith("history_page_") ||
                trimmed.equals("my_appointments") ||
                trimmed.equals("contacts");

        return isTextCommand || isNavigationCallback;
    }
}
