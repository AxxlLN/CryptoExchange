package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.UserService;
import com.crypto.exchange.web.dto.request.ChangePasswordRequest;
import com.crypto.exchange.web.dto.request.UpdateAvatarRequest;
import com.crypto.exchange.web.dto.request.UpdateProfileRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import com.crypto.exchange.web.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    private UserPrincipal createTestUserPrincipal(Long id, String username) {
        return new UserPrincipal(
                id,
                username,
                username + "@example.com",
                "password123",
                Role.ROLE_USER,
                false,
                false
        );
    }

    private UserResponseDto createSampleUserResponseDto(Long id, String username) {
        return new UserResponseDto(
                id,
                username,
                username + "@example.com",
                "https://example.com/avatar.png",
                Role.ROLE_USER,
                false,
                false,
                OffsetDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("GET /api/v1/users/me - должен возвращать 200 OK и профиль текущего пользователя")
    void getMyProfileShouldReturn200AndProfile() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        UserResponseDto responseDto = createSampleUserResponseDto(userId, "john_doe");

        when(userService.getUserProfile(userId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john_doe@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.isBlocked").value(false))
                .andExpect(jsonPath("$.isDeleted").value(false));

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/me - должен возвращать 401 Unauthorized для неаутентифицированного запроса")
    void getMyProfileShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - должен возвращать 200 OK при успешном обновлении профиля")
    void updateProfileShouldReturn200AndUpdatedProfile() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        UpdateProfileRequest request = new UpdateProfileRequest("new_username");
        UserResponseDto responseDto = createSampleUserResponseDto(userId, "new_username");

        when(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_username"));

        verify(userService, times(1)).updateProfile(eq(userId), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/avatar - должен возвращать 200 OK при обновлении аватара")
    void updateAvatarShouldReturn200AndProfile() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        UpdateAvatarRequest request = new UpdateAvatarRequest("https://example.com/new-avatar.png");
        UserResponseDto responseDto = createSampleUserResponseDto(userId, "john_doe");

        when(userService.updateAvatar(eq(userId), any(UpdateAvatarRequest.class))).thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/users/me/avatar")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateAvatar(eq(userId), any(UpdateAvatarRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/password - должен возвращать 204 No Content при успешной смене пароля")
    void changePasswordShouldReturn204() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "NewPassword@123");

        doNothing().when(userService).changePassword(eq(userId), any(ChangePasswordRequest.class));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).changePassword(eq(userId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me - должен возвращать 204 No Content при удалении профиля")
    void deleteMyProfileShouldReturn204() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");

        doNothing().when(userService).deleteOwnProfile(userId);

        mockMvc.perform(delete("/api/v1/users/me")
                        .with(user(principal)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteOwnProfile(userId);
    }
}