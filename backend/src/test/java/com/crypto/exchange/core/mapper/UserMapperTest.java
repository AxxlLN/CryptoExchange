package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.web.dto.request.RegisterRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toResponseDtoShouldMapAllFieldsCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        User entity = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .avatarUrl("https://example.com/avatar.png")
                .role(Role.ROLE_USER)
                .blocked(false)
                .deleted(false)
                .createdAt(now)
                .build();

        UserResponseDto dto = userMapper.toResponseDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.username()).isEqualTo("johndoe");
        assertThat(dto.email()).isEqualTo("john@example.com");
        assertThat(dto.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(dto.role()).isEqualTo(Role.ROLE_USER);
        assertThat(dto.blocked()).isFalse();
        assertThat(dto.deleted()).isFalse();
        assertThat(dto.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponseDtoShouldReturnNullWhenEntityIsNull() {
        UserResponseDto dto = userMapper.toResponseDto(null);

        assertThat(dto).isNull();
    }

    @Test
    void toEntityShouldMapRegisterRequestToUserIgnoringUnmappedFields() {
        RegisterRequest request = new RegisterRequest(
                "johndoe",
                "john@example.com",
                "secretPassword123"
        );

        User entity = userMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getUsername()).isEqualTo("johndoe");
        assertThat(entity.getEmail()).isEqualTo("john@example.com");

        assertThat(entity.getId()).isNull();
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getRole()).isNull();
        assertThat(entity.getAvatarUrl()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.isBlocked()).isFalse();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void toEntityShouldReturnNullWhenRequestIsNull() {
        User entity = userMapper.toEntity(null);

        assertThat(entity).isNull();
    }
}