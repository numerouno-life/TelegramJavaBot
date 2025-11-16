package ru.bot.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.bot.handler.AdminCallbackHandler;
import ru.bot.handler.UserCallBackHandler;
import ru.model.enums.CallbackType;
import ru.service.FloodProtectionService;
import ru.service.NotificationService;
import ru.service.UserService;


@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    @Autowired
    private final NotificationService notificationService;
    private final AdminCallbackHandler adminCallbackHandler;
    private final UserService userService;
    private final UserCallBackHandler userCallbackHandler;
    private final FloodProtectionService floodProtectionService;

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        Long userId = callbackQuery.getFrom().getId();

        if (floodProtectionService.isFloodDetected(userId, data)) {
            log.warn("Флуд защита сработала для пользователя {} в коллбэке", userId);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Вы слишком часто отправляете запросы. Пожалуйста, подождите.", null);
            return;
        }

        log.debug("Processing callback: data='{}', type={}", data, CallbackType.fromString(data));
        try {
            CallbackType type = CallbackType.fromString(data);
            boolean isAdmin = userService.isAdmin(userId);

            if (isAdmin && isAdministrativeCallback(type)) {
                adminCallbackHandler.handleAdminCallback(callbackQuery);
                return;
            }

            userCallbackHandler.handleUserCallback(callbackQuery);
        } catch (Exception e) {
            log.error("Ошибка обработки обратного запроса: {}", data, e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Произошла ошибка. Попробуйте снова.", null);
        }
    }

    private boolean isAdministrativeCallback(CallbackType type) {
        return switch (type) {
            case ADMIN_SHOW_USERS,
                 ADMIN_SHOW_APPOINTMENTS,
                 ADMIN_ALL_TODAY_APP,
                 ADMIN_ALL_TOMORROW_APP,
                 ADMIN_CREATE_APPOINTMENT,
                 ADMIN_CANCEL_APPOINTMENT,
                 ADMIN_USERS_PAGE,
                 ADMIN_BLOCK_USER,
                 ADMIN_UNBLOCK_USER,
                 ADMIN_SHOW_STATS,
                 ADMIN_EDIT_WORK_SCHEDULE,
                 ADMIN_EDIT_DAY,
                 ADMIN_SAVE_DAY,
                 ADMIN_BACK_TO_SCHEDULE,
                 ADMIN_SCHEDULE_MENU,
                 ADMIN_MANAGE_OVERRIDES,
                 ADMIN_ADD_OVERRIDE,
                 ADMIN_DELETE_OVERRIDE,
                 ADMIN_MENU_APPOINTMENTS,
                 ADMIN_MENU_SCHEDULE,
                 ADMIN_APPOINTMENTS_PAGE,
                 ADMIN_ADD_NEW_ADMIN,
                 ADMIN_SET_NEW_ADMIN,
                 ADMIN_DELETE_ADMIN,
                 ADMIN_LUNCH_MENU,
                 ADMIN_EDIT_LUNCH,
                 ADMIN_SAVE_LUNCH,
                 ADMIN_BACK -> true;
            default -> false;
        };
    }
}
