package ru.miniapp.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.miniapp.config.MiniAppProperties;
import ru.model.enums.UserRole;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

class TelegramAuthAndJwtTest {

    private TelegramInitDataValidator validator;
    private JwtService jwtService;
    private final String botToken = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz";

    @BeforeEach
    void setUp() {
        validator = new TelegramInitDataValidator();
        MiniAppProperties properties = new MiniAppProperties();
        properties.setJwtSecret("super-secret-jwt-key-for-testing-purpose-only-32bytes!");
        properties.setJwtTtlHours(24);
        jwtService = new JwtService(properties);
    }

    private String calculateTelegramHash(String dataCheckString, String token) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] secretKey = hmacSha256.doFinal(token.getBytes(StandardCharsets.UTF_8));

        Mac dataHmac = Mac.getInstance("HmacSHA256");
        dataHmac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        byte[] hash = dataHmac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Test
    @DisplayName("TelegramInitDataValidator: Успешная валидация корректной подписи")
    void testValidInitData() throws Exception {
        long authDate = Instant.now().getEpochSecond();
        String userJson = "{\"id\":12345678,\"first_name\":\"Ivan\",\"username\":\"ivan_barber\"}";

        // В алфавитном порядке: auth_date, query_id, user
        String dataCheckString = "auth_date=" + authDate + "\nquery_id=AAHdF6IQAAAAAN0XohDhrOrc\nuser=" + userJson;
        String hash = calculateTelegramHash(dataCheckString, botToken);

        String initData = "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=" +
                java.net.URLEncoder.encode(userJson, StandardCharsets.UTF_8) +
                "&auth_date=" + authDate +
                "&hash=" + hash;

        Map<String, String> parsed = validator.validateAndParse(initData, botToken);
        assertNotNull(parsed);
        assertEquals("AAHdF6IQAAAAAN0XohDhrOrc", parsed.get("query_id"));
        assertEquals(userJson, parsed.get("user"));
    }

    @Test
    @DisplayName("TelegramInitDataValidator: Ошибка при неверном hash")
    void testInvalidHash() {
        long authDate = Instant.now().getEpochSecond();
        String initData = "auth_date=" + authDate + "&user=test&hash=invalid_hash_value";

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateAndParse(initData, botToken));
    }

    @Test
    @DisplayName("TelegramInitDataValidator: Ошибка при устаревшем auth_date (> 24ч)")
    void testExpiredAuthDate() throws Exception {
        long oldAuthDate = Instant.now().getEpochSecond() - 90_000L; // более 24 часов назад
        String dataCheckString = "auth_date=" + oldAuthDate;
        String hash = calculateTelegramHash(dataCheckString, botToken);

        String initData = "auth_date=" + oldAuthDate + "&hash=" + hash;

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateAndParse(initData, botToken));
    }

    @Test
    @DisplayName("JwtService: Выпуск и успешный парсинг токена")
    void testJwtGenerateAndParse() {
        Long telegramId = 987654321L;
        UserRole role = UserRole.ADMIN;

        String token = jwtService.generateToken(telegramId, role);
        assertNotNull(token);

        var claims = jwtService.parseToken(token);
        assertNotNull(claims);
        assertEquals(telegramId, jwtService.extractTelegramId(claims));
        assertEquals(role, jwtService.extractRole(claims));
    }

    @Test
    @DisplayName("TelegramInitDataValidator: Ошибка при отсутствии auth_date")
    void testMissingAuthDate() throws Exception {
        String dataCheckString = "user=test";
        String hash = calculateTelegramHash(dataCheckString, botToken);
        String initData = "user=test&hash=" + hash;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateAndParse(initData, botToken));
        assertTrue(ex.getMessage().contains("auth_date"));
    }

    @Test
    @DisplayName("TelegramInitDataValidator: Ошибка при auth_date из будущего")
    void testFutureAuthDate() throws Exception {
        long futureAuthDate = Instant.now().getEpochSecond() + 3600L; // +1 час в будущем
        String dataCheckString = "auth_date=" + futureAuthDate;
        String hash = calculateTelegramHash(dataCheckString, botToken);
        String initData = "auth_date=" + futureAuthDate + "&hash=" + hash;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateAndParse(initData, botToken));
        assertTrue(ex.getMessage().contains("будущего"));
    }

    @Test
    @DisplayName("JwtService: Ошибка при слишком коротком ключе (< 32 байт)")
    void testShortJwtSecretThrowsException() {
        MiniAppProperties properties = new MiniAppProperties();
        properties.setJwtSecret("too-short-secret");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new JwtService(properties));
        assertTrue(ex.getMessage().contains("32"));
        assertTrue(ex.getMessage().contains("openssl rand"));
    }

    @Test
    @DisplayName("JwtService: Невалидный токен возвращает null")
    void testInvalidJwtReturnsNull() {
        assertNull(jwtService.parseToken("invalid.jwt.token"));
    }
}
