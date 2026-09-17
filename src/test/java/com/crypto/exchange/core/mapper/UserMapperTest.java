package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.web.dto.request.RegisterRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .createdAt(OffsetDateTime.now())
                .build();

        registerRequest = new RegisterRequest(
                "jane_doe",
                "jane@example.com",
                "secretPassword123"
        );
    }

    @Nested
    @DisplayName("Маппинг User -> UserResponseDto")
    class UserToResponseDtoTests {

        @Test
        @DisplayName("Успешно маппит сущность User в UserResponseDto")
        void toResponseDto_Success() {
            UserResponseDto dto = userMapper.toResponseDto(testUser);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.username()).isEqualTo("john_doe");
            assertThat(dto.email()).isEqualTo("john@example.com");
            assertThat(dto.role()).isEqualTo(Role.ROLE_USER);
            assertThat(dto.createdAt()).isEqualTo(testUser.getCreatedAt());
        }

        @Test
        @DisplayName("Возвращает null при передаче null сущности User")
        void toResponseDto_NullEntity_ReturnsNull() {
            assertThat(userMapper.toResponseDto(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Маппинг RegisterRequest -> User")
    class RegisterRequestToUserTests {

        @Test
        @DisplayName("Успешно маппит RegisterRequest в User с игнорированием системных полей")
        void toEntity_Success() {
            User entity = userMapper.toEntity(registerRequest);

            assertThat(entity).isNotNull();
            assertThat(entity.getUsername()).isEqualTo("jane_doe");
            assertThat(entity.getEmail()).isEqualTo("jane@example.com");

            assertThat(entity.getId()).isNull();
            assertThat(entity.getPassword()).isNull();
            assertThat(entity.getRole()).isNull();
            assertThat(entity.getCreatedAt()).isNull();

            assertThat(entity.getWallets()).isEmpty();
            assertThat(entity.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Возвращает null при передаче null DTO")
        void toEntity_NullRequest_ReturnsNull() {
            assertThat(userMapper.toEntity(null)).isNull();
        }
    }
}