package com.crypto.exchange.core.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleNotFoundExceptionShouldReturn404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Cryptocurrency not found"));
    }

    @Test
    void handleValidationExceptionsShouldReturn400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: must not be blank;"));
    }

    @Test
    void handleBadRequestExceptionShouldReturn400ForInsufficientFunds() throws Exception {
        mockMvc.perform(get("/test/insufficient-funds"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Недостаточно средств на балансе"));
    }

    @Test
    void handleBadRequestExceptionShouldReturn400ForMissingParam() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void handleBadRequestExceptionShouldReturn400ForTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type-mismatch").param("id", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void handleHttpMessageNotReadableShouldReturn400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json {"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Некорректный формат JSON или тела запроса"));
    }

    @Test
    void handleBadCredentialsShouldReturn401() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Неверное имя пользователя или пароль"));
    }

    @Test
    void handleAuthenticationExceptionShouldReturn401() throws Exception {
        mockMvc.perform(get("/test/authentication-error"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Неверные учетные данные или невалидный токен"));
    }

    @Test
    void handleUserAlreadyExistsExceptionShouldReturn409() throws Exception {
        mockMvc.perform(get("/test/user-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Пользователь с таким email уже существует"));
    }

    @Test
    void handleAccessDeniedExceptionShouldReturn403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    @Test
    void handleMethodNotAllowedShouldReturn405() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void handleInternalServerErrorShouldReturn500() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Произошла внутренняя ошибка сервера"));
    }

    /**
     * Вспомогательный контроллер для генерации исключений в тестах
     */
    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Cryptocurrency not found");
        }

        @PostMapping("/test/validation")
        public void validateDto(@Valid @RequestBody TestDto dto) {
        }

        @GetMapping("/test/bad-credentials")
        public void throwBadCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/test/authentication-error")
        public void throwAuthenticationException() {
            throw new InsufficientAuthenticationException("Token expired");
        }

        @GetMapping("/test/insufficient-funds")
        public void throwInsufficientFunds() {
            throw new InsufficientFundsException("Недостаточно средств на балансе");
        }

        @GetMapping("/test/user-exists")
        public void throwUserAlreadyExists() {
            throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
        }

        @GetMapping("/test/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/test/missing-param")
        public void throwMissingParam(@RequestParam("requiredParam") String param) {
        }

        @GetMapping("/test/type-mismatch")
        public void throwTypeMismatch(@RequestParam("id") Long id) {
        }

        @GetMapping("/test/internal-error")
        public void throwInternalError() {
            throw new IllegalStateException("Unexpected internal state");
        }
    }

    @Getter
    @Setter
    static class TestDto {
        @NotBlank(message = "must not be blank")
        private String title;
    }
}