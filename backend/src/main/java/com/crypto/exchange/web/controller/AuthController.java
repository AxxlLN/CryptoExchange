package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.AuthService;
import com.crypto.exchange.web.dto.request.LoginRequest;
import com.crypto.exchange.web.dto.request.RegisterRequest;
import com.crypto.exchange.web.dto.response.AuthResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация и аутентификация пользователей")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким username или email уже существует")
    })
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя и получение JWT-токена")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "401", description = "Неверный логин/email или пароль"),
            @ApiResponse(responseCode = "403", description = "Аккаунт заблокирован или удален")
    })
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}