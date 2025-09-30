package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.service.AppointmentService;
import ru.service.NotificationService;
import ru.service.UserSessionService;
import ru.service.WorkScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

        LocalTime startTime = workTime[0];
        LocalTime endTime = workTime[1];

        // 3. Генерируем слоты (по 1 часу)
        LocalDateTime current = localDate.atTime(startTime);
        LocalDateTime end = localDate.atTime(endTime);
        List<LocalDateTime> availableSlots = new ArrayList<>();

        while (!current.isAfter(end)) {
            if (current.isAfter(LocalDateTime.now()) && isTimeSlotAvailable(current)) {
                availableSlots.add(current);
            }
            current = current.plusHours(1);
        }
        return availableSlots;
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
        List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);

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

        for (User admin : admins) {
            if (admin.getTelegramId() != null && !admin.getIsBlocked()) {
                notificationService.sendMessage(admin.getTelegramId(), msg);
            }
        }
    }

    public void cancellationNoticeForAdmins(Appointment appointment) {
        List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);

        String msg = String.format(
                """
                        📢 Отмена записи!
                        
                        👤 Клиент: %s %s (@%s)
                        📞 Телефон: %s
                        📅 Дата и время: %s""",
                appointment.getUser().getFirstName(),
                appointment.getUser().getLastName() == null ? "" : appointment.getUser().getLastName(),
                appointment.getUser().getUsername(),
                appointment.getUser().getClientPhoneNumber(),
                appointment.getDateTime().format(DATE_FORMAT) + "-" + appointment.getDateTime().format(TIME_FORMAT)
        );

        for (User admin : admins) {
            if (admin.getTelegramId() != null && !admin.getIsBlocked()) {
                notificationService.sendMessage(admin.getTelegramId(), msg);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAppointmentInLast7Days(Long chatId, LocalDateTime newDateTime) {
        LocalDateTime sevenDaysAgo = newDateTime.minusDays(7);
        return appointmentRepository.findByUserTelegramId(chatId).stream()
                .anyMatch(app -> app.getStatus() == StatusAppointment.ACTIVE &&
                        !app.getDateTime().isBefore(sevenDaysAgo) &&
                        !app.getDateTime().isAfter(newDateTime));
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment getLastAppointmentWithin7Days(Long chatId, LocalDateTime newDateTime) {
        LocalDateTime sevenDaysAgo = newDateTime.minusDays(7);
        return appointmentRepository.findByUserTelegramId(chatId).stream()
                .filter(app -> app.getStatus() == StatusAppointment.ACTIVE ||
                        app.getStatus() == StatusAppointment.CONFIRMED)
                .filter(app -> app.getDateTime().isBefore(newDateTime)) // прошедшие или текущие
                .filter(app -> !app.getDateTime().isBefore(sevenDaysAgo)) // не старше 7 дней
                .max(Comparator.comparing(Appointment::getDateTime))
                .orElse(null);
    }

    @Override
    public Optional<Appointment> getLastAppointment(Long chatId) {
        return appointmentRepository
                .findTopByUserTelegramIdOrderByDateTimeDesc(chatId);
    }

}
