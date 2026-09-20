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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private User user;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("raw_password")
                .role(Role.ROLE_USER)
                .build();

        userResponseDto = new UserResponseDto(
                1L,
                "john_doe",
                "john@example.com",
                null,
                Role.ROLE_USER,
                false,
                false,
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("Тесты метода register")
    class RegisterTests {

        @Test
        void registerShouldThrowUserAlreadyExistsExceptionWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "password123");

            when(userRepository.existsByUsername("john_doe")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already in use");

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        void registerShouldThrowUserAlreadyExistsExceptionWhenEmailExists() {
            RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "password123");

            when(userRepository.existsByUsername("john_doe")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already in use");

            verify(userRepository, never()).save(any());
        }

        @Test
        void registerShouldSuccessfullyRegisterUserAndReturnAuthResponse() {
            RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "password123");

            when(userRepository.existsByUsername("john_doe")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(userRepository.save(user)).thenReturn(user);
            when(tokenProvider.generateToken(user)).thenReturn("mocked_jwt_token");
            when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

            AuthResponseDto response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("mocked_jwt_token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isEqualTo(userResponseDto);
            assertThat(user.getPassword()).isEqualTo("encoded_password");

            verify(userRepository).save(user);
            verify(tokenProvider).generateToken(user);
        }
    }

    @Nested
    @DisplayName("Тесты метода login")
    class LoginTests {

        @Test
        void loginShouldThrowExceptionWhenAuthenticationFails() {
            LoginRequest request = new LoginRequest("john_doe", "wrong_password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid credentials");

            verify(userRepository, never()).findByUsernameOrEmail(anyString());
        }

        @Test
        void loginShouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
            LoginRequest request = new LoginRequest("john_doe", "password123");

            when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(NoSuchElementException.class);

            verify(tokenProvider, never()).generateToken(any());
        }

        @Test
        void loginShouldSuccessfullyAuthenticateAndReturnAuthResponse() {
            LoginRequest request = new LoginRequest("john_doe", "password123");

            when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(user));
            when(tokenProvider.generateToken(user)).thenReturn("mocked_jwt_token");
            when(userMapper.toResponseDto(user)).thenReturn(userResponseDto);

            AuthResponseDto response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("mocked_jwt_token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user()).isEqualTo(userResponseDto);

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken("john_doe", "password123")
            );
            verify(tokenProvider).generateToken(user);
        }
    }
}