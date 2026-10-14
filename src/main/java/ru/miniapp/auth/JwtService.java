package ru.miniapp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.miniapp.config.MiniAppProperties;
import ru.model.enums.UserRole;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Сервис для выпуска и валидации JWT-токенов для Mini App.
 * Алгоритм: HS256. Payload: sub=telegramId, role=USER|ADMIN.
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long ttlMs;

    public JwtService(MiniAppProperties props) {
        String secret = props.getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET не задан или слишком короткий (требуется минимум 32 байта / 256 бит для HS256)! " +
                    "Сгенерируйте надёжный ключ, например: openssl rand -hex 32");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.ttlMs = props.getJwtTtlHours() * 3_600_000L;
        log.info("JwtService инициализирован, TTL={}ч", props.getJwtTtlHours());
    }

    /**
     * Выпускает JWT-токен для пользователя Mini App.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param role       роль пользователя (USER или ADMIN)
     * @return подписанный JWT-токен
     */
    public String generateToken(Long telegramId, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(String.valueOf(telegramId))
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Валидирует токен и возвращает claims.
     *
     * @param token JWT-токен из заголовка Authorization
     * @return claims или null, если токен невалиден/истёк
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("Невалидный JWT-токен: {}", e.getMessage());
            return null;
        }
    }

    /** Извлекает telegramId из Claims. */
    public Long extractTelegramId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /** Извлекает роль из Claims. */
    public UserRole extractRole(Claims claims) {
        String roleStr = claims.get("role", String.class);
        return UserRole.valueOf(roleStr);
    }
}
