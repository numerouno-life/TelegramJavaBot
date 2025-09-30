package ru.bot.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.bot.handler.AdminCallbackHandler;
import ru.model.Appointment;
import ru.model.User;
import ru.model.WorkDaysOverride;
import ru.model.WorkSchedule;
import ru.model.enums.*;
import ru.repository.WorkDaysOverrideRepository;
import ru.repository.WorkScheduleRepository;
import ru.service.*;
import ru.util.AdminKeyboard;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private final WorkScheduleService workScheduleService;
    private final WorkScheduleRepository workScheduleRepository;
    private final WorkDaysOverrideRepository workDaysOverrideRepository;
    private final KeyboardFactory keyboardFactory;

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
                case ADMIN_ALL_TODAY_APP -> showAllAppointmentsToday(chatId, messageId);
                case ADMIN_ALL_TOMORROW_APP -> showAllAppointmentsTomorrow(chatId, messageId);
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
                case ADMIN_EDIT_WORK_SCHEDULE, ADMIN_BACK_TO_SCHEDULE -> showWorkScheduleEditMenu(chatId, messageId);
                case ADMIN_EDIT_DAY -> {
                    String dayStr = data.substring("admin:edit:day_".length());
                    int dayOfWeek = Integer.parseInt(dayStr);
                    showEditDayForm(chatId, messageId, dayOfWeek);
                }
                case ADMIN_SAVE_DAY -> {
                    String prefix = "admin:save:day_";
                    String payload = data.substring(prefix.length());
                    String[] parts = payload.split("_");

                    int dayOfWeek = Integer.parseInt(parts[0]);
                    LocalTime startTime = parts[1].equals("null") ? null : LocalTime.parse(parts[1]);
                    LocalTime endTime = parts[2].equals("null") ? null : LocalTime.parse(parts[2]);
                    boolean isWorking = Boolean.parseBoolean(parts[3]);

                    saveWorkDay(chatId, messageId, dayOfWeek, startTime, endTime, isWorking);
                }
                case ADMIN_SCHEDULE_MENU -> showWorkScheduleMenu(chatId, messageId);
                case ADMIN_MANAGE_OVERRIDES -> showOverridesMenu(chatId, messageId);
                case ADMIN_ADD_OVERRIDE -> showAddOverrideForm(chatId, messageId);
                case ADMIN_DELETE_OVERRIDE -> {
                    LocalDate date = LocalDate.parse(data.substring("admin:override:delete_".length()));
                    deleteOverride(chatId, messageId, date);
                }
                case ADMIN_MENU_APPOINTMENTS -> notificationService.sendOrEditMessage(chatId, messageId,
                        "📋 *Управление записями*", adminKeyboard.getAppointmentsSubMenu());

                case ADMIN_MENU_SCHEDULE -> notificationService.sendOrEditMessage(chatId, messageId,
                        "🗓 *Управление расписанием*", adminKeyboard.getScheduleSubMenu());
                case ADMIN_APPOINTMENTS_PAGE -> {
                    int page = Integer.parseInt(data.substring("admin:appointments:page_".length()));
                    showAllActiveAppointments(chatId, messageId, page);
                }
                case UNKNOWN -> log.warn("Unknown admin callback: {}", data);
                default -> log.debug("Callback not handled by admin: {}", data);
            }

        } catch (Exception e) {
            log.error("Error handling admin callback query: {}", data, e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при обработке запроса. Попробуйте снова.", null);
        }
    }

    private void deleteOverride(Long chatId, Integer messageId, LocalDate date) {
        workScheduleService.deleteOverrideByDate(date);
        notificationService.sendOrEditMessage(chatId, messageId,
                "✅ Исключение удалено: " + date.format(DATE_FORMAT),
                null);
        showOverridesMenu(chatId, messageId);
    }

    private void showAddOverrideForm(Long chatId, Integer messageId) {
        String text = """
                📝 *Добавить исключение*
                
                Отправьте дату в формате: `ГГГГ-ММ-ДД`
                Например: `2025-12-31`
                
                Или нажмите "Отмена".
                """;
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(
                adminKeyboard.backToAdminMenu()
        ));
        notificationService.sendOrEditMessage(chatId, messageId, text, markup);
        userSessionService.setRole(chatId, "ADMIN");
        userSessionService.setAdminState(chatId, AdminAppointmentState.AWAITING_OVERRIDE_DATE);
    }

    private void showOverridesMenu(Long chatId, Integer messageId) {
        List<WorkDaysOverride> workDaysOverrides = workDaysOverrideRepository.findAllByOrderByDateDesc();
        InlineKeyboardMarkup markup = adminKeyboard.getOverridesMenu(workDaysOverrides);
        StringBuilder sb = new StringBuilder("📆 *Исключения в расписании*\n\n");
        if (workDaysOverrides.isEmpty()) {
            sb.append("❌ Нет исключений.\n\n");
        } else {
            for (WorkDaysOverride o : workDaysOverrides) {
                sb.append("📅 ").append(o.getDate().format(DATE_FORMAT))
                        .append(" — ").append(o.getIsWorkingDay() ? "✅ рабочий" : "❌ выходной");
                if (o.getReason() != null) {
                    sb.append(" (").append(o.getReason()).append(")");
                }
                if (o.getIsWorkingDay() && o.getStartTime() != null) {
                    sb.append(" ").append(o.getStartTime()).append("-").append(o.getEndTime());
                }
                sb.append("\n");
            }
        }
        notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);

    }

    private void saveWorkDay(Long chatId, Integer messageId, int dayOfWeek,
                             LocalTime startTime, LocalTime endTime, boolean isWorking) {
        try {
            workScheduleService.updateWorkDay(dayOfWeek, startTime, endTime, isWorking);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "✅ Расписание обновлено!", null);
            showWorkScheduleEditMenu(chatId, messageId);
        } catch (Exception e) {
            log.error("Ошибка при сохранении расписания: {}", e.getMessage(), e);
            notificationService.sendOrEditMessage(chatId, messageId,
                    "❌ Ошибка при сохранении", null);
        }
    }

    private void showEditDayForm(Long chatId, Integer messageId, int dayOfWeek) {
        WorkSchedule schedule = workScheduleRepository.findByDayOfWeek(dayOfWeek);
        if (schedule == null) {
            notificationService.sendOrEditMessage(chatId, messageId, "День не найден.", null);
            return;
        }
        String dayName = getDayName(dayOfWeek);
        InlineKeyboardMarkup markup = adminKeyboard.getEditDayKeyboard(dayOfWeek, schedule);

        String text = "✏️ Редактирование дня: " + dayName + "\n" +
                "Текущее время: " + (schedule.getIsWorkingDay()
                ? schedule.getStartTime() + " - " + schedule.getEndTime()
                : "выходной") + "\n\n" +
                "Выберите новое время или статус:";

        notificationService.sendOrEditMessage(chatId, messageId, text, markup);
    }

    private void showWorkScheduleEditMenu(Long chatId, Integer messageId) {
        List<WorkSchedule> schedules = workScheduleService.getAllWorkSchedules();
        InlineKeyboardMarkup markup = adminKeyboard.getWorkScheduleMenu(schedules);
        StringBuilder sb = new StringBuilder("📅 *Базовое расписание работы*\n\n");
        for (WorkSchedule s : schedules) {
            String dayName = getDayName(s.getDayOfWeek());
            if (s.getIsWorkingDay()) {
                sb.append("✅ ").append(dayName)
                        .append(": ").append(s.getStartTime()).append(" - ").append(s.getEndTime())
                        .append("\n");
            } else {
                sb.append("❌ ").append(dayName).append(" — выходной\n");
            }
        }
        notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);
    }

    private void showWorkScheduleMenu(Long chatId, Integer messageId) {
        List<WorkSchedule> schedules = workScheduleService.getAllWorkSchedules();
        StringBuilder sb = new StringBuilder("📅 *Расписание работы*\n\n");

        for (WorkSchedule s : schedules) {
            String dayName = getDayName(s.getDayOfWeek());
            if (s.getIsWorkingDay()) {
                sb.append("✅ ").append(dayName)
                        .append(": ").append(s.getStartTime()).append(" - ").append(s.getEndTime())
                        .append("\n");
            } else {
                sb.append("❌ ").append(dayName).append(" — выходной\n");
            }
        }

        // только кнопка "Назад"
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(adminKeyboard.backToScheduleMenu()));

        notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);
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
            notificationService.sendMessage(app.getUser().getTelegramId(),
                    "❌ Ваша запись отменена.\n" +
                            "Администратор отменил запись на " + app.getDateTime().format(DATE_FORMAT) +
                            " " + app.getDateTime().format(TIME_FORMAT) + "\n" +
                            "Если вы не согласны с этим, свяжитесь с администратором.");
            log.info("Пользователь {} получил уведомление о отмене записи", app.getUser().getTelegramId());

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
        int totalUsersUnique = adminService.getAllUsers().stream()
                .filter(user -> user.getTelegramId() != null)
                .toList().size();
        int totalAppointments = adminService.getAllAppointments().size();
        int totalActiveAppointments = adminService.getAllActiveAppointments().size();

        String stats = "📊 Статистика:\n" +
                "• 👥 Всего пользователей: " + totalUsers + "\n" +
                "• 👥 Уникальных пользователей: " + totalUsersUnique + "\n" +
                "• 🚫 Заблокированных: " + blockedUsers + "\n" +
                "• ✅ Активных записей: " + totalActiveAppointments + "\n" +
                "• 📆 Записей за всё время: " + totalAppointments;
        notificationService.sendOrEditMessage(chatId, messageId, stats, adminKeyboard.getMainAdminMenu());
    }

    private void showUsers(Long chatId, Integer messageId, int page) {
        List<User> users = adminService.getAllUsers();

        if (users.isEmpty()) {
            notificationService.sendOrEditMessage(chatId, messageId, "Пользователей пока нет.",
                    adminKeyboard.getMainAdminMenu());
            return;
        }
        List<User> sortedUsers = users.stream()
                .sorted(Comparator.comparing(
                        user -> {
                            String name = user.getFirstName();
                            if (name == null || name.trim().isEmpty()) {
                                return user.getUsername() != null ? user.getUsername() : "";
                            }
                            return name;
                        },
                        String.CASE_INSENSITIVE_ORDER
                ))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) users.size() / PAGE_SIZE_FIVE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE_FIVE;
        int end = Math.min(start + PAGE_SIZE_FIVE, users.size());
        List<User> subList = sortedUsers.subList(start, end);

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
        showAppointments(chatId, messageId, AppointmentPeriod.ALL, appointments, page);
    }

    private void showAllAppointmentsToday(Long chatId, Integer messageId) {
        List<Appointment> todayAppointments = adminService.getAppointmentsToday();
        showAppointments(chatId, messageId, AppointmentPeriod.TODAY, todayAppointments, 0);
    }

    private void showAllAppointmentsTomorrow(Long chatId, Integer messageId) {
        List<Appointment> tomorrowAppointments = adminService.getAppointmentsTomorrow();
        showAppointments(chatId, messageId, AppointmentPeriod.TOMORROW, tomorrowAppointments, 0);
    }


    private void createAppointmentByAdmin(Long chatId, Integer messageId) {
        notificationService.deleteMessage(chatId, messageId);
        appointmentService.clearUserState(chatId);
        userSessionService.setRole(chatId, "ADMIN");
        appointmentService.setAdminState(chatId, AdminAppointmentState.ADM_AWAITING_DATE);
        sendDateSelectionForAdmin(chatId);
    }

    private String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Понедельник";
            case 2 -> "Вторник";
            case 3 -> "Среда";
            case 4 -> "Четверг";
            case 5 -> "Пятница";
            case 6 -> "Суббота";
            case 7 -> "Воскресенье";
            default -> "Неизвестный день";
        };

    }

    private void sendDateSelectionForAdmin(Long chatId) {
        LocalDate today = LocalDate.now();
        List<LocalDate> availableDates = new ArrayList<>();
        for (int i = 0; i < 14; i++) { // даём больше дней админу
            LocalDate date = today.plusDays(i);
            if (appointmentService.isWorkingDay(date)) {
                List<LocalDateTime> slots = appointmentService.getAvailableTimeSlots(date.atStartOfDay());
                if (!slots.isEmpty()) {
                    availableDates.add(date);
                }
            }
        }

        InlineKeyboardMarkup markup = keyboardFactory.dateSelectionKeyboard(availableDates, UserRole.ADMIN);
        notificationService.sendOrEditMessage(chatId, null, "📅 Выберите дату для клиента:", markup);
    }

    private void showAppointments(Long chatId, Integer messageId,
                                  AppointmentPeriod period, List<Appointment> appointments, int page) {
        if (appointments.isEmpty()) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(
                    adminKeyboard.backToAppointmentsMenu()
            ));
            notificationService.sendOrEditMessage(chatId, messageId, period.getEmptyMessage(), markup);
            return;
        }

        List<Appointment> sortedAppointments = appointments.stream()
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .toList();

        List<InlineKeyboardRow> rows = new ArrayList<>();
        int totalPages;
        int start;
        int end;

        if (period == AppointmentPeriod.ALL) {
            totalPages = (int) Math.ceil((double) sortedAppointments.size() / PAGE_SIZE_FIVE);
            int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
            start = clampedPage * PAGE_SIZE_FIVE;
            end = Math.min(start + PAGE_SIZE_FIVE, sortedAppointments.size());

            String title = period.getTitle() + " (стр. " + (clampedPage + 1) + "/" + totalPages + ")";
            StringBuilder sb = new StringBuilder("📋 *" + title + "*\n\n");
            log.info("Найдено активных записей ({}): {}", period, sortedAppointments.size());

            List<Appointment> pageAppointments = sortedAppointments.subList(start, end);
            rows.addAll(adminKeyboard.createAppointmentRows(pageAppointments));

            if (totalPages > 1) {
                List<InlineKeyboardButton> paginationButtons = new ArrayList<>();
                if (clampedPage > 0) {
                    paginationButtons.add(InlineKeyboardButton.builder()
                            .text("⬅️ Назад")
                            .callbackData("admin:appointments:page_" + (clampedPage - 1))
                            .build());
                }
                paginationButtons.add(InlineKeyboardButton.builder()
                        .text((clampedPage + 1) + "/" + totalPages)
                        .callbackData("noop")
                        .build());
                if (clampedPage < totalPages - 1) {
                    paginationButtons.add(InlineKeyboardButton.builder()
                            .text("Вперёд ➡️")
                            .callbackData("admin:appointments:page_" + (clampedPage + 1))
                            .build());
                }
                rows.add(new InlineKeyboardRow(paginationButtons));
            }

            rows.add(adminKeyboard.backToAppointmentsMenu());
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
            notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);
        } else {
            // Для TODAY / TOMORROW — без пагинации
            StringBuilder sb = new StringBuilder("📋 *" + period.getTitle() + "*\n\n");
            log.info("Найдено активных записей ({}): {}", period, sortedAppointments.size());

            rows.addAll(adminKeyboard.createAppointmentRows(sortedAppointments));
            rows.add(adminKeyboard.backToAppointmentsMenu());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
            notificationService.sendOrEditMessage(chatId, messageId, sb.toString(), markup);
        }
    }
}
