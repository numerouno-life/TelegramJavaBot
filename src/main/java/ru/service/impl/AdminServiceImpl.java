package ru.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.StatusAppointment;
import ru.repository.AppointmentRepository;
import ru.repository.UserRepository;
import ru.service.AdminService;
import ru.service.AppointmentService;
import ru.util.KeyboardFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final KeyboardFactory keyboardFactory;
    private final NotificationServiceImpl notificationService;

    @Transactional(readOnly = true)
    @Override
    public List<Appointment> getAllActiveAppointments() {
        return appointmentRepository.findAllByStatusOrderByDateTimeAsc(StatusAppointment.ACTIVE).stream()
                .filter(app -> app.getDateTime().isAfter(LocalDateTime.now()))
                .toList();
    }

    // Записи на конкретный день
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDate(LocalDateTime dateTime) {
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(23, 59);
        return appointmentRepository.findByDateTimeBetweenAndStatusNotOrderByDateTimeAsc(startOfDay, endOfDay,
                StatusAppointment.ACTIVE);
    }

    // Записи на сегодня
    @Transactional(readOnly = true)
    @Override
    public List<Appointment> getAppointmentsToday() {
        return getAppointmentsByDate(LocalDateTime.now());
    }

    // Записи на завтра
    @Transactional(readOnly = true)
    @Override
    public List<Appointment> getAppointmentsTomorrow() {
        return getAppointmentsByDate(LocalDateTime.now().plusDays(1));
    }

    // Записи на неделю
    @Transactional(readOnly = true)
    @Override
    public Map<LocalDateTime, List<Appointment>> getAppointmentsThisWeek() {
        LocalDateTime today = LocalDateTime.now();
        Map<LocalDateTime, List<Appointment>> weekAppointments = new TreeMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDateTime date = today.plusDays(i);
            weekAppointments.put(date, getAppointmentsByDate(date));
        }
        return weekAppointments;
    }

    // блокировка пользователя
    @Override
    @Transactional
    public void blockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsBlocked(true);
            userRepository.save(user);
        });
    }

    // разблокировка пользователя
    @Override
    @Transactional
    public void unblockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsBlocked(false);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getBlockedUsers() {
        if (userRepository.findAllByIsBlockedTrue().isEmpty()) {
            return List.of();
        } else {
            return userRepository.findAllByIsBlockedTrue();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        if (userRepository.findAll().isEmpty()) {
            return List.of();
        } else {
            return userRepository.findAll();
        }
    }

    @Override
    public void sendTimeSelectionForAdmin(Long chatId, Integer messageId, LocalDate date) {
        List<LocalDateTime> availableSlots = appointmentService.getAvailableTimeSlots(date.atStartOfDay())
                .stream()
                .filter(slot -> slot.isAfter(LocalDateTime.now()))
                .toList();

        InlineKeyboardMarkup markup = keyboardFactory.timeSelectionKeyboard(date, availableSlots);

        notificationService.sendOrEditMessage(chatId, messageId,
                "Доступное время на " + date.format(DateTimeFormatter.ofPattern("dd.MM (E)")) + ":\n🟢 - свободно",
                markup
        );
    }


}
// • ✅ Записать пользователя +
// • ✅ Отменить запись +
// • ✅ Блокировка/разблокировка пользователей +
//• ✅ Список заблокированных пользователей +
// • ✅ Просмотр всех записей +
// • ✅ Редактирование рабочего времени
// • ✅ Редактирование рабочих дней
// • ✅ Показать список записей по дате +
//
//Дополнительные идеи (на будущее):
// • Уведомления о записи (за день до)
// • Статистика: сколько записей в день/неделю
// • Лимит записей на день