package ru.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.model.User;
import ru.model.enums.UserRole;
import ru.repository.UserRepository;
import ru.service.impl.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    // ─────────────────── getOrCreateUser ───────────────────

    @Test
    void getOrCreateUser_CreatesNewUser_WhenNotFound() {
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.empty());
        User saved = User.builder().telegramId(100L).username("user1").firstName("Ivan").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.getOrCreateUser(100L, "user1", "Ivan", "Ivanov");

        verify(userRepository).save(any(User.class));
        assertNotNull(result);
    }

    @Test
    void getOrCreateUser_ReturnsExistingUser_WhenDataUnchanged() {
        User existing = User.builder()
                .telegramId(101L)
                .username("same_user")
                .firstName("Anna")
                .lastName("Smith")
                .build();
        when(userRepository.findByTelegramId(101L)).thenReturn(Optional.of(existing));

        User result = userService.getOrCreateUser(101L, "same_user", "Anna", "Smith");

        verify(userRepository, never()).save(any());
        assertEquals(existing, result);
    }

    @Test
    void getOrCreateUser_UpdatesUser_WhenDataChanged() {
        User existing = User.builder()
                .telegramId(102L)
                .username("old_name")
                .firstName("OldFirst")
                .build();
        when(userRepository.findByTelegramId(102L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        User result = userService.getOrCreateUser(102L, "new_name", "OldFirst", null);

        verify(userRepository).save(existing);
    }

    // ─────────────────── updateUserDetails ───────────────────

    @Test
    void updateUserDetails_UpdatesPhoneAndName() {
        User user = User.builder().telegramId(200L).build();
        when(userRepository.findByTelegramId(200L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateUserDetails(200L, "NewName", "+79001234567");

        assertEquals("NewName", user.getFirstName());
        assertEquals("+79001234567", user.getClientPhoneNumber());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserDetails_ThrowsEntityNotFoundException_WhenUserNotFound() {
        when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updateUserDetails(999L, "Name", "Phone"));
    }

    // ─────────────────── isAdmin ───────────────────

    @Test
    void isAdmin_ReturnsTrue_ForAdminUser() {
        User admin = User.builder().role(UserRole.ADMIN).build();
        when(userRepository.findByTelegramId(1L)).thenReturn(Optional.of(admin));

        assertTrue(userService.isAdmin(1L));
    }

    @Test
    void isAdmin_ReturnsFalse_ForRegularUser() {
        User user = User.builder().role(UserRole.USER).build();
        when(userRepository.findByTelegramId(2L)).thenReturn(Optional.of(user));

        assertFalse(userService.isAdmin(2L));
    }

    @Test
    void isAdmin_ReturnsFalse_WhenUserNotFound() {
        when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

        assertFalse(userService.isAdmin(999L));
    }

    // ─────────────────── isBlocked ───────────────────

    @Test
    void isBlocked_ReturnsTrue_WhenUserBlocked() {
        when(userRepository.findByTelegramId(3L))
                .thenReturn(Optional.of(User.builder().isBlocked(true).build()));

        assertTrue(userService.isBlocked(3L));
    }

    @Test
    void isBlocked_ReturnsFalse_WhenUserNotBlocked() {
        when(userRepository.findByTelegramId(4L))
                .thenReturn(Optional.of(User.builder().isBlocked(false).build()));

        assertFalse(userService.isBlocked(4L));
    }

    @Test
    void isBlocked_ReturnsFalse_WhenUserNotFound() {
        when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

        assertFalse(userService.isBlocked(999L));
    }

    // ─────────────────── findOrCreateByPhone ───────────────────

    @Test
    void findOrCreateByPhone_ReturnsExisting_WhenFound() {
        User existing = User.builder().clientPhoneNumber("+7000").build();
        when(userRepository.findByClientPhoneNumber("+7000")).thenReturn(Optional.of(existing));

        User result = userService.findOrCreateByPhone("+7000", "Name");

        assertEquals(existing, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateByPhone_CreatesNew_WhenNotFound() {
        when(userRepository.findByClientPhoneNumber("+7111")).thenReturn(Optional.empty());
        User saved = User.builder().clientPhoneNumber("+7111").firstName("Igor").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.findOrCreateByPhone("+7111", "Igor");

        verify(userRepository).save(any(User.class));
        assertEquals("+7111", result.getClientPhoneNumber());
    }

    // ─────────────────── createUserWithPhoneAndName ───────────────────

    @Test
    void createUserWithPhoneAndName_ReturnsExisting_WhenPhoneFound() {
        User existing = User.builder().clientPhoneNumber("+7222").build();
        when(userRepository.findByClientPhoneNumber("+7222")).thenReturn(Optional.of(existing));

        User result = userService.createUserWithPhoneAndName(500L, "+7222", "Maria");

        assertEquals(existing, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserWithPhoneAndName_CreatesNew_WhenPhoneNotFound() {
        when(userRepository.findByClientPhoneNumber("+7333")).thenReturn(Optional.empty());
        User saved = User.builder().telegramId(500L).clientPhoneNumber("+7333").firstName("Olga").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.createUserWithPhoneAndName(500L, "+7333", "Olga");

        verify(userRepository).save(any(User.class));
        assertNotNull(result);
    }

    // ─────────────────── createClientOnly ───────────────────

    @Test
    void createClientOnly_SavesUserWithCorrectFields() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createClientOnly("Sergey", "+79001112233");

        assertEquals("Sergey", result.getFirstName());
        assertEquals("+79001112233", result.getClientPhoneNumber());
        assertEquals("📞 +79001112233", result.getUsername());
        assertFalse(result.getIsBlocked());
    }

    // ─────────────────── getAllUsers ───────────────────

    @Test
    void getAllUsers_ReturnsAllFromRepository() {
        List<User> users = List.of(new User(), new User(), new User());
        when(userRepository.findAll()).thenReturn(users);

        assertEquals(3, userService.getAllUsers().size());
    }
}
