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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationScheduler notificationScheduler;

    // Состояния пользователя
    private final Map<Long, LocalDateTime> pendingDates = new ConcurrentHashMap<>();
    private final Map<Long, String> userStates = new ConcurrentHashMap<>(); // Храним состояние пользователя
    private final Map<Long, String> pendingNames = new ConcurrentHashMap<>();
    private final Map<Long, Integer> pendingMessageId = new ConcurrentHashMap<>();
    private final Map<Long, Integer> historyPage = new ConcurrentHashMap<>();

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
        userStates.put(chatId, status);
    }

    @Override
    public String getUserState(Long chatId) {
        return userStates.get(chatId);
    }

    @Override
    public void clearUserState(Long chatId) {
        userStates.remove(chatId);
        pendingDates.remove(chatId);
        pendingNames.remove(chatId);
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
        return appointmentRepository.findByClientChatId(chatId);
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
        List<LocalDateTime> availableSlots = new ArrayList<>();
        LocalDateTime start = date.withHour(10).withMinute(0);
        LocalDateTime end = date.withHour(21).withMinute(0); // до 20:00

        while (start.isBefore(end)) {
            if (start.isBefore(LocalDateTime.now())) {
                start = start.plusHours(1);
                continue;
            }
            if (isTimeSlotAvailable(start)) {
                availableSlots.add(start);
            }
            start = start.plusHours(1);
        }
        return availableSlots;
    }

    // Временное сохранение даты
    @Override
    public void setPendingDate(Long chatId, LocalDateTime dateTime) {
        pendingDates.put(chatId, dateTime);
    }

    @Override
    public LocalDateTime getPendingDate(Long chatId) {
        return pendingDates.get(chatId);
    }

    @Override
    public void setPendingName(Long chatId, String name) {
        pendingNames.put(chatId, name);
    }

    @Override
    public String getPendingName(Long chatId) {
        return pendingNames.get(chatId);
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
        return appointmentRepository.findByClientChatId(chatId).stream()
                .filter(app -> app.getStatus() != StatusAppointment.CANCELED &&
                        app.getDateTime().isAfter(LocalDateTime.now()))
                .toList();
    }

    // Получить прошедшие записи клиента
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getPastAppointments(Long chatId) {
        return appointmentRepository.findByClientChatId(chatId).stream()
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
        pendingMessageId.put(chatId, messageId);
    }

    @Override
    public Integer getPendingMessageId(Long chatId) {
        return pendingMessageId.get(chatId);
    }

    @Override
    public void clearPendingMessageId(Long chatId) {
        pendingMessageId.remove(chatId);
    }

    @Override
    public void setHistoryPage(Long chatId, int page) {
        log.debug("setHistoryPage: chatId={}, page={}", chatId, page);
        historyPage.put(chatId, page);
        log.debug("Текущее состояние historyPage: {}", historyPage);
    }

    @Override
    public int getHistoryPage(Long chatId) {
        int page = historyPage.getOrDefault(chatId, 0);
        log.debug("getHistoryPage: chatId={}, page={}", chatId, page);
        return page;
    }

    @Override
    public void clearHistoryPage(Long chatId) {
        historyPage.remove(chatId);
    }
}
