package com.crypto.exchange.web.security;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Успешная аутентификация при передаче валидного Bearer токена и активного пользователя")
    void doFilterInternalShouldSetAuthenticationWhenTokenAndUserAreValid() throws ServletException, IOException {
        String token = "valid_jwt_token";
        String username = "john_doe";

        User user = User.builder()
                .id(1L)
                .username(username)
                .email("john@example.com")
                .password("secret")
                .role(Role.ROLE_USER)
                .blocked(false)
                .deleted(false)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn(username);
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo(username);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Не должен устанавливать аутентификацию, если заголовок Authorization отсутствует")
    void doFilterInternalShouldNotSetAuthWhenHeaderIsMissing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(tokenProvider, userRepository);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Не должен устанавливать аутентификацию, если заголовок не начинается с 'Bearer '")
    void doFilterInternalShouldNotSetAuthWhenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(tokenProvider, userRepository);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Не должен устанавливать аутентификацию, если токен невалиден")
    void doFilterInternalShouldNotSetAuthWhenTokenIsInvalid() throws ServletException, IOException {
        String invalidToken = "invalid_jwt_token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenProvider.validateToken(invalidToken)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userRepository, never()).findByUsernameOrEmail(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Не должен устанавливать аутентификацию, если пользователь заблокирован")
    void doFilterInternalShouldNotSetAuthWhenUserIsBlocked() throws ServletException, IOException {
        String token = "valid_jwt_token";
        String username = "blocked_user";

        User blockedUser = User.builder()
                .id(2L)
                .username(username)
                .role(Role.ROLE_USER)
                .blocked(true)
                .deleted(false)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn(username);
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(blockedUser));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Не должен устанавливать аутентификацию, если пользователь не найден в базе")
    void doFilterInternalShouldNotSetAuthWhenUserNotFound() throws ServletException, IOException {
        String token = "valid_jwt_token";
        String username = "unknown_user";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn(username);
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }
}