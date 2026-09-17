package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.exception.UserAlreadyExistsException;
import com.crypto.exchange.core.mapper.UserMapper;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.web.dto.request.LoginRequest;
import com.crypto.exchange.web.dto.request.RegisterRequest;
import com.crypto.exchange.web.dto.response.AuthResponseDto;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User userEntity;
    private UserResponseDto userResponseDto;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "john_doe",
                "john@example.com",
                "securePassword123"
        );

        loginRequest = new LoginRequest(
                "john_doe",
                "securePassword123"
        );

        userEntity = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encodedPassword")
                .role(Role.ROLE_USER)
                .build();

        userResponseDto = new UserResponseDto(
                1L,
                "john_doe",
                "john@example.com",
                Role.ROLE_USER,
                null
        );

        jwtToken = "mock.jwt.token.string";
    }

    @Nested
    @DisplayName("Регистрация пользователя (register)")
    class RegisterTests {

        @Test
        @DisplayName("Успешная регистрация нового пользователя")
        void register_Success() {
            when(userRepository.existsByUsername("john_doe")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(registerRequest)).thenReturn(userEntity);
            when(passwordEncoder.encode("securePassword123")).thenReturn("encodedPassword");
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(tokenProvider.generateToken(userEntity)).thenReturn(jwtToken);
            when(userMapper.toResponseDto(userEntity)).thenReturn(userResponseDto);

            AuthResponseDto response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(jwtToken);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isEqualTo(userResponseDto);

            verify(userRepository).save(userEntity);
            verify(passwordEncoder).encode("securePassword123");
            verify(tokenProvider).generateToken(userEntity);
        }

        @Test
        @DisplayName("Выбрасывает UserAlreadyExistsException, если username уже занят")
        void register_UsernameAlreadyExists_ThrowsException() {
            when(userRepository.existsByUsername("john_doe")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already in use");

            verify(userRepository, never()).existsByEmail(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Выбрасывает UserAlreadyExistsException, если email уже занят")
        void register_EmailAlreadyExists_ThrowsException() {
            when(userRepository.existsByUsername("john_doe")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already in use");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Вход в систему (login)")
    class LoginTests {

        @Test
        @DisplayName("Успешная аутентификация и генерация токена")
        void login_Success() {
            when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(userEntity));
            when(tokenProvider.generateToken(userEntity)).thenReturn(jwtToken);
            when(userMapper.toResponseDto(userEntity)).thenReturn(userResponseDto);

            AuthResponseDto response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(jwtToken);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isEqualTo(userResponseDto);

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
            );
            verify(userRepository).findByUsernameOrEmail("john_doe");
            verify(tokenProvider).generateToken(userEntity);
        }

        @Test
        @DisplayName("Выбрасывает исключение, если пользователь не найден в БД после успешной аутентификации")
        void login_UserNotFoundAfterAuth_ThrowsException() {
            when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(java.util.NoSuchElementException.class);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider, never()).generateToken(any());
        }
    }
}