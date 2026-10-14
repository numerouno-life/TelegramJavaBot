package ru.miniapp.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Валидация Telegram WebApp initData по алгоритму из документации Telegram.
 * <p>
 * Алгоритм:
 * 1. Парсим initData как URL query string
 * 2. Извлекаем hash, убираем его из набора
 * 3. Сортируем оставшиеся пары "key=value" алфавитно и склеиваем через "\n"
 * 4. Секретный ключ = HMAC-SHA256(botToken, key="WebAppData")
 * 5. Считаем HMAC-SHA256(dataCheckString, secretKey)
 * 6. Сравниваем hex-результат с hash через MessageDigest.isEqual
 * 7. Проверяем auth_date: обязателен, не из будущего (age >= 0), не старше maxAgeSeconds (24ч)
 */
@Slf4j
@Component
public class TelegramInitDataValidator {

    private static final String HMAC_ALGO = "HmacSHA256";
    /** Максимальный возраст initData в секундах (24 часа). */
    private static final long MAX_AGE_SECONDS = 86_400L;

    /**
     * Проверяет подпись initData и возвращает распарсенные поля.
     *
     * @param initData  строка initData из window.Telegram.WebApp.initData
     * @param botToken  токен бота (из переменной окружения BOT_TOKEN)
     * @return Map с полями initData (id, username, first_name, …)
     * @throws IllegalArgumentException если подпись невалидна, auth_date отсутствует/некорректен или данные устарели
     */
    public Map<String, String> validateAndParse(String initData, String botToken) {
        if (initData == null || initData.isBlank()) {
            throw new IllegalArgumentException("initData не может быть пустым");
        }

        // 1. Парсим строку запроса в Map
        Map<String, String> params = parseQueryString(initData);

        // 2. Извлекаем hash
        String receivedHash = params.remove("hash");
        if (receivedHash == null) {
            throw new IllegalArgumentException("initData не содержит поле hash");
        }

        // 3. Строим data-check-string: отсортированные "key=value" через "\n"
        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        // 4. Секретный ключ = HMAC-SHA256(botToken, key="WebAppData")
        byte[] secretKey = hmacSha256(botToken.getBytes(StandardCharsets.UTF_8),
                "WebAppData".getBytes(StandardCharsets.UTF_8));

        // 5. Вычисляем ожидаемый hash
        byte[] expectedHashBytes = hmacSha256(dataCheckString.getBytes(StandardCharsets.UTF_8), secretKey);
        String expectedHash = bytesToHex(expectedHashBytes);

        // 6. Сравниваем — constant-time способом через MessageDigest.isEqual
        if (!timingSafeEquals(expectedHash, receivedHash)) {
            log.warn("Невалидная подпись initData. Ожидалось: {}, получено: {}",
                    expectedHash, receivedHash);
            throw new IllegalArgumentException("Невалидная подпись initData");
        }

        // 7. Проверяем auth_date (обязателен, не из будущего, не старше 24ч)
        String authDateStr = params.get("auth_date");
        if (authDateStr == null || authDateStr.isBlank()) {
            throw new IllegalArgumentException("initData не содержит auth_date");
        }
        try {
            long authDate = Long.parseLong(authDateStr);
            long age = Instant.now().getEpochSecond() - authDate;
            if (age < 0) {
                throw new IllegalArgumentException("Некорректный auth_date: дата из будущего");
            }
            if (age > MAX_AGE_SECONDS) {
                throw new IllegalArgumentException(
                        "initData устарел: возраст " + age + " сек (макс. " + MAX_AGE_SECONDS + ")");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный auth_date: " + authDateStr);
        }

        log.debug("initData успешно валидирован для user: {}", params.get("user"));
        return params;
    }

    // --- Вспомогательные методы ---

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private byte[] hmacSha256(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка вычисления HMAC-SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Сравнение строк за постоянное время (constant-time) через MessageDigest.isEqual — защита от timing-атак. */
    private boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
