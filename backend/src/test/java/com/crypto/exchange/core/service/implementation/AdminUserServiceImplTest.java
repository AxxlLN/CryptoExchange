package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.exception.BadRequestException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.UserMapper;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletBalanceRepository walletBalanceRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User activeUser;
    private User deletedUser;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.ROLE_USER)
                .avatarUrl("http://avatar.com/1.png")
                .deleted(false)
                .blocked(false)
                .createdAt(OffsetDateTime.now())
                .build();

        deletedUser = User.builder()
                .id(2L)
                .username("deleted_user_2")
                .email("deleted_uuid@deleted.local")
                .password("encoded_uuid")
                .role(Role.ROLE_USER)
                .deleted(true)
                .blocked(false)
                .createdAt(OffsetDateTime.now())
                .build();

        userResponseDto = new UserResponseDto(
                1L,
                "john_doe",
                "john@example.com",
                "http://avatar.com/1.png",
                Role.ROLE_USER,
                false,
                false,
                activeUser.getCreatedAt()
        );
    }

    @Test
    void getAllUsersShouldReturnOnlyNonDeletedUsers() {
        when(userRepository.findAll()).thenReturn(List.of(activeUser, deletedUser));
        when(userMapper.toResponseDto(activeUser)).thenReturn(userResponseDto);

        List<UserResponseDto> result = adminUserService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(userResponseDto);
        verify(userMapper).toResponseDto(activeUser);
        verify(userMapper, never()).toResponseDto(deletedUser);
    }

    @Test
    void getUserByIdShouldReturnUserDtoWhenUserExistsAndNotDeleted() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userMapper.toResponseDto(activeUser)).thenReturn(userResponseDto);

        UserResponseDto result = adminUserService.getUserById(1L);

        assertThat(result).isEqualTo(userResponseDto);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByIdShouldThrowResourceNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь с ID 99 не найден");
    }

    @Test
    void getUserByIdShouldThrowResourceNotFoundExceptionWhenUserIsDeleted() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> adminUserService.getUserById(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь с ID 2 не найден");
    }

    @Test
    void blockUserByUsernameShouldSetBlockedToTrueAndSave() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(activeUser));

        adminUserService.blockUserByUsername("john_doe");

        assertThat(activeUser.isBlocked()).isTrue();
        verify(userRepository).save(activeUser);
    }

    @Test
    void blockUserByUsernameShouldThrowResourceNotFoundExceptionWhenUserNotFoundOrDeleted() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.blockUserByUsername("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь с именем 'unknown' не найден");

        verify(userRepository, never()).save(any());
    }

    @Test
    void unblockUserByUsernameShouldSetBlockedToFalseAndSave() {
        activeUser.setBlocked(true);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(activeUser));

        adminUserService.unblockUserByUsername("john_doe");

        assertThat(activeUser.isBlocked()).isFalse();
        verify(userRepository).save(activeUser);
    }

    @Test
    void unblockUserByUsernameShouldThrowResourceNotFoundExceptionWhenUserNotFoundOrDeleted() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.unblockUserByUsername("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь с именем 'unknown' не найден");

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUserShouldAnonymizeAndSoftDeleteUserWhenNoPositiveBalances() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(walletBalanceRepository.hasPositiveBalances(1L)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_random_password");

        adminUserService.deleteUser(1L);

        assertThat(activeUser.isDeleted()).isTrue();
        assertThat(activeUser.getDeletedAt()).isNotNull();
        assertThat(activeUser.getEmail()).startsWith("deleted_").endsWith("@deleted.local");
        assertThat(activeUser.getUsername()).isEqualTo("deleted_user_1");
        assertThat(activeUser.getPassword()).isEqualTo("new_encoded_random_password");
        assertThat(activeUser.getAvatarUrl()).isNull();

        verify(userRepository).save(activeUser);
    }

    @Test
    void deleteUserShouldThrowBadRequestExceptionWhenUserHasPositiveBalances() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(walletBalanceRepository.hasPositiveBalances(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.deleteUser(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Нельзя удалить аккаунт с активным балансом.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUserShouldThrowResourceNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь с ID 99 не найден");

        verify(userRepository, never()).save(any());
    }
}