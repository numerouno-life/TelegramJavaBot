package ru.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import ru.conf.BotProperties;

@Component
@Slf4j
@RequiredArgsConstructor
public class MyTelegramBot implements SpringLongPollingBot {
    private final UpdateConsumer updateConsumer;
    private final BotProperties botProperties;

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }

}