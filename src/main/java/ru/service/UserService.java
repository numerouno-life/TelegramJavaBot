package ru.service;

import ru.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User getOrCreateUser(Long telegramId, String username, String firstName, String lastName);

    User updateUserDetails(Long telegramId,String name, String phone);

    Optional<User> findByTelegramId(Long telegramId);

    Boolean isAdmin(Long telegramId);

    List<User> getAllUsers();

    Optional<User> findByPhoneNumber(String phoneNumber);

    User findOrCreateByPhone(String phone, String name);

    User createUserWithPhoneAndName(Long chatId, String phoneNumber, String firstName);

    User createClientOnly(String name, String phone);

    void save(User user);
}
