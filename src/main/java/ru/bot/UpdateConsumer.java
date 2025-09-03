package ru.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.bot.handler.CallbackQueryHandler;
import ru.bot.handler.TextMessageHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {
    private final TextMessageHandler textMessageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    @Override
    public void consume(Update update) {
        try {
            log.info("Received update: {}", update);

            if (update.hasMessage() && update.getMessage().hasText()) {
                textMessageHandler.handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                callbackQueryHandler.handleCallbackQuery(update.getCallbackQuery());
            } else {
                log.info("Unknown update type: {}", update);
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }
}