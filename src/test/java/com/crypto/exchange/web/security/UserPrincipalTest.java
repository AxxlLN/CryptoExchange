package com.crypto.exchange.web.security;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Nested
    @DisplayName("Создание объекта и маппинг")
    class CreationTests {

        @Test
        @DisplayName("Фабричный метод create() корректно маппит все поля из User")
        void createMapsAllFieldsFromUser() {
            User user = User.builder()
                    .id(42L)
                    .username("satoshi")
                    .email("satoshi@nakamoto.org")
                    .password("secret_hash")
                    .role(Role.ROLE_ADMIN)
                    .build();

            UserPrincipal principal = UserPrincipal.create(user);

            assertThat(principal.getId()).isEqualTo(42L);
            assertThat(principal.getUsername()).isEqualTo("satoshi");
            assertThat(principal.getEmail()).isEqualTo("satoshi@nakamoto.org");
            assertThat(principal.getPassword()).isEqualTo("secret_hash");
            assertThat(principal.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Конструктор напрямую правильно инициализирует поля и роли")
        void constructorInitializesFieldsCorrectly() {
            UserPrincipal principal = new UserPrincipal(
                    10L,
                    "trader",
                    "trader@crypto.com",
                    "password123",
                    Role.ROLE_USER
            );

            assertThat(principal.getId()).isEqualTo(10L);
            assertThat(principal.getUsername()).isEqualTo("trader");
            assertThat(principal.getEmail()).isEqualTo("trader@crypto.com");
            assertThat(principal.getPassword()).isEqualTo("password123");
            assertThat(principal.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Флаги состояния аккаунта Spring Security")
    class AccountStatusFlagsTests {

        @Test
        @DisplayName("Все флаги активности аккаунта возвращают true")
        void accountStatusFlagsReturnTrue() {
            UserPrincipal principal = new UserPrincipal(1L, "user", "user@mail.com", "pass", Role.ROLE_USER);

            assertThat(principal.isAccountNonExpired()).isTrue();
            assertThat(principal.isAccountNonLocked()).isTrue();
            assertThat(principal.isCredentialsNonExpired()).isTrue();
            assertThat(principal.isEnabled()).isTrue();
        }
    }
}