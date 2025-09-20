package ru.util;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.model.User;

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
                keyboardFactory.row(CMD_WORKING_HOURS, "admin:schedule:menu"),
                keyboardFactory.row(CMD_SHOW_STATS, "admin_stats")
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
}
