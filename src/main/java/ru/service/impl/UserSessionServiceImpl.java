package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.model.enums.AdminAppointmentState;
import ru.service.UserSessionService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static ru.util.BotConstants.PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionServiceImpl implements UserSessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofHours(24); // удалять через 24 часа

    // Ключи
    private String keyState(Long chatId) { return PREFIX + chatId + ":state"; }
    private String keyDate(Long chatId) { return PREFIX + chatId + ":date"; }
    private String keyName(Long chatId) { return PREFIX + chatId + ":name"; }
    private String keyMessageId(Long chatId) { return PREFIX + chatId + ":messageId"; }
    private String keyHistoryPage(Long chatId) { return PREFIX + chatId + ":historyPage"; }
    private String keyRole(Long chatId) { return PREFIX + chatId + ":role"; }


    // Установить состояние
    @Override
    public void setUserState(Long chatId, String state) {
        if (chatId == null) return;
        if (state == null) {
            clearUserState(chatId);
            return;
        }
        redisTemplate.opsForValue().set(keyState(chatId), state, TTL);
    }

    @Override
    public String getUserState(Long chatId) {
        if (chatId == null) return null;
        Object state = redisTemplate.opsForValue().get(keyState(chatId));
        return state != null ? state.toString() : null;
    }

    // Дата
    @Override
    public void setPendingDate(Long chatId, LocalDateTime dateTime) {
        log.info("💾 Сохраняем pendingDate для {}: {}", chatId, dateTime);
        redisTemplate.opsForValue().set(keyDate(chatId), dateTime, TTL);
    }

    @Override
    public LocalDateTime getPendingDate(Long chatId) {
        Object date = redisTemplate.opsForValue().get(keyDate(chatId));
        log.info("🔍 Читаем pendingDate для {}: raw = {}, type = {}",
                chatId, date, date != null ? date.getClass() : "null");

        if (date instanceof String str) {
            try {
                return LocalDateTime.parse(str);
            } catch (Exception e) {
                log.warn("❌ Не удалось распарсить LocalDateTime из строки: {}", str);
                return null;
            }
        }
        return null;
    }

    // ID сообщения
    @Override
    public void setPendingMessageId(Long chatId, Integer messageId) {
        redisTemplate.opsForValue().set(keyMessageId(chatId), messageId, TTL);
    }

    @Override
    public Integer getPendingMessageId(Long chatId) {
        Object id = redisTemplate.opsForValue().get(keyMessageId(chatId));
        return id instanceof Integer ? (Integer) id : null;
    }

    @Override
    public void clearPendingMessageId(Long chatId) {
        redisTemplate.delete(keyMessageId(chatId));
    }

    // Страница истории
    @Override
    public void setHistoryPage(Long chatId, Integer page) {
        redisTemplate.opsForValue().set(keyHistoryPage(chatId), page, TTL);
    }

    @Override
    public Integer getHistoryPage(Long chatId) {
        Object page = redisTemplate.opsForValue().get(keyHistoryPage(chatId));
        return page instanceof Integer ? (Integer) page : null;
    }

    @Override
    public void clearHistoryPage(Long chatId) {
        redisTemplate.delete(keyHistoryPage(chatId));
    }

    // Полная очистка
    @Override
    public void clearUserState(Long chatId) {
        redisTemplate.delete(
                List.of(
                        keyState(chatId),
                        keyDate(chatId),
                        keyName(chatId),
                        keyMessageId(chatId),
                        keyHistoryPage(chatId)
                )
        );
    }

    @Override
    public void setAdminState(Long chatId, AdminAppointmentState state) {
        if (chatId == null) return;
        if (state == null) {
            clearAdminState(chatId);
            return;
        }
        redisTemplate.opsForValue().set(keyAdminState(chatId), state.name(), TTL);
    }

    @Override
    public AdminAppointmentState getAdminState(Long chatId) {
        if (chatId == null) return null;
        Object value = redisTemplate.opsForValue().get(keyAdminState(chatId));
        if (value != null) {
            try {
                return AdminAppointmentState.valueOf(value.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Не удалось распарсить AdminAppointmentState: {}", value);
            }
        }
        return null;
    }

    @Override
    public void clearAdminState(Long chatId) {
        redisTemplate.delete(keyAdminState(chatId));
    }


    @Override
    public void setRole(Long chatId, String role) {
        if (chatId == null) return;
        redisTemplate.opsForValue().set(keyRole(chatId), role, TTL);
    }

    @Override
    public String getRole(Long chatId) {
        if (chatId == null) return "USER";
        Object role = redisTemplate.opsForValue().get(keyRole(chatId));
        return role != null ? role.toString() : "USER";
    }

    @Override
    public void clearRole(Long chatId) {
        redisTemplate.delete(keyRole(chatId));
    }

    @Override
    public void setPendingName(Long chatId, String name) {
        redisTemplate.opsForValue().set(keyName(chatId), name, TTL);
    }

    @Override
    public String getPendingName(Long chatId) {
        Object name = redisTemplate.opsForValue().get(keyName(chatId));
        return name != null ? name.toString() : null;
    }

    @Override
    public void clearPendingName(Long chatId) {
        redisTemplate.delete(keyName(chatId));
    }

    @Override
    public void clearPendingDate(Long chatId) {
        redisTemplate.delete(keyDate(chatId));
    }

    private String keyAdminState(Long chatId) {
        return PREFIX + chatId + ":adminState";
    }

}
