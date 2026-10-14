package ru.miniapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Свойства Mini App — читаются из переменных окружения.
 * JWT_SECRET, MINI_APP_URL, MINI_APP_ORIGIN прописываются в .env и docker-compose.
 */
@Configuration
@ConfigurationProperties(prefix = "miniapp")
@Getter
@Setter
public class MiniAppProperties {

    /** Секрет для подписи JWT (минимум 32 символа). Env: JWT_SECRET */
    private String jwtSecret = "default-fallback-secret-for-dev-environment-at-least-256-bits-long!";

    /** TTL токена в часах. По умолчанию 24. */
    private long jwtTtlHours = 24;

    /** URL фронтенда Mini App (для setChatMenuButton и CORS). Env: MINIAPP_URL */
    private String url;

    /** Origin фронтенда для CORS (обычно = url). Env: MINIAPP_ORIGIN */
    private String origin;
}
