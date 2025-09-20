package ru.bot.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bot.handler.AdminCallbackHandler;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.AdminAppointmentState;
import ru.model.enums.CallbackType;
import ru.service.*;
import ru.util.AdminKeyboard;

import java.util.List;

import static ru.util.BotConstants.DATE_FORMAT;
import static ru.util.BotConstants.TIME_FORMAT;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminCallbackHandlerImpl implements AdminCallbackHandler {
    private final AdminService adminService;
    private final NotificationService notificationService;
    private final AdminKeyboard adminKeyboard;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final UserSessionService userSessionService;

    public static final int PAGE_SIZE_FIVE = 5;

    public void handleAdminCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        Long adminId = callbackQuery.getFrom().getId();

        if (!userService.isAdmin(adminId)) {
            notificationService.sendOrEditMessage(chatId, messageId, "❌ Доступ запрещён.", null);
        }

        try {
            CallbackType type = CallbackType.fromString(data);

            switch (type) {
                case ADMIN_SHOW_USERS -> showUsers(chatId, messageId, 0);
                case ADMIN_SHOW_APPOINTMENTS -> showAllActiveAppointments(chatId, messageId, 0);
                case ADMIN_CREATE_APPOINTMENT -> createAppointmentByAdmin(chatId, messageId);
                case ADMIN_USERS_PAGE -> {
                    int page = Integer.parseInt(data.substring("admin_users_page_".length()));
                    showUsers(chatId, messageId, page);
                }
                case ADMIN_BLOCK_USER -> {
                    Long userId = Long.parseLong(data.substring("admin_block_".length()));
                    blockUser(chatId, messageId, userId);
                }
                case ADMIN_UNBLOCK_USER -> {
                    Long userId = Long.parseLong(data.substring("admin_unblock_".length()));
                    unblockUser(chatId, messageId, userId);
                }
                case ADMIN_SHOW_STATS -> showStats(chatId, messageId);
                case ADMIN_BACK -> notificationService.sendOrEditMessage(chatId, messageId,
                        "⬅️ Возврат в админ-меню", adminKeyboard.getMainAdminMenu());
                case UNKNOWN -> log.warn("Unknown admin callback: {}", data);
                default -> log.debug("Callback not handled by admin: {}", data);
            }

        } catch (Exception e) {
            log.error("Error handling admin callback query: {}", data, e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при обработке запроса. Попробуйте снова.", null);
        }
    }


    private void unblockUser(Long chatId, Integer messageId, Long userId) {
        adminService.unblockUser(userId);
        log.info("User {} unblocked", userId);
        showUsers(chatId, messageId, 0);
    }

    private void blockUser(Long chatId, Integer messageId, Long userId) {
        adminService.blockUser(userId);
        log.info("User {} blocked", userId);
        showUsers(chatId, messageId, 0);
    }

    private void showStats(Long chatId, Integer messageId) {
        int totalUsers = adminService.getAllUsers().size();
        int blockedUsers = adminService.getBlockedUsers().size();

        String stats = "📊 Статистика:\n" +
                "• 👥 Всего пользователей: " + totalUsers + "\n" +
                "• 🚫 Заблокированных: " + blockedUsers + "\n" +
                "• ✅ Активных: " + (totalUsers - blockedUsers) + "\n";
        notificationService.sendOrEditMessage(chatId, messageId, stats, adminKeyboard.getMainAdminMenu());
    }

    private void showUsers(Long chatId, Integer messageId, int page) {
        List<User> users = adminService.getAllUsers();

        if (users.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, messageId, "Пользователей пока нет.",
                    adminKeyboard.getMainAdminMenu());
            return;
        }

        int totalPages = (int) Math.ceil((double) users.size() / PAGE_SIZE_FIVE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE_FIVE;
        int end = Math.min(start + PAGE_SIZE_FIVE, users.size());
        List<User> subList = users.subList(start, end);

        InlineKeyboardMarkup markup = adminKeyboard.getUsersListKeyboard(subList, page, totalPages);

        StringBuilder sb = new StringBuilder("👥 Список пользователей (")
                .append(page + 1).append("/").append(totalPages).append("):\n\n");

        for (User u : subList) {
            sb.append("• ").append(u.getFirstName())
                    .append(" (@").append(u.getUsername() != null ? u.getUsername() : "нет").append(")")
                    .append(u.getIsBlocked() ? " 🚫" : " ✅")
                    .append("\n");
        }

        notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);
    }

    private void showAllActiveAppointments(Long chatId, Integer messageId, int page) {
        List<Appointment> appointments = adminService.getAllAppointments();
        if (appointments.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, messageId,
                    "Нет активных записей.", adminKeyboard.getMainAdminMenu());
            return;
        }
        int totalPages = (int) Math.ceil((double) appointments.size() / PAGE_SIZE_FIVE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE_FIVE;
        int end = Math.min(start + PAGE_SIZE_FIVE, appointments.size());

        List<Appointment> subList = appointments.subList(start, end);
        StringBuilder sb = new StringBuilder("📋 Активные записи (")
                .append(page + 1).append("/").append(totalPages).append("):\n\n");
        for (Appointment a : subList) {
            User client = a.getUser();
            String username = client.getUsername() != null ? client.getUsername() : "нет nickname";
            sb.append("🔹 ")
                    .append(username).append(" — ")
                    .append(a.getDateTime().format(DATE_FORMAT))
                    .append(" в ").append(a.getDateTime().format(TIME_FORMAT)).append(" ")
                    .append("т.").append(client.getClientPhoneNumber())
                    .append("\n");
        }
        notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), adminKeyboard.getMainAdminMenu());
    }

    private void createAppointmentByAdmin(Long chatId, Integer messageId) {
        appointmentService.clearUserState(chatId);
        userSessionService.setRole(chatId, "ADMIN");
        appointmentService.setAdminState(chatId, AdminAppointmentState.AWAITING_NAME);

        notificationService.sendOrEditMessage(chatId, messageId, "👤 Введите имя клиента:", null);
    }


}
