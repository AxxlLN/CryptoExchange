package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.service.WalletService;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private UsernamePasswordAuthenticationToken createAuthToken() {
        UserPrincipal principal = new UserPrincipal(
                1L, "testuser", "test@mail.com", "password", Role.ROLE_USER
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void createWalletShouldReturnCreatedWhenRequestIsValid() throws Exception {
        CreateWalletRequestDto requestDto = new CreateWalletRequestDto("Main Wallet", true);
        WalletResponseDto responseDto = new WalletResponseDto(1L, "0xAddress123", "Main Wallet", true, List.of());

        given(walletService.createWallet(eq(1L), any(CreateWalletRequestDto.class))).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/wallets")
                        .with(authentication(createAuthToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.address").value("0xAddress123"))
                .andExpect(jsonPath("$.name").value("Main Wallet"));
    }

    @Test
    void getMyWalletsShouldReturnListWhenAuthenticated() throws Exception {
        WalletResponseDto responseDto = new WalletResponseDto(1L, "0xAddress123", "Main Wallet", true, List.of());

        given(walletService.getUserWallets(1L)).willReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/wallets")
                        .with(authentication(createAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Main Wallet"));
    }

    @Test
    void getWalletByIdShouldReturnWalletWhenExists() throws Exception {
        Long walletId = 1L;
        WalletResponseDto responseDto = new WalletResponseDto(walletId, "0xAddress123", "Main Wallet", true, List.of());

        given(walletService.getWalletById(1L, walletId)).willReturn(responseDto);

        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId)
                        .with(authentication(createAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId));
    }

    @Test
    void depositShouldReturnUpdatedWalletWhenRequestIsValid() throws Exception {
        Long walletId = 1L;
        BalanceOperationRequestDto requestDto = new BalanceOperationRequestDto("bitcoin", new BigDecimal("1.5"));
        WalletResponseDto responseDto = new WalletResponseDto(walletId, "0xAddress123", "Main Wallet", true, List.of());

        given(walletService.deposit(eq(1L), eq(walletId), any(BalanceOperationRequestDto.class))).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
                        .with(authentication(createAuthToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId));
    }

    @Test
    void withdrawShouldReturnUpdatedWalletWhenRequestIsValid() throws Exception {
        Long walletId = 1L;
        BalanceOperationRequestDto requestDto = new BalanceOperationRequestDto("bitcoin", new BigDecimal("0.5"));
        WalletResponseDto responseDto = new WalletResponseDto(walletId, "0xAddress123", "Main Wallet", true, List.of());

        given(walletService.withdraw(eq(1L), eq(walletId), any(BalanceOperationRequestDto.class))).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/wallets/{walletId}/withdraw", walletId)
                        .with(authentication(createAuthToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId));
    }

    @Test
    void getWalletByAddressShouldReturnWalletWhenAddressExists() throws Exception {
        String address = "0xAddress123";
        WalletResponseDto responseDto = new WalletResponseDto(1L, address, "Main Wallet", true, List.of());

        given(walletService.getWalletByAddress(address)).willReturn(responseDto);

        mockMvc.perform(get("/api/v1/wallets/by-address/{address}", address)
                        .with(authentication(createAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(address));
    }

    @Test
    void getAggregatedBalancesShouldReturnSummaryWhenAuthenticated() throws Exception {
        AggregatedBalanceDto balanceDto = new AggregatedBalanceDto(1L, "BTC", "Bitcoin", new BigDecimal("2.5"));

        given(walletService.getAggregatedBalances(1L)).willReturn(List.of(balanceDto));

        mockMvc.perform(get("/api/v1/wallets/balances/summary")
                        .with(authentication(createAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].totalAmount").value(2.5));
    }
}