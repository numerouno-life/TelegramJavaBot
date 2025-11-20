package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
