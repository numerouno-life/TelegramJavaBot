package ru.bot.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.bot.handler.AdminCallbackHandler;
import ru.bot.handler.UserCallBackHandler;
import ru.model.enums.CallbackType;
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

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.debug("Processing callback: data='{}', type={}", data, CallbackType.fromString(data));
        try {
            CallbackType type = CallbackType.fromString(data);
            Long userId = callbackQuery.getFrom().getId();
            boolean isAdmin = userService.isAdmin(userId);

            if (isAdmin && isAdministrativeCallback(type)) {
                adminCallbackHandler.handleAdminCallback(callbackQuery);
                return;
            }

            userCallbackHandler.handleUserCallback(callbackQuery);
        } catch (Exception e) {
            log.error("Error handling callback query: {}", data, e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Произошла ошибка. Попробуйте снова.", null);
        }
    }

    private boolean isAdministrativeCallback(CallbackType type) {
        return switch (type) {
            case ADMIN_SHOW_USERS,
                 ADMIN_SHOW_APPOINTMENTS,
                 ADMIN_CREATE_APPOINTMENT,
                 ADMIN_CANCEL_APPOINTMENT,
                 ADMIN_USERS_PAGE,
                 ADMIN_BLOCK_USER,
                 ADMIN_UNBLOCK_USER,
                 ADMIN_SHOW_STATS,
                 ADMIN_BACK -> true;
            default -> false;
        };
    }
}
