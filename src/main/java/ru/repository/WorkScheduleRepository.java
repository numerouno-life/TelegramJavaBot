package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.WorkSchedule;

import java.util.List;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    WorkSchedule findByDayOfWeek(Integer dayOfWeek);

    List<WorkSchedule> findAllByOrderByDayOfWeekAsc();
}
