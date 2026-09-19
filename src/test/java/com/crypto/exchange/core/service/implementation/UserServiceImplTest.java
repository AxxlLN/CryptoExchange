package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.UserMapper;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponseDto expectedDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        expectedDto = new UserResponseDto(
                1L,
                "john_doe",
                "john@example.com",
                Role.ROLE_USER,
                testUser.getCreatedAt()
        );
    }

    @Nested
    @DisplayName("Получение профиля пользователя (getUserProfile)")
    class GetUserProfileTests {

        @Test
        @DisplayName("Успешно возвращает профиль существующего пользователя")
        void getUserProfileSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponseDto(testUser)).thenReturn(expectedDto);

            UserResponseDto result = userService.getUserProfile(1L);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedDto);
            verify(userRepository).findById(1L);
            verify(userMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если пользователь не найден")
        void getUserProfileNotFoundThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь с ID 1 не найден");

            verify(userMapper, never()).toResponseDto(any());
        }
    }

    @Nested
    @DisplayName("Получение списка всех пользователей (getAllUsers)")
    class GetAllUsersTests {

        @Test
        @DisplayName("Возвращает список DTO всех пользователей")
        void getAllUsersSuccess() {
            when(userRepository.findAll()).thenReturn(List.of(testUser));
            when(userMapper.toResponseDto(testUser)).thenReturn(expectedDto);

            List<UserResponseDto> result = userService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(expectedDto);
            verify(userRepository).findAll();
            verify(userMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Возвращает пустой список, если пользователей нет в системе")
        void getAllUsersEmptyList() {
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            List<UserResponseDto> result = userService.getAllUsers();

            assertThat(result).isEmpty();
            verify(userRepository).findAll();
            verify(userMapper, never()).toResponseDto(any());
        }
    }

    @Nested
    @DisplayName("Получение пользователя по ID (getUserById)")
    class GetUserByIdTests {

        @Test
        @DisplayName("Успешно возвращает пользователя по ID")
        void getUserByIdSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponseDto(testUser)).thenReturn(expectedDto);

            UserResponseDto result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedDto);
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если пользователь с таким ID не существует")
        void getUserByIdNotFoundThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь с ID 1 не найден");
        }
    }

    @Nested
    @DisplayName("Удаление пользователя (deleteUser)")
    class DeleteUserTests {

        @Test
        @DisplayName("Успешно удаляет существующего пользователя")
        void deleteUserSuccess() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).existsById(1L);
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException при попытке удалить несуществующего пользователя")
        void deleteUserNotFoundThrowsException() {
            when(userRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь с ID 1 не найден");

            verify(userRepository, never()).deleteById(anyLong());
        }
    }
}