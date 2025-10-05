package ru.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.model.Appointment;
import ru.service.NotificationService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@AllArgsConstructor
public class AppointmentNotificationScheduler {

    private final NotificationService notificationService;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public void scheduleNotifications(Appointment appointment) {
        LocalDateTime appointmentTime = appointment.getDateTime();

        // Напоминание за день
        scheduleTask(appointment, appointmentTime.minusDays(1),
                "📅 Напоминаем: завтра у вас запись на %s!");

        // Напоминание за 2 часа
        scheduleTask(appointment, appointmentTime.minusHours(2),
                "⏰ Напоминаем: через 2 часа у вас запись на %s!");
    }

    private void scheduleTask(Appointment appointment, LocalDateTime notifyTime, String template) {
        LocalDateTime now = LocalDateTime.now();

        if (notifyTime.isBefore(now)) {
            return; // Напоминание уже прошло
        }

        long delay = Duration.between(now, notifyTime).toMillis();

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                String message = template.formatted(
                        appointment.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'в' HH:mm"))
                );
                if (appointment.getUser().getTelegramId() == null) {
                    log.warn("Пользователь {} не имеет Telegram ID", appointment.getUser().getUsername());
                    return; // Пропускаем отправку, если Telegram ID отсутствует
                }
                notificationService.sendMessage(
                        appointment.getUser().getTelegramId(),
                        message
                );
                log.info("Напоминание отправлено клиенту {} о записи на {}",
                        appointment.getUser().getUsername(), appointment.getDateTime());
            } catch (Exception e) {
                log.error("Ошибка при отправке напоминания", e);
            }
        }, delay, TimeUnit.MILLISECONDS);
        // сохраняем задачу (ключ = ID записи)
        scheduledTasks.put(appointment.getId(), future);
    }

    public void cancelNotifications(Long appointmentId) {
        ScheduledFuture<?> future = scheduledTasks.remove(appointmentId);
        if (future != null) {
            future.cancel(true);
            log.info("Уведомления для записи {} отменены", appointmentId);
        }
    }

}
