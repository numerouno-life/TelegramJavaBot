package ru.bot;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.bot.handler.callback.CallbackQueryHandler;
import ru.bot.handler.TextMessageHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {
    private final TextMessageHandler textMessageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    // Пул легковесных виртуальных потоков Java 21
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void consume(Update update) {
        executor.submit(() -> {
            try {
                log.debug("Received update: {}", update);

                if (update.hasMessage() && update.getMessage().hasText()) {
                    textMessageHandler.handleTextMessage(update);
                } else if (update.hasCallbackQuery()) {
                    callbackQueryHandler.handleCallbackQuery(update.getCallbackQuery());
                } else {
                    log.info("Unknown update type: {}", update);
                }
            } catch (Exception e) {
                log.error("Error processing update asynchronously", e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}