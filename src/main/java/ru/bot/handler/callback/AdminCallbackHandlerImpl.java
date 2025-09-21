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
import ru.model.enums.StatusAppointment;
import ru.service.*;
import ru.util.AdminKeyboard;

import java.time.LocalDateTime;
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

        log.debug("Processing admin callback: data='{}', type={}", data, CallbackType.fromString(data));


        if (!userService.isAdmin(adminId)) {
            notificationService.sendOrEditMessage(chatId, messageId, "❌ Доступ запрещён.", null);
        }

        try {
            CallbackType type = CallbackType.fromString(data);

            switch (type) {
                case ADMIN_SHOW_USERS -> showUsers(chatId, messageId, 0);
                case ADMIN_SHOW_APPOINTMENTS -> showAllActiveAppointments(chatId, messageId, 0);
                case ADMIN_CREATE_APPOINTMENT -> createAppointmentByAdmin(chatId, messageId);
                case ADMIN_CANCEL_APPOINTMENT -> {
                    Long appointmentId = Long.parseLong(data.substring("admin_cancel_".length()));
                    handleCancelAppointmentByAdmin(chatId, messageId, appointmentId);
                }
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

    private void handleCancelAppointmentByAdmin(Long chatId, Integer messageId, Long appointmentId) {
        try {
            Appointment app = appointmentService.findById(appointmentId);
            if (app.getStatus() == StatusAppointment.CANCELED) {
                notificationService.sendOrEditMessage(chatId, messageId,
                        "❌ Эта запись уже отменена.", null);
                return;
            }
            if (app.getDateTime().isBefore(LocalDateTime.now())) {
                notificationService.sendOrEditMessage(chatId, messageId,
                        "❌ Нельзя отменить прошедшую запись.", null);
                return;
            }
            appointmentService.cancelAppointment(appointmentId);

            String clientName = app.getUser().getFirstName() != null ? app.getUser().getFirstName() : "Клиент";
            String clientPhone = app.getUser().getClientPhoneNumber() != null ?
                    app.getUser().getClientPhoneNumber() : "не указан";

            notificationService.sendOrEditMessage(chatId, messageId,
                    "✅ Запись отменена:\n" +
                            "📅 " + app.getDateTime().format(DATE_FORMAT) + " " + app.getDateTime().format(TIME_FORMAT) + "\n" +
                            "👤 " + clientName + "\n" +
                            "📞 " + clientPhone,
                    adminKeyboard.getMainAdminMenu()
            );

            log.info("Администратор отменил запись id={} для клиента {}", appointmentId, clientName);

        } catch (Exception e) {
            log.error("Ошибка при отмене записи админом: id={}", appointmentId, e);

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
        List<Appointment> appointments = adminService.getAllActiveAppointments();
        if (appointments.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, messageId,
                    "Нет активных записей.", adminKeyboard.backToAdminMenu());
            return;
        }
        int totalPages = (int) Math.ceil((double) appointments.size() / PAGE_SIZE_FIVE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE_FIVE;
        int end = Math.min(start + PAGE_SIZE_FIVE, appointments.size());

        List<Appointment> subList = appointments.subList(start, end);
        for (Appointment a : subList) {
            User client = a.getUser();
            String username = client.getUsername() != null ? client.getUsername() : "нет nickname";
            String phone = client.getClientPhoneNumber();

            // Формируем текст для одной записи
            String text = "🔹 " +
                    username + " — " +
                    a.getDateTime().format(DATE_FORMAT) + " в " +
                    a.getDateTime().format(TIME_FORMAT) + " " +
                    "т." + phone + "\n";

            // Создаём клавиатуру для этой записи
            InlineKeyboardMarkup markup = adminKeyboard.adminCancelAppointmentButton(a.getId(), a.getDateTime());

            // Отправляем **отдельное сообщение** для каждой записи
            notificationService.sendOrEditMessage(chatId, null, text, markup);
        }
    }

    private void createAppointmentByAdmin(Long chatId, Integer messageId) {
        appointmentService.clearUserState(chatId);
        userSessionService.setRole(chatId, "ADMIN");
        appointmentService.setAdminState(chatId, AdminAppointmentState.AWAITING_NAME);

        notificationService.sendOrEditMessage(chatId, messageId, "👤 Введите имя клиента:", null);
    }


}
