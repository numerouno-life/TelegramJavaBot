package ru.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.model.enums.ServiceType;

import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "client_phone_number")
    String clientPhoneNumber;

    @Column(name = "client_name")
    String clientName;

    @Column(name = "amount", nullable = false)
    Double amount;

    @Enumerated(EnumType.STRING)
    ServiceType serviceType;

    @Column(name = "service_date", nullable = false)
    LocalDateTime serviceDate; // когда стриглись

    @Column(name = "payment_date", updatable = false, nullable = false)
    LocalDateTime paymentDate; // когда оплатили = когда создали запись

    @Column(name = "created_by", nullable = false)
    Long createdBy;

    @Column(name = "comment")
    String comment;
}
