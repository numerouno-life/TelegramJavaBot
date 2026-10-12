package ru.service.impl;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.error.exception.AppointmentNotFoundException;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.AdminAppointmentState;
import ru.model.enums.StatusAppointment;
import ru.model.enums.UserAppointmentState;
import ru.model.enums.UserRole;
import ru.repository.AppointmentRepository;
import ru.repository.UserRepository;
import ru.scheduler.AppointmentNotificationScheduler;
import ru.service.*;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.util.BotConstants.DATE_FORMAT;
import static ru.util.BotConstants.TIME_FORMAT;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationScheduler notificationScheduler;
    private final WorkScheduleService workScheduleService;
    private final UserSessionService userSessionService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final KeyboardFactory keyboardFactory;
    private final LunchBreakService lunchBreakService;

    private final ExecutorService asyncNotificationExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    public void shutdown() {
        asyncNotificationExecutor.shutdown();
    }

    @Override
    public void setUserState(Long chatId, UserAppointmentState state) {
        if (chatId == null) {
            log.warn("Попытка установить состояние для chatId = null");
            return;
        }
        if (state == null) {
            clearUserState(chatId);
            return;
        }
        userSessionService.setUserState(chatId, state);
    }

    @Override
    public UserAppointmentState getUserState(Long chatId) {
        return userSessionService.getUserState(chatId);
    }

    @Override
    public void clearUserState(Long chatId) {
        userSessionService.clearUserState(chatId);
    }

    // Сохранить запись
    @Override
    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        log.info("Создание записи: user.id={}, username={}",
                appointment.getUser().getId(),
                appointment.getUser().getUsername());
        if (!isTimeSlotAvailable(appointment.getDateTime())) {
            throw new IllegalStateException("Слот уже занят");
        }
        appointment.setStatus(StatusAppointment.ACTIVE);
        Appointment saved = appointmentRepository.save(appointment);
        if (saved.getUser().getRole() == UserRole.USER) {
            notificationScheduler.scheduleNotifications(saved);
            notifyAdminsNewAppointment(saved);
        }

        log.info("Запись создана: {}", saved);
        return saved;
    }

    // Отменить запись
    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(
                () -> new AppointmentNotFoundException("Запись c id " + appointmentId + " не найдена"));
        if (appointment.getStatus() == StatusAppointment.CANCELED) {
            log.warn("Попытка отменить уже отмененную запись: {}", appointmentId);
            return;
        }
        // Меняем статус записи
        appointment.setStatus(StatusAppointment.CANCELED);
        appointmentRepository.save(appointment);
        notificationScheduler.cancelNotifications(appointmentId);
        log.info("Запись отменена: {}", appointmentId);
    }

    // Получить записи клиента
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getUserAppointments(Long chatId) {
        return appointmentRepository.findByUserTelegramId(chatId);
    }

    @Override
    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newDateTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Запись c id " + appointmentId + " не найдена"));

        // Отменить старую запись
        notificationScheduler.cancelNotifications(appointmentId);

        // Обновить время
        appointment.setDateTime(newDateTime);
        Appointment saved = appointmentRepository.save(appointment);

        // создаём новые уведомления
        notificationScheduler.scheduleNotifications(saved);
        return saved;
    }

    // Проверить доступность времени
    @Override
    public boolean isTimeSlotAvailable(LocalDateTime dateTime) {
        return appointmentRepository.findByDateTime(dateTime)
                .stream()
                .noneMatch(a -> a.getStatus() == StatusAppointment.ACTIVE);
    }

    // Получить доступные слоты на день (по часам: 10:00, 11:00, ..., 20:00)
    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableTimeSlots(LocalDateTime date) {
        LocalDate localDate = date.toLocalDate();
        // 1. Проверяем, рабочий ли день
        if (!workScheduleService.isWorkingDay(localDate)) {
            return List.of();
        }
        // 2. Получаем рабочее время
        LocalTime[] workTime = workScheduleService.getWorkTimeForDate(localDate);
        if (workTime == null) {
            return List.of();
        }

        LocalTime startTime = workTime[0];
        LocalTime endTime = workTime[1];

        // 3. Загружаем все активные записи за этот день одним запросом
        LocalDateTime dayStart = localDate.atStartOfDay();
        LocalDateTime dayEnd = localDate.atTime(23, 59, 59);
        List<Appointment> dayAppointments = appointmentRepository
                .findByDateTimeBetweenAndStatusOrderByDateTimeAsc(dayStart, dayEnd, StatusAppointment.ACTIVE);
        Set<LocalDateTime> bookedSlots = dayAppointments.stream()
                .map(Appointment::getDateTime)
                .collect(Collectors.toSet());

        // 4. Генерируем слоты (по 1 часу)
        LocalDateTime current = localDate.atTime(startTime);
        LocalDateTime end = localDate.atTime(endTime);
        List<LocalDateTime> availableSlots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        while (!current.isAfter(end)) {
            // Пропускаем слоты, которые попадают на обеденное время
            if (lunchBreakService.isLunchTime(current)){
                current = current.plusHours(1);
                continue;
            }

            if (current.isAfter(now) && !bookedSlots.contains(current)) {
                availableSlots.add(current);
            }
            current = current.plusHours(1);
        }
        return availableSlots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(LocalDate startDate, int daysCount) {
        LocalDateTime periodStart = startDate.atStartOfDay();
        LocalDateTime periodEnd = startDate.plusDays(daysCount).atTime(23, 59, 59);

        // 1 запрос к БД на весь период вместо N*11 запросов!
        List<Appointment> periodAppointments = appointmentRepository
                .findByDateTimeBetweenAndStatusOrderByDateTimeAsc(periodStart, periodEnd, StatusAppointment.ACTIVE);
        Set<LocalDateTime> bookedSlots = periodAppointments.stream()
                .map(Appointment::getDateTime)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        List<LocalDate> availableDates = new ArrayList<>();

        for (int i = 0; i < daysCount; i++) {
            LocalDate date = startDate.plusDays(i);
            if (!workScheduleService.isWorkingDay(date)) {
                continue;
            }

            LocalTime[] workTime = workScheduleService.getWorkTimeForDate(date);
            if (workTime == null) {
                continue;
            }

            LocalTime start = workTime[0];
            LocalTime end = workTime[1];
            LocalDateTime current = date.atTime(start);
            LocalDateTime endDateTime = date.atTime(end);

            boolean hasSlot = false;
            while (!current.isAfter(endDateTime)) {
                if (!lunchBreakService.isLunchTime(current)
                        && current.isAfter(now)
                        && !bookedSlots.contains(current)) {
                    hasSlot = true;
                    break;
                }
                current = current.plusHours(1);
            }

            if (hasSlot) {
                availableDates.add(date);
            }
        }
        return availableDates;
    }

    @Override
    public boolean isWorkingDay(LocalDate date) {
        return workScheduleService.isWorkingDay(date);
    }

    // Временное сохранение даты
    @Override
    public void setPendingDate(Long chatId, LocalDateTime dateTime) {
        log.info("💾 Сохраняем pendingDate: {} → {}", chatId, dateTime);
        userSessionService.setPendingDate(chatId, dateTime);
    }

    @Override
    public LocalDateTime getPendingDate(Long chatId) {
        return userSessionService.getPendingDate(chatId);
    }

    @Override
    public Appointment findById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow(() ->
                new AppointmentNotFoundException("Запись с id " + appointmentId + " не найдена"));
    }

    // Получить активные записи клиента
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getActiveAppointments(Long chatId) {
        return appointmentRepository.findByUserTelegramId(chatId).stream()
                .filter(app -> app.getStatus() != StatusAppointment.CANCELED &&
                        app.getDateTime().isAfter(LocalDateTime.now()))
                .toList();
    }

    // Получить прошедшие записи клиента
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getPastAppointments(Long chatId) {
        return appointmentRepository.findByUserTelegramId(chatId).stream()
                .filter(app -> app.getDateTime().isBefore(LocalDateTime.now())
                        || app.getStatus() == StatusAppointment.CANCELED)
                .toList();
    }

    @Override
    @Transactional
    public Appointment updateAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    @Override
    public void setPendingMessageId(Long chatId, Integer messageId) {
        userSessionService.setPendingMessageId(chatId, messageId);
    }

    @Override
    public Integer getPendingMessageId(Long chatId) {
        return userSessionService.getPendingMessageId(chatId);
    }

    @Override
    public void clearPendingMessageId(Long chatId) {
        userSessionService.clearPendingMessageId(chatId);
    }

    @Override
    public void setHistoryPage(Long chatId, int page) {
        log.debug("setHistoryPage: chatId={}, page={}", chatId, page);
        userSessionService.setHistoryPage(chatId, page);
        log.debug("Текущее состояние historyPage: {}", userSessionService.getHistoryPage(chatId));
    }

    @Override
    public int getHistoryPage(Long chatId) {
        Integer page = userSessionService.getHistoryPage(chatId);
        log.debug("getHistoryPage: chatId={}, page={}", chatId, page);
        return page;
    }

    @Override
    public void clearHistoryPage(Long chatId) {
        userSessionService.clearHistoryPage(chatId);
    }

    @Override
    public String getPendingName(Long chatId) {
        if (chatId == null) {
            log.warn("Попытка получить имя для chatId = null");
            return null;
        } else {
            return userSessionService.getPendingName(chatId);
        }
    }

    @Override
    public void setAdminState(Long chatId, AdminAppointmentState state) {
        userSessionService.setAdminState(chatId, state);
    }

    @Override
    public AdminAppointmentState getAdminState(Long chatId) {
        return userSessionService.getAdminState(chatId);
    }

    @Override
    public void clearAdminState(Long chatId) {
        userSessionService.clearAdminState(chatId);
    }

    @Override
    public void clearPendingDate(Long chatId) {
        userSessionService.clearPendingDate(chatId);
    }

    private void notifyAdminsNewAppointment(Appointment appointment) {
        String msg = String.format(
                """
                        📢 Новая запись!
                        
                        👤 Клиент: %s %s (@%s)
                        📞 Телефон: %s
                        📅 Дата и время: %s""",
                appointment.getUser().getFirstName() == null ? "" : appointment.getUser().getFirstName(),
                appointment.getUser().getLastName() == null ? "" : appointment.getUser().getLastName(),
                appointment.getUser().getUsername() == null ? "Нет NickName" : appointment.getUser().getUsername(),
                appointment.getUser().getClientPhoneNumber(),
                appointment.getDateTime().format(DATE_FORMAT) + "-" + appointment.getDateTime().format(TIME_FORMAT)
        );

        CompletableFuture.runAsync(() -> {
            try {
                List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
                for (User admin : admins) {
                    if (admin.getTelegramId() != null && !admin.getIsBlocked()) {
                        notificationService.sendMessage(admin.getTelegramId(), msg);
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при асинхронной отправке уведомлений администраторам: {}", e.getMessage(), e);
            }
        }, asyncNotificationExecutor);
    }

    public void cancellationNoticeForAdmins(Appointment appointment) {
        String msg = String.format(
                """
                        📢 Отмена записи!
                        
                        👤 Клиент: %s %s (@%s)
                        📞 Телефон: %s
                        📅 Дата и время: %s""",
                appointment.getUser().getFirstName() == null ? "" : appointment.getUser().getFirstName(),
                appointment.getUser().getLastName() == null ? "" : appointment.getUser().getLastName(),
                appointment.getUser().getUsername() == null ? "Нет NickName" : appointment.getUser().getUsername(),
                appointment.getUser().getClientPhoneNumber(),
                appointment.getDateTime().format(DATE_FORMAT) + "-" + appointment.getDateTime().format(TIME_FORMAT)
        );
        InlineKeyboardMarkup backButton = keyboardFactory.backButton("⬅️ Назад", "back_to_menu");

        CompletableFuture.runAsync(() -> {
            try {
                List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
                for (User admin : admins) {
                    if (admin.getTelegramId() != null && !admin.getIsBlocked()) {
                        notificationService.sendOrEditMessage(admin.getTelegramId(), null, msg, backButton);
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при асинхронной отправке уведомления об отмене администраторам: {}", e.getMessage(), e);
            }
        }, asyncNotificationExecutor);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAppointmentInLast6Days(Long chatId, LocalDateTime newDateTime) {
        LocalDateTime sevenDaysAgo = newDateTime.minusDays(6);
        return appointmentRepository.findByUserTelegramId(chatId).stream()
                .anyMatch(app -> app.getStatus() == StatusAppointment.ACTIVE &&
                        !app.getDateTime().isBefore(sevenDaysAgo) &&
                        !app.getDateTime().isAfter(newDateTime));
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment getLastAppointmentWithin6Days(Long chatId, LocalDateTime newDateTime) {
        LocalDateTime sixDaysAgo = newDateTime.minusDays(6);
        return appointmentRepository.findByUserTelegramId(chatId).stream()
                .filter(app -> app.getStatus() == StatusAppointment.ACTIVE ||
                        app.getStatus() == StatusAppointment.CONFIRMED)
                .filter(app -> app.getDateTime().isBefore(newDateTime)) // прошедшие или текущие
                .filter(app -> !app.getDateTime().isBefore(sixDaysAgo)) // не старше 6 дней
                .max(Comparator.comparing(Appointment::getDateTime))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Appointment> getLastAppointment(Long chatId) {
        return appointmentRepository
                .findTopByUserTelegramIdAndStatusNotOrderByDateTimeDesc(
                        chatId, StatusAppointment.CANCELED
                );
    }

}
