package ru.miniapp.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Утилита для генерации Telegram WebApp initData в интеграционных тестах.
 */
public class TestInitDataGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String generateValid(String botToken, Long telegramId, String username, String firstName) {
        return generate(botToken, telegramId, username, firstName, Instant.now().getEpochSecond(), null);
    }

    public static String generateExpired(String botToken, Long telegramId, long secondsAgo) {
        return generate(botToken, telegramId, "test_user", "Test", Instant.now().getEpochSecond() - secondsAgo, null);
    }

    public static String generateWithInvalidHash(String botToken, Long telegramId) {
        return generate(botToken, telegramId, "test_user", "Test", Instant.now().getEpochSecond(), "invalid_hash_value_1234567890abcdef");
    }

    private static String generate(String botToken, Long telegramId, String username, String firstName, long authDate, String overrideHash) {
        try {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", telegramId);
            if (firstName != null) userMap.put("first_name", firstName);
            if (username != null) userMap.put("username", username);

            String userJson = OBJECT_MAPPER.writeValueAsString(userMap);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("auth_date", String.valueOf(authDate));
            params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
            params.put("user", userJson);

            // 1. dataCheckString = все параметры кроме hash, отсортированные алфавитно
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            // 2. secret = HMAC-SHA256("WebAppData", botToken)
            Mac hmacKey = Mac.getInstance("HmacSHA256");
            hmacKey.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = hmacKey.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            // 3. hash = HMAC-SHA256(secretKey, dataCheckString)
            Mac hmacData = Mac.getInstance("HmacSHA256");
            hmacData.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            String hash = overrideHash != null ? overrideHash : HexFormat.of().formatHex(hmacData.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8)));

            // 4. Формируем URL query string
            return "query_id=" + params.get("query_id") +
                    "&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8) +
                    "&auth_date=" + authDate +
                    "&hash=" + hash;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания тестового initData", e);
        }
    }
}
