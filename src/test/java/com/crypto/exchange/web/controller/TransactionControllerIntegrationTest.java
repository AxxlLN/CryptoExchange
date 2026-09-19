package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.service.TransactionService;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import com.crypto.exchange.web.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getUserTransactions_shouldReturnForbidden_whenUnauthenticated() throws Exception {
       mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserTransactions_shouldReturnPageOfTransactions_whenAuthenticated() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = new UserPrincipal(
                userId, "testuser", "test@mail.com", "password", Role.ROLE_USER
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        TransactionResponseDto dto = new TransactionResponseDto(
                100L, 10L, 20L, "BTC", "ETH",
                new BigDecimal("1.5"), new BigDecimal("25.0"),
                null, TransactionResponseDto.TransactionDirection.OUTGOING,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        Page<TransactionResponseDto> transactionPage = new PageImpl<>(List.of(dto));

        given(transactionService.getUserTransactions(eq(userId), any(Pageable.class)))
                .willReturn(transactionPage);

        mockMvc.perform(get("/api/v1/transactions")
                        .with(authentication(auth))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.content[0].fromCryptoSymbol").value("BTC"))
                .andExpect(jsonPath("$.content[0].toCryptoSymbol").value("ETH"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}