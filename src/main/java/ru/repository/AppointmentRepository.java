package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.model.Appointment;
import ru.model.enums.StatusAppointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserTelegramId(Long telegramId);

    List<Appointment> findByDateTime(LocalDateTime dateTime);

    // Все активные записи, отсортированные по дате
    List<Appointment> findAllByStatusOrderByDateTimeAsc(StatusAppointment statusAppointment);

    // Все записи, отсортированные по дате
    List<Appointment> findByDateTimeBetweenOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end);

    // Активные записи на конкретную дату
    List<Appointment> findByDateTimeBetweenAndStatusOrderByDateTimeAsc(LocalDateTime start, LocalDateTime end,
                                                                       StatusAppointment statusAppointment);

    @Query("SELECT COUNT(a) = 0 FROM Appointment a " +
            "WHERE a.dateTime = :dateTime " +
            "AND a.user.telegramId = :userId " +
            "AND a.status != 'CANCELED'")
    boolean isAvailableForUser(@Param("dateTime") LocalDateTime dateTime, @Param("userId") Long userId);

    Optional<Appointment> findTopByUserTelegramIdOrderByDateTimeDesc(Long chatId);
}
