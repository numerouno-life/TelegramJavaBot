package ru.miniapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Настройка кнопки меню бота при старте (только профиль docker).
 * Вызывает setChatMenuButton с типом web_app и URL из MINIAPP_URL.
 * Если MINIAPP_URL не задан — пропускает.
 */
@Slf4j
@Configuration
@Profile("docker")
@RequiredArgsConstructor
public class MenuButtonConfig {

    private final TelegramClient telegramClient;
    private final MiniAppProperties miniAppProperties;

    @Bean
    public ApplicationRunner setMenuButton() {
        return args -> {
            String miniAppUrl = miniAppProperties.getUrl();
            if (miniAppUrl == null || miniAppUrl.isBlank()) {
                log.info("MINIAPP_URL не задан — кнопка меню бота не настраивается");
                return;
            }

            try {
                MenuButtonWebApp menuButton = new MenuButtonWebApp("🌐 Открыть приложение", new WebAppInfo(miniAppUrl));

                SetChatMenuButton setChatMenuButton = SetChatMenuButton.builder()
                        .menuButton(menuButton)
                        .build();

                telegramClient.execute(setChatMenuButton);
                log.info("Кнопка меню бота настроена: {}", miniAppUrl);
            } catch (Exception e) {
                log.warn("Не удалось настроить кнопку меню бота: {}", e.getMessage());
            }
        };
    }
}
