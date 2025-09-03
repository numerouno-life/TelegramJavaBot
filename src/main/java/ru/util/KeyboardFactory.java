package ru.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup mainMenu() {
        return new InlineKeyboardMarkup(List.of(
                row("💇 Записаться", "book_appointment"),
                row("📋 Мои записи", "my_appointments"),
                row("📞 Контакты", "contacts")
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
