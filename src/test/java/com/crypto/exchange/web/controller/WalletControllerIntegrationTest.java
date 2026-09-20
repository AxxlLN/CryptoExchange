package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.WalletService;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletBalanceResponseDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private WalletResponseDto createSampleWalletDto(String address, String name) {
        WalletBalanceResponseDto balanceDto = new WalletBalanceResponseDto(
                1L,
                1L,
                "bitcoin",
                "BTC",
                "Bitcoin",
                new BigDecimal("2.5"),
                new BigDecimal("65000.00"),
                new BigDecimal("162500.00")
        );

        return new WalletResponseDto(
                1L,
                address,
                name,
                true,
                List.of(balanceDto)
        );
    }

    @Test
    @DisplayName("POST /api/v1/wallets - должен возвращать 201 Created при успешном создании кошелька")
    void createWalletShouldReturn201() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        CreateWalletRequestDto request = new CreateWalletRequestDto("Main Wallet", true);
        WalletResponseDto responseDto = createSampleWalletDto("0xWalletAddress123", "Main Wallet");

        when(walletService.createWallet(eq(userId), any(CreateWalletRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/wallets")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").value("0xWalletAddress123"))
                .andExpect(jsonPath("$.name").value("Main Wallet"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.balances.length()").value(1))
                .andExpect(jsonPath("$.balances[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.balances[0].amount").value(2.5))
                .andExpect(jsonPath("$.balances[0].totalValueUsd").value(162500.00));

        verify(walletService, times(1)).createWallet(eq(userId), any(CreateWalletRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/wallets - должен возвращать 401 Unauthorized для неаутентифицированного запроса")
    void createWalletShouldReturn401WhenUnauthenticated() throws Exception {
        CreateWalletRequestDto request = new CreateWalletRequestDto("Main Wallet", true);

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("GET /api/v1/wallets - должен возвращать 200 OK и список кошельков пользователя")
    void getMyWalletsShouldReturn200AndList() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        List<WalletResponseDto> wallets = List.of(
                createSampleWalletDto("0xWallet1", "Wallet 1"),
                createSampleWalletDto("0xWallet2", "Wallet 2")
        );

        when(walletService.getUserWallets(userId)).thenReturn(wallets);

        mockMvc.perform(get("/api/v1/wallets")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].address").value("0xWallet1"))
                .andExpect(jsonPath("$[1].address").value("0xWallet2"));

        verify(walletService, times(1)).getUserWallets(userId);
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{address} - должен возвращать 200 OK и кошелек по адресу")
    void getWalletByAddressShouldReturn200() throws Exception {
        Long userId = 1L;
        String address = "0xWallet123";
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        WalletResponseDto responseDto = createSampleWalletDto(address, "My Wallet");

        when(walletService.getWalletByAddressAndUser(userId, address)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/wallets/{address}", address)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(address))
                .andExpect(jsonPath("$.name").value("My Wallet"));

        verify(walletService, times(1)).getWalletByAddressAndUser(userId, address);
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{address} - должен возвращать 404 Not Found, если кошелек не найден")
    void getWalletByAddressShouldReturn404WhenNotFound() throws Exception {
        Long userId = 1L;
        String address = "0xUnknownWallet";
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");

        when(walletService.getWalletByAddressAndUser(userId, address))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        mockMvc.perform(get("/api/v1/wallets/{address}", address)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(walletService, times(1)).getWalletByAddressAndUser(userId, address);
    }

    @Test
    @DisplayName("POST /api/v1/wallets/{address}/deposit - должен возвращать 200 OK при успешном депозите")
    void depositShouldReturn200() throws Exception {
        Long userId = 1L;
        String address = "0xWallet123";
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("1.0"));
        WalletResponseDto responseDto = createSampleWalletDto(address, "Main Wallet");

        when(walletService.deposit(eq(userId), eq(address), any(BalanceOperationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/wallets/{address}/deposit", address)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(address));

        verify(walletService, times(1)).deposit(eq(userId), eq(address), any(BalanceOperationRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/wallets/{address}/withdraw - должен возвращать 200 OK при успешном выводе")
    void withdrawShouldReturn200() throws Exception {
        Long userId = 1L;
        String address = "0xWallet123";
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("0.5"));
        WalletResponseDto responseDto = createSampleWalletDto(address, "Main Wallet");

        when(walletService.withdraw(eq(userId), eq(address), any(BalanceOperationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/wallets/{address}/withdraw", address)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(address));

        verify(walletService, times(1)).withdraw(eq(userId), eq(address), any(BalanceOperationRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/balances/summary - должен возвращать 200 OK и агрегированные балансы")
    void getAggregatedBalancesShouldReturn200() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        AggregatedBalanceDto aggregatedDto = new AggregatedBalanceDto(
                1L,
                "BTC",
                "Bitcoin",
                new BigDecimal("3.5")
        );

        when(walletService.getAggregatedBalances(userId)).thenReturn(List.of(aggregatedDto));

        mockMvc.perform(get("/api/v1/wallets/balances/summary")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cryptoId").value(1))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].totalAmount").value(3.5));

        verify(walletService, times(1)).getAggregatedBalances(userId);
    }
}