package ru.util;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.model.User;
import ru.model.WorkSchedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ru.util.BotConstants.*;

@Component
@AllArgsConstructor
public class AdminKeyboard {

    private final KeyboardFactory keyboardFactory;

    public InlineKeyboardMarkup getMainAdminMenu() {
        return new InlineKeyboardMarkup(List.of(
                keyboardFactory.row(CMD_ALL_APPOINTMENTS, "admin_appointments"),
                keyboardFactory.row(CMD_CREATE_APPOINTMENT_ADMIN, "admin_create_appointment"),
                keyboardFactory.row(CMD_ALL_USERS, "admin_show_users"),
                keyboardFactory.row(CMD_ADMIN_SCHEDULE_MENU, "admin:schedule:menu"),
                keyboardFactory.row(CMD_SHOW_STATS, "admin_stats"),
                keyboardFactory.row(CMD_ADMIN_EDIT_WORK_SCHEDULE, "admin:edit:schedule")

        ));
    }

    public InlineKeyboardMarkup getUserManagementMenu() {
        return new InlineKeyboardMarkup(List.of(
                keyboardFactory.row(CMD_BLOCKED_USER, "admin_block_"),
                keyboardFactory.row(CMD_UNBLOCKED_USER, "admin_unblock_"),
                keyboardFactory.row(CMD_ADMIN_BACK, "admin:back")
        ));
    }

    public InlineKeyboardMarkup getUsersListKeyboard(List<User> users, int page, int totalPages) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (User user : users) {
            String userInfo = String.format("👤 %s (@%s)",
                    user.getFirstName(),
                    user.getUsername() != null ? user.getUsername() : "нет username");

            InlineKeyboardButton userButton = InlineKeyboardButton.builder()
                    .text(userInfo)
                    .callbackData("admin_user_detail_" + user.getTelegramId())
                    .build();

            InlineKeyboardButton blockButton = InlineKeyboardButton.builder()
                    .text(user.getIsBlocked() ? "✅ Разблокировать" : "❌ Заблокировать")
                    .callbackData(user.getIsBlocked()
                            ? "admin_unblock_" + user.getTelegramId()
                            : "admin_block_" + user.getTelegramId())
                    .build();

            rows.add(new InlineKeyboardRow(List.of(userButton)));
            rows.add(new InlineKeyboardRow(List.of(blockButton)));
        }

        // пагинация
        if (totalPages > 1) {
            List<InlineKeyboardButton> paginationButtons = new ArrayList<>();

            if (page > 0) {
                paginationButtons.add(InlineKeyboardButton.builder()
                        .text("⬅️ Назад")
                        .callbackData("admin_users_page_" + (page - 1))
                        .build());
            }

            paginationButtons.add(InlineKeyboardButton.builder()
                    .text((page + 1) + "/" + totalPages)
                    .callbackData("admin_users_page_info")
                    .build());

            if (page < totalPages - 1) {
                paginationButtons.add(InlineKeyboardButton.builder()
                        .text("Вперёд ➡️")
                        .callbackData("admin_users_page_" + (page + 1))
                        .build());
            }

            rows.add(new InlineKeyboardRow(paginationButtons));
        }

        // возврат в меню
        rows.add(new InlineKeyboardRow(List.of(
                InlineKeyboardButton.builder()
                        .text("⬅️ В админ-меню")
                        .callbackData("admin_back")
                        .build()
        )));

        return new InlineKeyboardMarkup(rows);
    }

    // Кнопки отмены записи админом
    public InlineKeyboardMarkup adminCancelAppointmentButton(Long appointmentId, LocalDateTime dateTime) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(keyboardFactory.createButton("❌ Отменить", "admin_cancel_" + appointmentId));
        row.add(keyboardFactory.createButton("⬅️ Назад", "admin_back"));
        rows.add(row);
        return new InlineKeyboardMarkup(rows);
    }

    // Кнопка назад в админ меню
    public InlineKeyboardMarkup backToAdminMenu() {
        return new InlineKeyboardMarkup(List.of(keyboardFactory.row("⬅️ Назад в меню", "admin_back")));
    }

    public InlineKeyboardMarkup getWorkScheduleMenu(List<WorkSchedule> schedules) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (WorkSchedule s : schedules) {
            String dayName = getShortDayName(s.getDayOfWeek());
            String text = (s.getIsWorkingDay() ? "✅ " : "❌ ") + dayName;
            InlineKeyboardButton button = keyboardFactory.createButton(
                    text,
                    "admin:edit:day_" + s.getDayOfWeek()
            );
            rows.add(new InlineKeyboardRow(List.of(button)));
        }

        rows.add(new InlineKeyboardRow(List.of(
                keyboardFactory.createButton("⬅️ Назад", "admin_back")
        )));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getEditDayKeyboard(int dayOfWeek, WorkSchedule schedule) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        // Варианты времени
        String[][] timeOptions = {
                {"10:00", "18:00"},
                {"09:00", "17:00"},
                {"11:00", "20:00"},
                {"10:00", "22:00"},
                {"10:00", "20:00"}
        };
        for (String[] option : timeOptions) {
            String text = option[0] + " - " + option[1];
            String callback = "admin:save:day_" + dayOfWeek + "_" + option[0] + "_" + option[1] + "_true";
            rows.add(new InlineKeyboardRow(List.of(keyboardFactory.createButton(text, callback))));
        }
        // Кнопка "Выходной"
        String callback = "admin:save:day_" + dayOfWeek + "_null_null_false";
        rows.add(new InlineKeyboardRow(List.of(keyboardFactory.createButton("❌ Сделать выходным", callback))));

// Назад
        rows.add(new InlineKeyboardRow(List.of(
                keyboardFactory.createButton("⬅️ Назад", "admin:back:schedule")
        )));
        return new InlineKeyboardMarkup(rows);
    }

    private String getShortDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Пн";
            case 2 -> "Вт";
            case 3 -> "Ср";
            case 4 -> "Чт";
            case 5 -> "Пт";
            case 6 -> "Сб";
            case 7 -> "Вс";
            default -> "?";
        };
    }
}
