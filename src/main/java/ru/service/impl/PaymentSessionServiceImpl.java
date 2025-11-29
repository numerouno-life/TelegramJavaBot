package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.model.enums.PaymentState;
import ru.model.enums.ServiceType;
import ru.service.PaymentSessionService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static ru.util.BotConstants.PREFIX_PAYMENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSessionServiceImpl implements PaymentSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration TTL = Duration.ofHours(24);

    private void setValue(Long chatId, String keySuffix, String value) {
        if (chatId == null) return;
        String key = PREFIX_PAYMENT + chatId + ":" + keySuffix;
        if (value == null) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForValue().set(key, value, TTL);
        }
    }

    private String getValue(Long chatId, String keySuffix) {
        if (chatId == null) return null;
        String key = PREFIX_PAYMENT + chatId + ":" + keySuffix;
        Object obj = redisTemplate.opsForValue().get(key);
        return obj != null ? obj.toString() : null;
    }

    @Override
    public void setPaymentState(Long chatId, PaymentState state) {
        setValue(chatId, "state", state != null ? state.name() : null);
    }

    @Override
    public PaymentState getPaymentState(Long chatId) {
        String value = getValue(chatId, "state");
        if (value != null) {
            try {
                return PaymentState.valueOf(value);
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестное PaymentState в Redis: {}", value);
            }
        }
        return null;
    }

    @Override
    public void setAmount(Long chatId, BigDecimal amount) {
        setValue(chatId, "amount", amount != null ? amount.toString() : null);
    }

    @Override
    public BigDecimal getAmount(Long chatId) {
        String value = getValue(chatId, "amount");
        if (value != null) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                log.warn("Некорректное значение суммы в Redis: {}", value);
            }
        }
        return null;
    }

    @Override
    public void setServiceDate(Long chatId, LocalDateTime date) {
        setValue(chatId, "serviceDate", date != null ? date.toString() : null);
    }

    @Override
    public LocalDateTime getServiceDate(Long chatId) {
        String value = getValue(chatId, "serviceDate");
        if (value != null) {
            try {
                return LocalDateTime.parse(value);
            } catch (IllegalArgumentException e) {
                log.warn("Некорректная дата услуги в Redis: {}", value);
            }
        }
        return null;
    }

    @Override
    public void setServiceType(Long chatId, ServiceType type) {
        setValue(chatId, "serviceType", type != null ? type.name() : null);
    }

    @Override
    public ServiceType getServiceType(Long chatId) {
        String value = getValue(chatId, "serviceType");
        if (value != null) {
            try {
                return ServiceType.valueOf(value);
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный ServiceType в Redis: {}", value);
            }
        }
        return null;
    }

    @Override
    public void setClientPhone(Long chatId, String phoneNumber) {
        setValue(chatId, "phone", phoneNumber);
    }

    @Override
    public String getClientPhone(Long chatId) {
        return getValue(chatId, "phone");
    }

    @Override
    public void setClientName(Long chatId, String name) {
        setValue(chatId, "name", name);
    }

    @Override
    public String getClientName(Long chatId) {
        return getValue(chatId, "name");
    }

    @Override
    public void setStatsStartDate(Long chatId, LocalDate date) {
        setValue(chatId, "statsStartDate", date != null ? date.toString() : null);
    }

    @Override
    public LocalDate getStatsStartDate(Long chatId) {
        String value = getValue(chatId, "statsStartDate");
        if (value != null) {
            try {
                return LocalDate.parse(value);
            } catch (IllegalArgumentException e) {
                log.warn("Некорректная начальная дата выбора статистики в Redis: {}", value);
            }
        }
        return null;
    }

    @Override
    public void setStatsEndDate(Long chatId, LocalDate date) {
        setValue(chatId, "statsEndDate", date != null ? date.toString() : null);
    }

    @Override
    public LocalDate getStatsEndDate(Long chatId) {
        String value = getValue(chatId, "statsEndDate");
        if (value != null) {
            try {
                return LocalDate.parse(value);
            } catch (IllegalArgumentException e) {
                log.warn("Некорректная конечная дата выбора статистики в Redis: {}", value);
            }
        }
        return null;
    }

    @Override
    public void clearPaymentState(Long chatId) {
        if (chatId == null) return;
        List<String> keys = Stream.of(
                        "state", "amount", "serviceDate", "serviceType",
                        "phone", "name", "statsStartDate", "statsEndDate"
                )
                .map(suffix -> PREFIX_PAYMENT + chatId + ":" + suffix)
                .toList();

        redisTemplate.delete(keys);
    }
}