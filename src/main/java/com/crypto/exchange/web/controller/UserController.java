package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.UserService;
import com.crypto.exchange.web.dto.request.ChangePasswordRequest;
import com.crypto.exchange.web.dto.request.UpdateAvatarRequest;
import com.crypto.exchange.web.dto.request.UpdateProfileRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import com.crypto.exchange.web.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Управление собственным профилем пользователя")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Получить профиль текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<UserResponseDto> getMyProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getUserProfile(currentUser.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить имя пользователя (username)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Имя пользователя уже занято или некорректно"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<UserResponseDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getId(), request));
    }

    @PatchMapping("/me/avatar")
    @Operation(summary = "Обновить аватар пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аватар успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректный URL аватара"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<UserResponseDto> updateAvatar(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateAvatarRequest request
    ) {
        return ResponseEntity.ok(userService.updateAvatar(currentUser.getId(), request));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Сменить пароль пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пароль успешно изменен"),
            @ApiResponse(responseCode = "400", description = "Неверный текущий пароль или невалидный новый пароль"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @Operation(summary = "Удалить собственный профиль (Soft Delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Профиль успешно удален"),
            @ApiResponse(responseCode = "400", description = "Нельзя удалить профиль с ненулевыми балансами"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        userService.deleteOwnProfile(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}