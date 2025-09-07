package ru.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

    // Кнопка "Назад"
    public InlineKeyboardMarkup backButton(String text, String callbackData) {
        return new InlineKeyboardMarkup(List.of(row(text, callbackData)));
    }

    // Универсальный метод: одна кнопка
    public InlineKeyboardMarkup singleButton(String text, String callbackData) {
        return new InlineKeyboardMarkup(List.of(row(text, callbackData)));
    }

    // Универсальный метод: одна кнопка внизу под текстом
    public InlineKeyboardMarkup withBackButton(String text, String callbackText, String callbackData) {
        return new InlineKeyboardMarkup(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(callbackText)
                        .callbackData(callbackData)
                        .build())
        ));
    }

    // Пагинация для истории записей
    public InlineKeyboardMarkup historyPagination(int currentPage, int totalPages, String baseCallback) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow navRow = new InlineKeyboardRow();

        if (currentPage > 0) {
            navRow.add(createButton("⬅️ Назад", baseCallback + "_" + (currentPage - 1)));
        }
        if (currentPage < totalPages - 1) {
            navRow.add(createButton("➡️ Вперёд", baseCallback + "_" + (currentPage + 1)));
        }

        if (!navRow.isEmpty()) {
            rows.add(navRow);
        }

        // Кнопка "Назад в меню"
        rows.add(backButton("🏠 В меню", "back_to_menu").getKeyboard().get(0));

        return new InlineKeyboardMarkup(rows);
    }

    // Кнопки отмены записи
    public InlineKeyboardMarkup cancelAppointmentButton(Long appointmentId, LocalDateTime dateTime) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(createButton("❌ Отменить", "cancel_"));
        row.add(createButton("⬅️ Назад", "back_to_menu"));
        rows.add(row);
        return new InlineKeyboardMarkup(rows);
    }

    // Клавиатура выбора времени
    public InlineKeyboardMarkup timeSelectionKeyboard(LocalDate date, List<LocalDateTime> availableSlots) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (LocalDateTime slot : availableSlots) {
            if (slot.isAfter(LocalDateTime.now())) {
                currentRow.add(createButton(
                        "🟢 " + slot.toLocalTime().format(TIME_FORMAT),
                        "time_" + slot
                ));

                if (currentRow.size() == 3) {
                    rows.add(currentRow);
                    currentRow = new InlineKeyboardRow();
                }
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        // Кнопка "Назад" к датам
        rows.add(backButton("⬅️ Назад", "back_to_dates").getKeyboard().get(0));

        return new InlineKeyboardMarkup(rows);
    }

    // Клавиатура выбора даты
    public InlineKeyboardMarkup dateSelectionKeyboard(List<LocalDate> availableDates) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM (E)");

        for (LocalDate date : availableDates) {
            rows.add(row(date.format(dateFormat), "date_" + date));
        }

        // Кнопка "Назад" к меню
        rows.add(backButton("⬅️ Назад", "back_to_menu").getKeyboard().get(0));

        return new InlineKeyboardMarkup(rows);
    }

    // Универсальный метод создания кнопки
    public InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    // Универсальный метод создания строки
    public InlineKeyboardRow row(String text, String callbackData) {
        return new InlineKeyboardRow(createButton(text, callbackData));
    }

    // Универсальный метод создания строки из нескольких кнопок
    public InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.addAll(Arrays.asList(buttons));
        return row;
    }

    // Создание клавиатуры из нескольких строк
    public InlineKeyboardMarkup createKeyboard(List<InlineKeyboardRow> rows) {
        return new InlineKeyboardMarkup(rows);
    }

    // Создание клавиатуры из массива строк
    public InlineKeyboardMarkup createKeyboard(InlineKeyboardRow... rows) {
        return new InlineKeyboardMarkup(List.of(rows));
    }
}