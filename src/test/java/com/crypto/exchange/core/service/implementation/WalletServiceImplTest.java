package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.core.exception.InsufficientFundsException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.WalletMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.core.service.PortfolioService;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CryptoCurrencyRepository cryptoCurrencyRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletBalanceRepository walletBalanceRepository;

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User user;
    private User wrongUser;
    private Wallet wallet;
    private WalletResponseDto walletResponseDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .build();

        wrongUser = User.builder()
                .id(2L)
                .username("other_user")
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .address("0x123abc")
                .name("Main Wallet")
                .isDefault(true)
                .user(user)
                .balances(new HashSet<>())
                .build();

        walletResponseDto = new WalletResponseDto(10L, "0x123abc", "Main Wallet", true, Collections.emptyList());
    }

    @Nested
    @DisplayName("Тесты метода createWallet")
    class CreateWalletTests {

        @Test
        void createWalletShouldThrowResourceNotFoundExceptionWhenUserNotFound() {
            CreateWalletRequestDto request = new CreateWalletRequestDto("Main", true);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.createWallet(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 1");

            verify(walletRepository, never()).save(any());
        }

        @Test
        void createWalletFirstWalletShouldBeDefaultAutomatically() {
            CreateWalletRequestDto request = new CreateWalletRequestDto("First Wallet", false);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(walletRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());

            CryptoCurrency btc = CryptoCurrency.builder().id(100L).symbol("BTC").externalId("bitcoin").build();
            when(cryptoCurrencyRepository.findAll()).thenReturn(List.of(btc));

            Wallet savedWalletWithoutBalances = Wallet.builder()
                    .id(10L)
                    .name("First Wallet")
                    .isDefault(true)
                    .user(user)
                    .balances(new HashSet<>())
                    .build();
            when(walletRepository.save(any(Wallet.class))).thenReturn(savedWalletWithoutBalances);

            when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(walletResponseDto);

            WalletResponseDto result = walletService.createWallet(1L, request);

            assertThat(result).isEqualTo(walletResponseDto);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository, times(2)).save(walletCaptor.capture());

            Wallet initialSaved = walletCaptor.getAllValues().get(0);
            assertThat(initialSaved.getIsDefault()).isTrue();
            assertThat(initialSaved.getName()).isEqualTo("First Wallet");

            Wallet finalSaved = walletCaptor.getAllValues().get(1);
            assertThat(finalSaved.getBalances()).hasSize(1);
            WalletBalance createdBalance = finalSaved.getBalances().iterator().next();
            assertThat(createdBalance.getCryptoCurrency()).isEqualTo(btc);
            assertThat(createdBalance.getAmount()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void createWalletNewDefaultWalletShouldResetPreviousDefaultWallets() {
            CreateWalletRequestDto request = new CreateWalletRequestDto("Second Wallet", true);

            Wallet existingDefaultWallet = Wallet.builder()
                    .id(10L)
                    .name("Old Default")
                    .isDefault(true)
                    .user(user)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(existingDefaultWallet));
            when(cryptoCurrencyRepository.findAll()).thenReturn(Collections.emptyList());

            Wallet savedWallet = Wallet.builder()
                    .id(11L)
                    .name("Second Wallet")
                    .isDefault(true)
                    .user(user)
                    .balances(new HashSet<>())
                    .build();
            when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);
            when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(walletResponseDto);

            walletService.createWallet(1L, request);

            assertThat(existingDefaultWallet.getIsDefault()).isFalse();
            verify(walletRepository).saveAll(List.of(existingDefaultWallet));
        }
    }

    @Nested
    @DisplayName("Тесты метода getUserWallets")
    class GetUserWalletsTests {

        @Test
        void getUserWalletsShouldReturnListOfWalletResponseDtos() {
            when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(wallet));
            when(walletMapper.toResponseDto(wallet)).thenReturn(walletResponseDto);

            List<WalletResponseDto> result = walletService.getUserWallets(1L);

            assertThat(result).containsExactly(walletResponseDto);
            verify(walletRepository).findAllByUserId(1L);
        }
    }

    @Nested
    @DisplayName("Тесты метода getWalletByAddressAndUser")
    class GetWalletByAddressAndUserTests {

        @Test
        void getWalletByAddressAndUserShouldReturnDtoWhenUserIsOwner() {
            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.of(wallet));
            when(walletMapper.toResponseDto(wallet)).thenReturn(walletResponseDto);

            WalletResponseDto result = walletService.getWalletByAddressAndUser(1L, "0x123abc");

            assertThat(result).isEqualTo(walletResponseDto);
        }

        @Test
        void getWalletByAddressAndUserShouldThrowExceptionWhenWalletNotFound() {
            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getWalletByAddressAndUser(1L, "0x123abc"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found with address: 0x123abc");
        }

        @Test
        void getWalletByAddressAndUserShouldThrowExceptionWhenUserIsNotOwner() {
            wallet.setUser(wrongUser);
            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.getWalletByAddressAndUser(1L, "0x123abc"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found with address: 0x123abc");
        }
    }

    @Nested
    @DisplayName("Тесты метода deposit")
    class DepositTests {

        @Test
        void depositShouldIncreaseBalanceAndTakeSnapshot() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("1.5"));

            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.of(wallet));
            when(walletBalanceRepository.addBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("1.5"))).thenReturn(1);
            when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
            when(walletMapper.toResponseDto(wallet)).thenReturn(walletResponseDto);

            WalletResponseDto result = walletService.deposit(1L, "0x123abc", request);

            assertThat(result).isEqualTo(walletResponseDto);
            verify(portfolioService).takeSnapshot(1L);
        }

        @Test
        void depositShouldThrowResourceNotFoundExceptionWhenBalanceEntryNotFound() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("unknown", new BigDecimal("1.5"));

            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.of(wallet));
            when(walletBalanceRepository.addBalanceByExternalIdNative(10L, "unknown", new BigDecimal("1.5"))).thenReturn(0);

            assertThatThrownBy(() -> walletService.deposit(1L, "0x123abc", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Balance entry not found for externalId: unknown");

            verify(portfolioService, never()).takeSnapshot(any());
        }
    }

    @Nested
    @DisplayName("Тесты метода withdraw")
    class WithdrawTests {

        @Test
        void withdrawShouldDeductBalanceAndTakeSnapshot() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("0.5"));

            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.of(wallet));
            when(walletBalanceRepository.deductBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("0.5"))).thenReturn(1);
            when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
            when(walletMapper.toResponseDto(wallet)).thenReturn(walletResponseDto);

            WalletResponseDto result = walletService.withdraw(1L, "0x123abc", request);

            assertThat(result).isEqualTo(walletResponseDto);
            verify(portfolioService).takeSnapshot(1L);
        }

        @Test
        void withdrawShouldThrowInsufficientFundsExceptionWhenUpdatedRowsIsZero() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("100.0"));

            when(walletRepository.findByAddress("0x123abc")).thenReturn(Optional.of(wallet));
            when(walletBalanceRepository.deductBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("100.0"))).thenReturn(0);

            assertThatThrownBy(() -> walletService.withdraw(1L, "0x123abc", request))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds or balance entry not found for externalId: bitcoin");

            verify(portfolioService, never()).takeSnapshot(any());
        }
    }

    @Nested
    @DisplayName("Тесты метода getAggregatedBalances")
    class GetAggregatedBalancesTests {

        @Test
        void getAggregatedBalancesShouldReturnListWhenUserExists() {
            AggregatedBalanceDto aggregatedDto = new AggregatedBalanceDto(100L, "BTC", "Bitcoin", new BigDecimal("2.5"));
            when(userRepository.existsById(1L)).thenReturn(true);
            when(walletBalanceRepository.getAggregatedBalancesByUserId(1L)).thenReturn(List.of(aggregatedDto));

            List<AggregatedBalanceDto> result = walletService.getAggregatedBalances(1L);

            assertThat(result).containsExactly(aggregatedDto);
        }

        @Test
        void getAggregatedBalancesShouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {
            when(userRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> walletService.getAggregatedBalances(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 1");

            verify(walletBalanceRepository, never()).getAggregatedBalancesByUserId(any());
        }
    }
}