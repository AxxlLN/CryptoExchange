package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.exception.InsufficientFundsException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.TransferService;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

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

    private TransferRequestDto createSampleTransferRequest() {
        return new TransferRequestDto(
                "0xSenderWalletAddress",
                "0xRecipientWalletAddress",
                "bitcoin",
                new BigDecimal("1.5")
        );
    }

    private TransferResponseDto createSampleTransferResponse() {
        return new TransferResponseDto(
                500L,
                "0xSenderWalletAddress",
                "0xRecipientWalletAddress",
                "BTC",
                new BigDecimal("1.5"),
                OffsetDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC),
                TransactionStatus.SUCCESS
        );
    }

    @Test
    @DisplayName("POST /api/v1/transfers - должен возвращать 201 Created при успешном P2P переводе")
    void transferShouldReturn201Created() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        TransferRequestDto request = createSampleTransferRequest();
        TransferResponseDto response = createSampleTransferResponse();

        when(transferService.transfer(eq(userId), any(TransferRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(500))
                .andExpect(jsonPath("$.senderAddress").value("0xSenderWalletAddress"))
                .andExpect(jsonPath("$.recipientAddress").value("0xRecipientWalletAddress"))
                .andExpect(jsonPath("$.cryptoSymbol").value("BTC"))
                .andExpect(jsonPath("$.amount").value(1.5))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(transferService, times(1)).transfer(eq(userId), any(TransferRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - должен возвращать 400 Bad Request при невалидных входных данных")
    void transferShouldReturn400WhenValidationFails() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");

        TransferRequestDto invalidRequest = new TransferRequestDto(
                null,
                "",
                "",
                new BigDecimal("0.00000000")
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    @Test
    @DisplayName("POST /api/v1/transfers - должен возвращать 401 Unauthorized для неаутентифицированного запроса")
    void transferShouldReturn401WhenUnauthenticated() throws Exception {
        TransferRequestDto request = createSampleTransferRequest();

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transferService);
    }

    @Test
    @DisplayName("POST /api/v1/transfers - должен возвращать 404 Not Found, если кошелек не найден")
    void transferShouldReturn404WhenWalletNotFound() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        TransferRequestDto request = createSampleTransferRequest();

        when(transferService.transfer(eq(userId), any(TransferRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Target wallet not found"));

        mockMvc.perform(post("/api/v1/transfers")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(transferService, times(1)).transfer(eq(userId), any(TransferRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - должен возвращать 400 Bad Request при недостатке средств")
    void transferShouldReturn400WhenInsufficientFunds() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        TransferRequestDto request = createSampleTransferRequest();

        when(transferService.transfer(eq(userId), any(TransferRequestDto.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds for transfer"));

        mockMvc.perform(post("/api/v1/transfers")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferService, times(1)).transfer(eq(userId), any(TransferRequestDto.class));
    }
}