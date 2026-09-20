package com.crypto.exchange.web.security;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("Фабричный метод create должен корректно мапить все поля из сущности User")
    void createShouldMapAllFieldsFromUserEntity() {
        User user = User.builder()
                .id(42L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .blocked(false)
                .deleted(false)
                .build();

        UserPrincipal principal = UserPrincipal.create(user);

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("john_doe");
        assertThat(principal.getEmail()).isEqualTo("john@example.com");
        assertThat(principal.getPassword()).isEqualTo("encoded_password");
        assertThat(principal.isBlocked()).isFalse();
        assertThat(principal.isDeleted()).isFalse();

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("isAccountNonExpired и isCredentialsNonExpired должны всегда возвращать true")
    void expirationChecksShouldAlwaysReturnTrue() {
        UserPrincipal principal = new UserPrincipal(
                1L, "user", "user@test.com", "pass", Role.ROLE_USER, false, false
        );

        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "false, true",
            "true, false"
    })
    @DisplayName("isAccountNonLocked должен возвращать инвертированное значение флага blocked")
    void isAccountNonLockedShouldDependOnBlockedStatus(boolean blocked, boolean expectedNonLocked) {
        UserPrincipal principal = new UserPrincipal(
                1L, "user", "user@test.com", "pass", Role.ROLE_USER, blocked, false
        );

        assertThat(principal.isAccountNonLocked()).isEqualTo(expectedNonLocked);
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, true",
            "true, false, false",
            "false, true, false",
            "true, true, false"
    })
    @DisplayName("isEnabled должен возвращать true только если аккаунт не заблокирован и не удален")
    void isEnabledShouldDependOnBlockedAndDeletedStatus(boolean blocked, boolean deleted, boolean expectedEnabled) {
        UserPrincipal principal = new UserPrincipal(
                1L, "user", "user@test.com", "pass", Role.ROLE_USER, blocked, deleted
        );

        assertThat(principal.isEnabled()).isEqualTo(expectedEnabled);
    }
}