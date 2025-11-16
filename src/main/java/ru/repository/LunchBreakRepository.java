package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.LunchBreak;

import java.util.List;

public interface LunchBreakRepository extends JpaRepository<LunchBreak, Long> {

    List<LunchBreak> findAllByOrderByDayOfWeekAsc();

    LunchBreak findByDayOfWeek(Integer dayOfWeek);
}
