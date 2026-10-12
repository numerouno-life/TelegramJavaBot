package ru.conf;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class TelegramConfig {

    private final BotProperties botProperties;

    public TelegramConfig(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(30))
                .pingInterval(Duration.ofSeconds(25))
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .build();
    }

    @Bean
    public TelegramClient telegramClient(OkHttpClient okHttpClient) {
        return new OkHttpTelegramClient(okHttpClient, botProperties.getToken());
    }
}