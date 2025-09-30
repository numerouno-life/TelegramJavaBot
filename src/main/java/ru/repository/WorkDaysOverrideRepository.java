package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.WorkDaysOverride;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkDaysOverrideRepository extends JpaRepository<WorkDaysOverride, Long> {

    Optional<WorkDaysOverride> findByDate(LocalDate date);

    List<WorkDaysOverride> findAllByOrderByDateDesc();

    void deleteByDate(LocalDate date);
}
