package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserTelegramId(Long telegramId);

    List<Appointment> findByDateTime(LocalDateTime dateTime);

}
