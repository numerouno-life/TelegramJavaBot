package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        notificationScheduler.scheduleNotifications(saved);
        return saved;
    }

    // Отменить запись
    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        appointmentRepository.deleteById(appointmentId);
        notificationScheduler.cancelNotifications(appointmentId);
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
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена"));

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
}
