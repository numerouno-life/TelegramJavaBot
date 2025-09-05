package ru.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import static ru.util.BotConstants.*;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup mainMenu() {
        return new InlineKeyboardMarkup(List.of(
                row(CMD_BOOK, "book_appointment"),
                row(CMD_MY_APPOINTMENTS, "my_appointments"),
                row(CMD_HISTORY, "history"),
                row(CMD_CONTACTS, "contacts")
        ));
    }

    public InlineKeyboardMarkup backButton(String callbackData) {
        return new InlineKeyboardMarkup(List.of(
                row("⬅️ Назад", callbackData)
        ));
    }

    private InlineKeyboardRow row(String text, String callbackData) {
        return new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build());
    }
}
