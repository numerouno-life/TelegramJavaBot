package ru.service;

import ru.model.User;

import java.util.Optional;

public interface UserService {

    User getOrCreateUser(Long telegramId, String username, String firstName, String lastName);

    User updateUserPhone(Long telegramId, String phone);

    Optional<User> findByTelegramId(Long telegramId);
}
