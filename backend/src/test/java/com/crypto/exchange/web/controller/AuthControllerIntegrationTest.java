package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.exception.UserAlreadyExistsException;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.AuthService;
import com.crypto.exchange.web.dto.request.LoginRequest;
import com.crypto.exchange.web.dto.request.RegisterRequest;
import com.crypto.exchange.web.dto.response.AuthResponseDto;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    private AuthResponseDto createAuthResponseDto(String username, String email) {
        UserResponseDto userDto = new UserResponseDto(
                1L,
                username,
                email,
                "https://example.com/avatar.png",
                Role.ROLE_USER,
                false,
                false,
                OffsetDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        );
        return new AuthResponseDto("mocked-jwt-token-xyz", "Bearer", userDto);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - должен возвращать 201 Created при успешной регистрации")
    void registerShouldReturn201Created() throws Exception {
        String username = "john_doe";
        String email = "john@example.com";
        RegisterRequest request = new RegisterRequest(username, email, "Password123!");
        AuthResponseDto responseDto = createAuthResponseDto(username, email);

        when(authService.register(any(RegisterRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-xyz"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(email));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - должен возвращать 400 Bad Request при некорректных входных данных")
    void registerShouldReturn400WhenValidationFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "invalid-email", "123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - должен возвращать 409 Conflict, если пользователь уже существует")
    void registerShouldReturn409WhenUserAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "Password123!");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("User with this username already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - должен возвращать 200 OK при успешной аутентификации")
    void loginShouldReturn200Ok() throws Exception {
        String username = "john_doe";
        String email = "john_doe@example.com";
        LoginRequest request = new LoginRequest(username, "Password123!");
        AuthResponseDto responseDto = createAuthResponseDto(username, email);

        when(authService.login(any(LoginRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-xyz"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(email));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - должен возвращать 401 Unauthorized при неверном логине или пароле")
    void loginShouldReturn401WhenBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("john_doe", "WrongPassword!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - должен возвращать 403 Forbidden, если аккаунт заблокирован или удален")
    void loginShouldReturn403WhenAccountIsBlockedOrDeleted() throws Exception {
        LoginRequest request = new LoginRequest("blocked_user", "Password123!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccessDeniedException("Account is blocked"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }
}