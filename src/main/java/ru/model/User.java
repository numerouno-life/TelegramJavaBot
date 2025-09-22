package ru.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.model.enums.UserRole;

import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "telegram_id")
    Long telegramId;

    @Column(name = "username")
    String username; // telegram @username

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "client_phone_number")
    String clientPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    UserRole role = UserRole.USER;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    Boolean isBlocked = false;

    @Column(name = "created_at", updatable = false, insertable = false)
    LocalDateTime createdAt;

}
