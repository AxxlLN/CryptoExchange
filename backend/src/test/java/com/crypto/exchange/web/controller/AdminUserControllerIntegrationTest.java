package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.AdminUserService;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    private UserResponseDto createTestUserDto(Long id, String username) {
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
    @DisplayName("Должен возвращать 401 Unauthorized при запросе без аутентификации")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Должен возвращать 403 Forbidden для пользователя с ролью USER")
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/users - должен возвращать 200 OK и список пользователей")
    void getAllUsersShouldReturn200AndUserList() throws Exception {
        List<UserResponseDto> users = List.of(
                createTestUserDto(1L, "john_doe"),
                createTestUserDto(2L, "alice_smith")
        );

        when(adminUserService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("john_doe"))
                .andExpect(jsonPath("$[0].avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$[0].isBlocked").value(false))
                .andExpect(jsonPath("$[0].isDeleted").value(false))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("alice_smith"));

        verify(adminUserService, times(1)).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/users/{id} - должен возвращать 200 OK и профиль пользователя")
    void getUserByIdShouldReturn200AndUserDto() throws Exception {
        Long userId = 1L;
        UserResponseDto userDto = createTestUserDto(userId, "john_doe");

        when(adminUserService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john_doe@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.isBlocked").value(false))
                .andExpect(jsonPath("$.isDeleted").value(false));

        verify(adminUserService, times(1)).getUserById(userId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/v1/admin/users/{username}/block - должен заблокировать пользователя и вернуть 204 No Content")
    void blockUserShouldReturn204NoContent() throws Exception {
        String username = "john_doe";

        doNothing().when(adminUserService).blockUserByUsername(username);

        mockMvc.perform(patch("/api/v1/admin/users/{username}/block", username))
                .andExpect(status().isNoContent());

        verify(adminUserService, times(1)).blockUserByUsername(username);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/v1/admin/users/{username}/unblock - должен разблокировать пользователя и вернуть 204 No Content")
    void unblockUserShouldReturn204NoContent() throws Exception {
        String username = "john_doe";

        doNothing().when(adminUserService).unblockUserByUsername(username);

        mockMvc.perform(patch("/api/v1/admin/users/{username}/unblock", username))
                .andExpect(status().isNoContent());

        verify(adminUserService, times(1)).unblockUserByUsername(username);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/admin/users/{id} - должен успешно удалить пользователя и вернуть 204 No Content")
    void deleteUserShouldReturn204NoContent() throws Exception {
        Long userId = 1L;

        doNothing().when(adminUserService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(adminUserService, times(1)).deleteUser(userId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/users/{id} - должен возвращать 404 Not Found, если пользователь не найден")
    void getUserByIdShouldReturn404WhenNotFound() throws Exception {
        Long nonExistingId = 999L;

        when(adminUserService.getUserById(nonExistingId))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + nonExistingId));

        mockMvc.perform(get("/api/v1/admin/users/{id}", nonExistingId))
                .andExpect(status().isNotFound());

        verify(adminUserService, times(1)).getUserById(nonExistingId);
    }
}