package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.PortfolioSnapshot;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.PortfolioSnapshotRepository;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.web.dto.response.DetailedPortfolioDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private User user;
    private CryptoCurrency btc;
    private CryptoCurrency eth;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .build();

        btc = CryptoCurrency.builder()
                .id(10L)
                .symbol("BTC")
                .name("Bitcoin")
                .priceUsd(new BigDecimal("50000.00"))
                .build();

        eth = CryptoCurrency.builder()
                .id(20L)
                .symbol("ETH")
                .name("Ethereum")
                .priceUsd(new BigDecimal("3000.00"))
                .build();
    }

    @Test
    void getDetailedPortfolioShouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> portfolioService.getDetailedPortfolio(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");

        verify(walletRepository, never()).findAllByUserId(any());
    }

    @Test
    void getDetailedPortfolioShouldReturnEmptyPortfolioWhenUserHasNoWallets() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(walletRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(portfolioSnapshotRepository.findAllByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(1L), any()))
                .thenReturn(Collections.emptyList());

        DetailedPortfolioDto result = portfolioService.getDetailedPortfolio(1L);

        assertThat(result.totalBalanceUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.assets()).isEmpty();
        assertThat(result.analytics().pnl24hUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.analytics().pnl24hPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.analytics().pnl7dUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.analytics().pnl7dPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getDetailedPortfolioShouldCalculateAggregatedBalancesAndPnlCorrectly() {
        when(userRepository.existsById(1L)).thenReturn(true);

        WalletBalance wb1 = WalletBalance.builder().cryptoCurrency(btc).amount(new BigDecimal("1.0")).build();
        WalletBalance wb2 = WalletBalance.builder().cryptoCurrency(eth).amount(new BigDecimal("2.0")).build();
        Wallet wallet1 = Wallet.builder().balances(Set.of(wb1, wb2)).build();

        WalletBalance wb3 = WalletBalance.builder().cryptoCurrency(btc).amount(new BigDecimal("0.5")).build();
        Wallet wallet2 = Wallet.builder().balances(Set.of(wb3)).build();

        when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(wallet1, wallet2));

        // Итого в портфеле:
        // BTC: 1.5 * 50000 = 75000 USD
        // ETH: 2.0 * 3000 = 6000 USD
        // Total = 81000 USD

        PortfolioSnapshot snapshot24h = PortfolioSnapshot.builder()
                .totalBalanceUsd(new BigDecimal("70000.00"))
                .recordedAt(LocalDateTime.now().minusHours(12))
                .build();

        PortfolioSnapshot snapshot7d = PortfolioSnapshot.builder()
                .totalBalanceUsd(new BigDecimal("60000.00"))
                .recordedAt(LocalDateTime.now().minusDays(5))
                .build();

        when(portfolioSnapshotRepository.findAllByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(1L), any()))
                .thenReturn(List.of(snapshot24h))
                .thenReturn(List.of(snapshot7d));

        DetailedPortfolioDto result = portfolioService.getDetailedPortfolio(1L);

        assertThat(result.totalBalanceUsd()).isEqualByComparingTo(new BigDecimal("81000.00"));
        assertThat(result.assets()).hasSize(2);

        DetailedPortfolioDto.CryptoAssetBalanceDto btcAsset = result.assets().stream()
                .filter(a -> a.symbol().equals("BTC"))
                .findFirst()
                .orElseThrow();
        assertThat(btcAsset.totalAmount()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(btcAsset.currentPriceUsd()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(btcAsset.totalValueUsd()).isEqualByComparingTo(new BigDecimal("75000.00"));

        assertThat(result.analytics().pnl24hUsd()).isEqualByComparingTo(new BigDecimal("11000.00"));
        BigDecimal expectedPnl24hPct = new BigDecimal("11000.00")
                .multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal("70000.00"), 2, RoundingMode.HALF_UP);
        assertThat(result.analytics().pnl24hPercentage()).isEqualByComparingTo(expectedPnl24hPct);

        assertThat(result.analytics().pnl7dUsd()).isEqualByComparingTo(new BigDecimal("21000.00"));
        BigDecimal expectedPnl7dPct = new BigDecimal("21000.00")
                .multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal("60000.00"), 2, RoundingMode.HALF_UP);
        assertThat(result.analytics().pnl7dPercentage()).isEqualByComparingTo(expectedPnl7dPct);
    }

    @Test
    void getDetailedPortfolioShouldHandleNullCryptoPriceAndZeroSnapshotBalance() {
        when(userRepository.existsById(1L)).thenReturn(true);

        CryptoCurrency unknownCrypto = CryptoCurrency.builder()
                .id(30L)
                .symbol("UNKNOWN")
                .name("Unknown Token")
                .priceUsd(null)
                .build();

        WalletBalance wb = WalletBalance.builder().cryptoCurrency(unknownCrypto).amount(new BigDecimal("100")).build();
        Wallet wallet = Wallet.builder().balances(Set.of(wb)).build();

        when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(wallet));

        PortfolioSnapshot zeroSnapshot = PortfolioSnapshot.builder()
                .totalBalanceUsd(BigDecimal.ZERO)
                .build();

        when(portfolioSnapshotRepository.findAllByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(1L), any()))
                .thenReturn(List.of(zeroSnapshot));

        DetailedPortfolioDto result = portfolioService.getDetailedPortfolio(1L);

        assertThat(result.totalBalanceUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.assets()).hasSize(1);
        assertThat(result.assets().get(0).currentPriceUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.assets().get(0).percentageOfPortfolio()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.analytics().pnl24hPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.analytics().pnl7dPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void takeSnapshotShouldDoNothingWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        portfolioService.takeSnapshot(99L);

        verify(walletRepository, never()).findAllByUserId(any());
        verify(portfolioSnapshotRepository, never()).save(any());
    }

    @Test
    void takeSnapshotShouldCalculateTotalUsdAndSaveSnapshot() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        WalletBalance wb1 = WalletBalance.builder().cryptoCurrency(btc).amount(new BigDecimal("2.0")).build(); // 100 000 USD
        WalletBalance wb2 = WalletBalance.builder().cryptoCurrency(eth).amount(new BigDecimal("10.0")).build(); // 30 000 USD
        Wallet wallet = Wallet.builder().balances(Set.of(wb1, wb2)).build();

        when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(wallet));

        portfolioService.takeSnapshot(1L);

        ArgumentCaptor<PortfolioSnapshot> snapshotCaptor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(portfolioSnapshotRepository).save(snapshotCaptor.capture());

        PortfolioSnapshot savedSnapshot = snapshotCaptor.getValue();
        assertThat(savedSnapshot.getUser()).isEqualTo(user);
        assertThat(savedSnapshot.getTotalBalanceUsd()).isEqualByComparingTo(new BigDecimal("130000.00"));
        assertThat(savedSnapshot.getRecordedAt()).isNotNull();
    }
}