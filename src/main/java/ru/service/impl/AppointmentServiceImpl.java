package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.error.exception.AppointmentNotFoundException;
import ru.model.Appointment;
import ru.model.enums.StatusAppointment;
import ru.repository.AppointmentRepository;
import ru.scheduler.AppointmentNotificationScheduler;
import ru.service.AppointmentService;
import ru.service.UserSessionService;
import ru.service.WorkScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationScheduler notificationScheduler;
    private final WorkScheduleService workScheduleService;
    private final UserSessionService userSessionService;

    @Override
    public void setUserState(Long chatId, String status) {
        if (chatId == null) {
            log.warn("Попытка установить состояние для chatId = null");
            return;
        }
        if (status == null) {
            clearUserState(chatId);
            return;
        }
        userSessionService.setUserState(chatId, status);
    }

    @Override
    public String getUserState(Long chatId) {
        return userSessionService.getUserState(chatId);
    }

    @Override
    public void clearUserState(Long chatId) {
        userSessionService.clearUserState(chatId);
    }

    // Сохранить запись
    @Transactional
    @Override
    public Appointment createAppointment(Appointment appointment) {
        Appointment saved = appointmentRepository.save(appointment);
        saved.setStatus(StatusAppointment.CONFIRMED);
        notificationScheduler.scheduleNotifications(saved);
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
                .noneMatch(a -> a.getStatus() != StatusAppointment.CANCELED);
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

        while (current.isBefore(end)) {
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
}
