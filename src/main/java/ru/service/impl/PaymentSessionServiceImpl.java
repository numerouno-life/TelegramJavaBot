package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.model.enums.PaymentState;
import ru.model.enums.ServiceType;
import ru.service.PaymentSessionService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static ru.util.BotConstants.PREFIX_PAYMENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSessionServiceImpl implements PaymentSessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofHours(24); // удалять через 24 часа

    private String keyState(Long chatId) {
        return PREFIX_PAYMENT + chatId + ":state";
    }

    private String keyAmount(Long chatId) {
        return PREFIX_PAYMENT + chatId + ":amount";
    }

    private String keyServiceDate(Long chatId) {
        return PREFIX_PAYMENT + chatId + ":serviceDate";
    }

    private String keyServiceType(Long chatId) {
        return PREFIX_PAYMENT + chatId + ":serviceType";
    }

    private String keyPhone(Long chatId) {
        return PREFIX_PAYMENT + chatId + ":phone";
    }

    private String keyName(Long chatId) {
        return PREFIX_PAYMENT + chatId + ":name";
    }

    @Override
    public void setPaymentState(Long chatId, PaymentState state) {
        if (chatId == null) return;
        if (state == null) {
            clearPaymentState(chatId);
            return;
        }
        redisTemplate.opsForValue().set(keyState(chatId), state.name(), TTL);
    }

    @Override
    public PaymentState getPaymentState(Long chatId) {
        if (chatId == null) return null;
        Object state = redisTemplate.opsForValue().get(keyState(chatId));
        if (state instanceof String str) {
            try {
                return PaymentState.valueOf(str);
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестное PaymentState в Redis: {}", str);
            }
        }
        return null;
    }

    @Override
    public void setAmount(Long chatId, Double amount) {
        if (chatId == null) return;
        String value = amount != null ? amount.toString() : null;
        if (value == null) {
            redisTemplate.delete(keyAmount(chatId));
        } else {
            redisTemplate.opsForValue().set(keyAmount(chatId), value, TTL);
        }
    }

    @Override
    public Double getAmount(Long chatId) {
        if (chatId == null) return null;
        Object amount = redisTemplate.opsForValue().get(keyAmount(chatId));
        if (amount instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                log.warn("Некорректное значение суммы в Redis: {}", str);
            }
        }
        return null;
    }

    @Override
    public void setServiceDate(Long chatId, LocalDateTime date) {
        if (chatId == null) return;
        String value = date != null ? date.toString() : null;
        if (value == null) {
            redisTemplate.delete(keyServiceDate(chatId));
        } else {
            redisTemplate.opsForValue().set(keyServiceDate(chatId), value, TTL);
        }
    }

    @Override
    public LocalDateTime getServiceDate(Long chatId) {
        if (chatId == null) return null;
        Object date = redisTemplate.opsForValue().get(keyServiceDate(chatId));
        if (date instanceof String str) {
            try {
                return LocalDateTime.parse(str);
            } catch (IllegalArgumentException e) {
                log.warn("Некорректная дата услуги в Redis: {}", str);
            }
        }
        return null;
    }

    @Override
    public void setServiceType(Long chatId, ServiceType type) {
        if (chatId == null) return;
        if (type == null) {
            redisTemplate.delete(keyServiceType(chatId));
        } else {
            redisTemplate.opsForValue().set(keyServiceType(chatId), type.name(), TTL);
        }
    }

    @Override
    public ServiceType getServiceType(Long chatId) {
        if (chatId == null) return null;
        Object obj = redisTemplate.opsForValue().get(keyServiceType(chatId));
        if (obj instanceof String str) {
            try {
                return ServiceType.valueOf(str);
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный ServiceType в Redis: {}", str);
            }
        }
        return null;
    }

    @Override
    public void setClientPhone(Long chatId, String phoneNumber) {
        if (chatId == null) return;
        if (phoneNumber == null) {
            redisTemplate.delete(keyPhone(chatId));
        } else {
            redisTemplate.opsForValue().set(keyPhone(chatId), phoneNumber, TTL);
        }
    }

    @Override
    public String getClientPhone(Long chatId) {
        if (chatId == null) return null;
        Object obj = redisTemplate.opsForValue().get(keyPhone(chatId));
        return obj != null ? obj.toString() : null;
    }

    @Override
    public void setClientName(Long chatId, String name) {
        if (chatId == null) return;
        if (name == null) {
            redisTemplate.delete(keyName(chatId));
        } else {
            redisTemplate.opsForValue().set(keyName(chatId), name, TTL);
        }
    }

    @Override
    public String getClientName(Long chatId) {
        if (chatId == null) return null;
        Object obj = redisTemplate.opsForValue().get(keyName(chatId));
        return obj != null ? obj.toString() : null;
    }

    // полная очистка
    @Override
    public void clearPaymentState(Long chatId) {
        redisTemplate.delete(
                List.of(
                        keyState(chatId),
                        keyAmount(chatId),
                        keyServiceDate(chatId),
                        keyServiceType(chatId),
                        keyPhone(chatId),
                        keyName(chatId)
                )
        );
    }

}
