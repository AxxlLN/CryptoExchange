package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.exception.BadRequestException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.UserMapper;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.web.dto.request.ChangePasswordRequest;
import com.crypto.exchange.web.dto.request.UpdateAvatarRequest;
import com.crypto.exchange.web.dto.request.UpdateProfileRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletBalanceRepository walletBalanceRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_old_password")
                .role(Role.ROLE_USER)
                .avatarUrl("https://example.com/avatar.png")
                .blocked(false)
                .deleted(false)
                .createdAt(OffsetDateTime.now())
                .build();

        userResponseDto = new UserResponseDto(
                1L,
                "john_doe",
                "john@example.com",
                "https://example.com/avatar.png",
                Role.ROLE_USER,
                false,
                false,
                OffsetDateTime.now()
        );
    }

    @Test
    void getUserProfileShouldReturnUserProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.getUserProfile(1L);

        assertThat(result).isEqualTo(userResponseDto);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserProfileShouldThrowResourceNotFoundExceptionWhenUserNotFoundOrDeleted() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Пользователь с ID 1 не найден");

        verify(userMapper, never()).toResponseDto(any());
    }

    @Test
    void deleteOwnProfileShouldThrowBadRequestExceptionWhenUserHasPositiveBalances() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(walletBalanceRepository.hasPositiveBalances(1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteOwnProfile(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete account with active funds");

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteOwnProfileShouldAnonymizeAndSoftDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(walletBalanceRepository.hasPositiveBalances(1L)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_random_password");

        userService.deleteOwnProfile(1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.isDeleted()).isTrue();
        assertThat(savedUser.getDeletedAt()).isNotNull();
        assertThat(savedUser.getEmail()).startsWith("deleted_").endsWith("@deleted.local");
        assertThat(savedUser.getUsername()).isEqualTo("deleted_user_1");
        assertThat(savedUser.getAvatarUrl()).isNull();
        assertThat(savedUser.getPassword()).isEqualTo("encoded_random_password");
    }

    @Test
    void updateAvatarShouldUpdateAvatarUrlAndReturnDto() {
        UpdateAvatarRequest request = new UpdateAvatarRequest("https://example.com/new_avatar.png");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.updateAvatar(1L, request);

        assertThat(result).isEqualTo(userResponseDto);
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/new_avatar.png");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileShouldThrowBadRequestExceptionWhenUsernameIsAlreadyTaken() {
        UpdateProfileRequest request = new UpdateProfileRequest("new_username");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new_username")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Имя пользователя 'new_username' уже занято");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfileShouldUpdateUsernameWhenUsernameIsSameAsCurrent() {
        UpdateProfileRequest request = new UpdateProfileRequest("john_doe");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.updateProfile(1L, request);

        assertThat(result).isEqualTo(userResponseDto);
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileShouldUpdateUsernameWhenNewUsernameIsAvailable() {
        UpdateProfileRequest request = new UpdateProfileRequest("new_john");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new_john")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.updateProfile(1L, request);

        assertThat(result).isEqualTo(userResponseDto);
        assertThat(user.getUsername()).isEqualTo("new_john");
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenCurrentPasswordIsIncorrect() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrong_pass", "new_pass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_pass", "encoded_old_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Неверный текущий пароль");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenNewPasswordMatchesOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("old_pass", "old_pass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_pass", "encoded_old_password")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Новый пароль не должен совпадать со старым паролем");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordShouldSuccessfullyChangePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("old_pass", "new_pass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_pass", "encoded_old_password")).thenReturn(true);
        when(passwordEncoder.matches("new_pass", "encoded_old_password")).thenReturn(false);
        when(passwordEncoder.encode("new_pass")).thenReturn("encoded_new_password");

        userService.changePassword(1L, request);

        assertThat(user.getPassword()).isEqualTo("encoded_new_password");
        verify(userRepository).save(user);
    }
}