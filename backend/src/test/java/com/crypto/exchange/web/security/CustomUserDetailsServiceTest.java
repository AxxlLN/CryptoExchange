package com.crypto.exchange.web.security;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("Метод loadUserByUsername")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Возвращает UserDetails, когда пользователь найден по username или email")
        void loadUserByUsernameSuccess() {
            String identifier = "john_doe";
            when(userRepository.findByUsernameOrEmail(identifier)).thenReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("john_doe");
            assertThat(userDetails.getPassword()).isEqualTo("encoded_password");
            assertThat(userDetails.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");

            verify(userRepository).findByUsernameOrEmail(identifier);
        }

        @Test
        @DisplayName("Выбрасывает UsernameNotFoundException, когда пользователь не найден")
        void loadUserByUsernameNotFoundThrowsException() {
            String identifier = "unknown_user";
            when(userRepository.findByUsernameOrEmail(identifier)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(identifier))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with username or email: " + identifier);

            verify(userRepository).findByUsernameOrEmail(identifier);
        }
    }
}