package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.WorkSchedule;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    WorkSchedule findByDayOfWeek(Integer dayOfWeek);
}
