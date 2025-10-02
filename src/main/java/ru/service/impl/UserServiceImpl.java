package ru.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.model.User;
import ru.model.enums.UserRole;
import ru.repository.UserRepository;
import ru.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public User getOrCreateUser(Long telegramId, String username, String firstName, String lastName) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .telegramId(telegramId)
                            .role(UserRole.USER)
                            .isBlocked(false)
                            .build();
                    log.info("Создан новый пользователь: {}", newUser);
                    return newUser;
                });

        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        log.info("Обновление пользователя: id={}, username={}, firstName={}", telegramId, username, firstName);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserDetails(Long telegramId, String name, String phone) {
        log.info("Обновление пользователя: chatId={}, name={}, phone={}",
                telegramId, name, phone);
        User user = findUserById(telegramId);
        user.setClientPhoneNumber(phone);
        user.setFirstName(name);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    @Override
    public Boolean isAdmin(Long chatId) {
        return findByTelegramId(chatId)
                .map(user -> UserRole.ADMIN.equals(user.getRole()))
                .orElse(false);
    }

    @Override
    public boolean isBlocked(Long userId) {
        return userRepository.findByTelegramId(userId)
                .map(User::getIsBlocked)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return Optional.ofNullable(userRepository.findByClientPhoneNumber(phoneNumber).orElseThrow(
                () -> {
                    log.warn("Пользователь с телефоном {} не найден", phoneNumber);
                    return new EntityNotFoundException("Пользователь с телефоном " + phoneNumber + " не найден");
                }));
    }

    @Override
    public User findOrCreateByPhone(String phone, String name) {
        return userRepository.findByClientPhoneNumber(phone)
                .orElseGet(() -> {
                    User user = User.builder()
                            .clientPhoneNumber(phone)
                            .firstName(name)
                            .username("phone_" + phone.replaceAll("\\D+", "")) // временный username
                            .createdAt(LocalDateTime.now())
                            .isBlocked(false)
                            .build();
                    return userRepository.save(user);
                });
    }


    @Override
    public User createUserWithPhoneAndName(Long chatId, String phoneNumber, String firstName) {
        if (findByPhoneNumber(phoneNumber).isEmpty()) {
            User user = User.builder()
                    .telegramId(chatId)
                    .clientPhoneNumber(phoneNumber)
                    .firstName(firstName)
                    .build();
            return userRepository.save(user);
        }
        return findByPhoneNumber(phoneNumber).get();
    }

    @Override
    public User createClientOnly(String name, String phone) {
        User user = new User();
        user.setFirstName(name);
        user.setClientPhoneNumber(phone);
        user.setUsername("📞 " + phone);
        user.setIsBlocked(false);
        return userRepository.save(user);
    }


    private User findUserById(Long telegramId) {
        return userRepository.findByTelegramId(telegramId).orElseThrow(
                () -> {
                    log.warn("Пользователь с id {} не найден", telegramId);
                    return new EntityNotFoundException("Пользователь c id " + telegramId + " не найден");
                });
    }
}
