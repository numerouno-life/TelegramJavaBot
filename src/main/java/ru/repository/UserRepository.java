package ru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.model.User;
import ru.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    Optional<User> findByClientPhoneNumber(String phoneNumber);

    List<User> findAllByIsBlockedTrue();

    List<User> findAllByRole(UserRole role);

}
