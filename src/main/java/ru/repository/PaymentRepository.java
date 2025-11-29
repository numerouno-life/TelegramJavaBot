package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.serviceDate >= :start AND p.serviceDate < :end")
    BigDecimal sumAmountByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p")
    BigDecimal sumAllAmounts();

    @Query(value = """
            SELECT CAST(p.service_date AS DATE) AS day, SUM(p.amount) AS total
            FROM payments p
            WHERE p.service_date >= :start AND p.service_date < :end
            GROUP BY CAST(p.service_date AS DATE)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> findDailySumsByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM p.service_date) AS hour, SUM(p.amount) AS total
            FROM payments p
            WHERE CAST(p.service_date AS DATE) = :date
            GROUP BY EXTRACT(HOUR FROM p.service_date)
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> findHourlySumsByDate(@Param("date") LocalDate date);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM p.service_date) AS hour, COUNT(*) AS count, SUM(p.amount) AS total
            FROM payments p
            WHERE p.service_date >= :start AND p.service_date < :end
            GROUP BY EXTRACT(HOUR FROM p.service_date)
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> findHourlyStatsByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    List<Payment> findByServiceDateBetween(LocalDateTime start, LocalDateTime end);
}