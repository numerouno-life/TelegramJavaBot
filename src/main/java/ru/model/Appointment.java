package ru.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.model.enums.StatusAppointment;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "appointments")
/**  Запись  **/
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "client_chat_id", nullable = false)
    Long clientChatId;

    @Column(name = "client_name", nullable = false)
    String clientName;

    @Column(name = "client_phone_number", nullable = false)
    String clientPhoneNumber;

    @Column(name = "date_time", nullable = false)
    LocalDateTime dateTime;

    @Column(name = "price")
    Double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    StatusAppointment status;
}