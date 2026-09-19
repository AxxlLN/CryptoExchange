package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.service.TransferService;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import com.crypto.exchange.web.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void transfer_shouldSucceed_whenValidRequestProvided() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = new UserPrincipal(
                userId, "testuser", "test@mail.com", "password", Role.ROLE_USER
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        TransferRequestDto requestDto = new TransferRequestDto(
                10L,
                "0x123456789abcdef",
                "bitcoin",
                new BigDecimal("0.5")
        );

        TransferResponseDto responseDto = new TransferResponseDto(
                "0xSenderAddress",
                "0x123456789abcdef",
                "BTC",
                new BigDecimal("0.5"),
                OffsetDateTime.now(ZoneOffset.UTC),
                "SUCCESS"
        );

        given(transferService.transfer(eq(userId), any(TransferRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderAddress").value("0xSenderAddress"))
                .andExpect(jsonPath("$.recipientAddress").value("0x123456789abcdef"))
                .andExpect(jsonPath("$.cryptoSymbol").value("BTC"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void transfer_shouldFail_whenValidationFails() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                1L, "testuser", "test@mail.com", "password", Role.ROLE_USER
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        TransferRequestDto invalidRequest = new TransferRequestDto(
                null,
                "",
                "",
                new BigDecimal("0.00")
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_shouldReturnForbidden_whenUnauthenticated() throws Exception {
        TransferRequestDto requestDto = new TransferRequestDto(
                10L,
                "0x123456789abcdef",
                "bitcoin",
                new BigDecimal("0.5")
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }
}