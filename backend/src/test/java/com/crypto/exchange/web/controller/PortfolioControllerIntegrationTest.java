package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.PortfolioService;
import com.crypto.exchange.web.dto.response.DetailedPortfolioDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import com.crypto.exchange.web.security.UserPrincipal;
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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@Import(SecurityConfig.class)
class PortfolioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

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

    private DetailedPortfolioDto createSamplePortfolioDto() {
        DetailedPortfolioDto.CryptoAssetBalanceDto asset = new DetailedPortfolioDto.CryptoAssetBalanceDto(
                1L,
                "BTC",
                "Bitcoin",
                new BigDecimal("0.5"),
                new BigDecimal("65000.00"),
                new BigDecimal("32500.00"),
                new BigDecimal("100.00")
        );

        DetailedPortfolioDto.PortfolioAnalyticsDto analytics = new DetailedPortfolioDto.PortfolioAnalyticsDto(
                new BigDecimal("1200.50"),
                new BigDecimal("3.83"),
                new BigDecimal("2500.00"),
                new BigDecimal("8.33")
        );

        return new DetailedPortfolioDto(
                new BigDecimal("32500.00"),
                List.of(asset),
                analytics
        );
    }

    @Test
    @DisplayName("GET /api/v1/portfolio - должен возвращать 200 OK и портфель аутентифицированного пользователя")
    void getPortfolioShouldReturn200WhenAuthenticated() throws Exception {
        Long userId = 1L;
        UserPrincipal principal = createTestUserPrincipal(userId, "john_doe");
        DetailedPortfolioDto portfolioDto = createSamplePortfolioDto();

        when(portfolioService.getDetailedPortfolio(userId)).thenReturn(portfolioDto);

        mockMvc.perform(get("/api/v1/portfolio")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalanceUsd").value(32500.00))
                .andExpect(jsonPath("$.assets.length()").value(1))
                .andExpect(jsonPath("$.assets[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.assets[0].totalValueUsd").value(32500.00))
                .andExpect(jsonPath("$.analytics.pnl24hUsd").value(1200.50))
                .andExpect(jsonPath("$.analytics.pnl24hPercentage").value(3.83));

        verify(portfolioService, times(1)).getDetailedPortfolio(userId);
    }

    @Test
    @DisplayName("GET /api/v1/portfolio - должен возвращать 401 Unauthorized для неаутентифицированного запроса")
    void getPortfolioShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(portfolioService);
    }

    @Test
    @DisplayName("GET /api/v1/portfolio - должен возвращать 404 Not Found, если пользователь не найден")
    void getPortfolioShouldReturn404WhenUserNotFound() throws Exception {
        Long userId = 999L;
        UserPrincipal principal = createTestUserPrincipal(userId, "non_existing_user");

        when(portfolioService.getDetailedPortfolio(userId))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + userId));

        mockMvc.perform(get("/api/v1/portfolio")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(portfolioService, times(1)).getDetailedPortfolio(userId);
    }
}