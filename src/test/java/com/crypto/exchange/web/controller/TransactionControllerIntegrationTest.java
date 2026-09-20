package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.entity.Transaction.TransactionType;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.TransactionService;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import com.crypto.exchange.web.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

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
                false, // blocked
                false  // deleted
        );
    }

    private TransactionResponseDto createSampleTransactionDto(Long id) {
        return new TransactionResponseDto(
                id,
                "0xSenderAddress",
                "0xReceiverAddress",
                "BTC",
                "USDT",
                new BigDecimal("0.5"),
                new BigDecimal("32500.00"),
                TransactionType.EXCHANGE,
                TransactionStatus.SUCCESS,
                TransactionDirection.OUTGOING,
                OffsetDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("GET /api/v1/transactions - должен возвращать 200 OK и страницу с транзакциями пользователя")
    void getUserTransactionsShouldReturn200AndPage() throws Exception {
        Long userId = 1L;
        String username = "john_doe";
        UserPrincipal principal = createTestUserPrincipal(userId, username);

        TransactionResponseDto txDto = createSampleTransactionDto(100L);
        Page<TransactionResponseDto> pageResponse = new PageImpl<>(
                List.of(txDto),
                PageRequest.of(0, 10),
                1
        );

        when(transactionService.getUserTransactions(eq(userId), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/transactions")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].fromWalletAddress").value("0xSenderAddress"))
                .andExpect(jsonPath("$.content[0].toWalletAddress").value("0xReceiverAddress"))
                .andExpect(jsonPath("$.content[0].fromCryptoSymbol").value("BTC"))
                .andExpect(jsonPath("$.content[0].toCryptoSymbol").value("USDT"))
                .andExpect(jsonPath("$.content[0].fromAmount").value(0.5))
                .andExpect(jsonPath("$.content[0].toAmount").value(32500.00))
                .andExpect(jsonPath("$.content[0].type").value("EXCHANGE"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].direction").value("OUTGOING"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(transactionService, times(1)).getUserTransactions(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/transactions - должен возвращать 401 Unauthorized для неаутентифицированного запроса")
    void getUserTransactionsShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("GET /api/v1/transactions/export/pdf - должен возвращать 200 OK и PDF-файл выписки")
    void exportTransactionsToPdfShouldReturn200AndPdfFile() throws Exception {
        Long userId = 1L;
        String username = "john_doe";
        UserPrincipal principal = createTestUserPrincipal(userId, username);

        byte[] fakePdfBytes = "%PDF-1.4 Fake PDF Content".getBytes();

        when(transactionService.generatePdfReport(userId, username)).thenReturn(fakePdfBytes);

        mockMvc.perform(get("/api/v1/transactions/export/pdf")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_report.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(fakePdfBytes));

        verify(transactionService, times(1)).generatePdfReport(userId, username);
    }

    @Test
    @DisplayName("GET /api/v1/transactions/export/pdf - должен возвращать 401 Unauthorized для неаутентифицированного запроса")
    void exportTransactionsToPdfShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/export/pdf"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }
}