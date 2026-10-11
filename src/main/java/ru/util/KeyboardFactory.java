package ru.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.model.enums.CallbackPaymentType;
import ru.model.enums.CallbackType;
import ru.model.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static ru.util.BotConstants.*;

@Component
public class KeyboardFactory {

    private static final DateTimeFormatter DATE_DAY_OF_WEEK_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM (E)", new Locale("ru"));

    public InlineKeyboardMarkup mainMenu() {
        return new InlineKeyboardMarkup(List.of(
                row(CMD_BOOK, CallbackType.BOOK_APPOINTMENT.getPrefix()),
                row(CMD_MY_APPOINTMENTS, CallbackType.MY_APPOINTMENTS.getPrefix()),
                row(CMD_HISTORY, CallbackType.HISTORY.getPrefix()),
                row(CMD_CONTACTS, CallbackType.CONTACTS.getPrefix())
        ));
    }

    // Кнопка "Назад"
    public InlineKeyboardMarkup backButton(String text, String callbackData) {
        return new InlineKeyboardMarkup(List.of(row(text, callbackData)));
    }

    public InlineKeyboardMarkup cancelStatsButton() {
        return backButton("⬅️ Назад в меню статистики", CallbackPaymentType.PAYMENT_CANCEL_STATS.getPrefix());
    }

    // Универсальный метод: одна кнопка
    public InlineKeyboardMarkup singleButton(String text, String callbackData) {
        return backButton(text, callbackData);
    }

    // Универсальный метод: одна кнопка внизу под текстом
    public InlineKeyboardMarkup withBackButton(String text, String callbackText, String callbackData) {
        return backButton(callbackText, callbackData);
    }

    public InlineKeyboardButton backToAdminMenu() {
        return createButton("⬅️ В админ-меню", CallbackType.ADMIN_BACK.getPrefix());
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
        rows.add(row("🏠 В меню", CallbackType.BACK_TO_MENU.getPrefix()));

        return new InlineKeyboardMarkup(rows);
    }

    // Кнопки отмены записи
    public InlineKeyboardMarkup userCancelAppointmentButton(Long appointmentId) {
        return userCancelAppointmentButton(appointmentId, null);
    }

    public InlineKeyboardMarkup userCancelAppointmentButton(Long appointmentId, LocalDateTime dateTime) {
        return new InlineKeyboardMarkup(List.of(
                row(
                        createButton("❌ Отменить", CallbackType.CANCEL.getPrefix() + appointmentId),
                        createButton("⬅️ Назад", CallbackType.BACK_TO_MENU.getPrefix())
                )
        ));
    }

    // Клавиатура выбора времени
    public InlineKeyboardMarkup timeSelectionKeyboard(LocalDate date, List<LocalDateTime> availableSlots,
                                                      UserRole userRole) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (availableSlots != null) {
            LocalDateTime now = LocalDateTime.now();
            List<InlineKeyboardButton> buttons = availableSlots.stream()
                    .filter(slot -> slot.isAfter(now))
                    .map(slot -> createButton(
                            "🟢 " + slot.toLocalTime().format(TIME_FORMAT),
                            CallbackType.TIME.getPrefix() + slot
                    ))
                    .toList();

            rows.addAll(createRows(buttons, 3));
        }

        // Кнопка "Назад" к датам
        if (userRole == UserRole.USER) {
            rows.add(row("⬅️ Назад", CallbackType.BACK_TO_DATES.getPrefix()));
        } else if (userRole == UserRole.ADMIN) {
            rows.add(row("⬅️ Назад", CallbackType.ADMIN_BACK.getPrefix()));
        }

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup timeSelectionKeyboardForPayment(LocalDate date, List<LocalTime> availableTimes) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (availableTimes != null) {
            List<InlineKeyboardButton> buttons = availableTimes.stream()
                    .map(time -> createButton(
                            time.format(TIME_FORMAT),
                            CallbackPaymentType.PAYMENT_SELECT_TIME.getPrefix() + date + "_" + time
                    ))
                    .toList();

            rows.addAll(createRows(buttons, 3));
        }

        rows.add(row("❌ Отменить платеж", CallbackPaymentType.PAYMENT_CANCEL.getPrefix()));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup dateSelectionKeyboardForPayment(List<LocalDate> availableDates, UserRole userRole) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (availableDates != null) {
            for (LocalDate date : availableDates) {
                String callbackData = CallbackPaymentType.PAYMENT_SELECT_DATE.getPrefix() + date;
                rows.add(row(date.format(DATE_DAY_OF_WEEK_FORMAT), callbackData));
            }
        }

        rows.add(row("❌ Отменить платеж", CallbackPaymentType.PAYMENT_CANCEL.getPrefix()));
        return new InlineKeyboardMarkup(rows);
    }

    // Клавиатура выбора даты
    public InlineKeyboardMarkup dateSelectionKeyboard(List<LocalDate> availableDates, UserRole userRole) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (availableDates != null) {
            for (LocalDate date : availableDates) {
                rows.add(row(date.format(DATE_DAY_OF_WEEK_FORMAT), CallbackType.DATE.getPrefix() + date));
            }
        }

        // Кнопка "Назад" к меню
        if (userRole == UserRole.USER) {
            rows.add(row("⬅️ Назад в меню", CallbackType.BACK_TO_MENU.getPrefix()));
        } else if (userRole == UserRole.ADMIN) {
            rows.add(row("⬅️ Назад в меню записей", CallbackType.ADMIN_MENU_APPOINTMENTS.getPrefix()));
        }

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
        InlineKeyboardRow keyboardRow = new InlineKeyboardRow();
        if (buttons != null) {
            keyboardRow.addAll(Arrays.asList(buttons));
        }
        return keyboardRow;
    }

    /**
     * Разбивает список кнопок на строки с заданным количеством колонок.
     */
    public List<InlineKeyboardRow> createRows(List<InlineKeyboardButton> buttons, int columns) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (buttons == null || buttons.isEmpty() || columns <= 0) {
            return rows;
        }

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        for (InlineKeyboardButton button : buttons) {
            currentRow.add(button);
            if (currentRow.size() == columns) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        return rows;
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