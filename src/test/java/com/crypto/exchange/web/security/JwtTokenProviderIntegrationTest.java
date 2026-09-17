package com.crypto.exchange.web.security;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class JwtTokenProviderIntegrationTest {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void generateAndValidateToken_shouldUsePropertiesSuccessfully() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test_user");
        user.setRole(Role.ROLE_USER);

        String token = tokenProvider.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("test_user");
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(1L);
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ROLE_USER");
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() {
        boolean isValid = tokenProvider.validateToken("invalid.jwt.token.string");
        assertThat(isValid).isFalse();
    }
}