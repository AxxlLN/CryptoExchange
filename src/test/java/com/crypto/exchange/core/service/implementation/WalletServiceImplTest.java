package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.exception.InsufficientFundsException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.WalletMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private WalletServiceImpl walletService;

    private User testUser;
    private Wallet testWallet;
    private WalletResponseDto expectedDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        testWallet = Wallet.builder()
                .id(10L)
                .address("0x123456789abcdef")
                .name("Primary Wallet")
                .isDefault(true)
                .user(testUser)
                .balances(new HashSet<>())
                .build();

        expectedDto = new WalletResponseDto(
                10L,
                "0x123456789abcdef",
                "Primary Wallet",
                true,
                Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("Создание кошелька (createWallet)")
    class CreateWalletTests {

        @Test
        @DisplayName("Успешное создание первого кошелька (автоматически назначается isDefault=true)")
        void createWalletFirstWalletSetDefaultTrue() {
            CreateWalletRequestDto request = new CreateWalletRequestDto("Main Wallet", false);
            CryptoCurrency btc = CryptoCurrency.builder().id(100L).externalId("bitcoin").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(walletRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cryptoCurrencyRepository.findAll()).thenReturn(List.of(btc));
            when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(expectedDto);

            WalletResponseDto result = walletService.createWallet(1L, request);

            assertThat(result).isNotNull();
            verify(walletRepository, times(2)).save(any(Wallet.class));
            verify(cryptoCurrencyRepository).findAll();
            verify(walletRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Создание нового дефолтного кошелька сбрасывает флаг isDefault у ранее существующих")
        void createWalletNewDefaultWalletResetsExistingDefault() {
            CreateWalletRequestDto request = new CreateWalletRequestDto("New Default Wallet", true);
            Wallet existingDefaultWallet = Wallet.builder()
                    .id(5L)
                    .name("Old Default")
                    .isDefault(true)
                    .user(testUser)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(existingDefaultWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cryptoCurrencyRepository.findAll()).thenReturn(Collections.emptyList());
            when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(expectedDto);

            WalletResponseDto result = walletService.createWallet(1L, request);

            assertThat(result).isNotNull();
            assertThat(existingDefaultWallet.getIsDefault()).isFalse();
            verify(walletRepository).saveAll(List.of(existingDefaultWallet));
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если пользователь не найден")
        void createWalletUserNotFoundThrowsException() {
            CreateWalletRequestDto request = new CreateWalletRequestDto("Wallet", false);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.createWallet(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 1");
        }
    }

    @Nested
    @DisplayName("Депозит средств (deposit)")
    class DepositTests {

        @Test
        @DisplayName("Успешное пополнение баланса")
        void depositSuccess() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("1.5"));

            when(walletRepository.findById(10L)).thenReturn(Optional.of(testWallet));
            when(walletBalanceRepository.addBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("1.5"))).thenReturn(1);
            when(walletMapper.toResponseDto(testWallet)).thenReturn(expectedDto);

            WalletResponseDto result = walletService.deposit(1L, 10L, request);

            assertThat(result).isNotNull();
            verify(walletBalanceRepository).addBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("1.5"));
            verify(walletRepository, times(2)).findById(10L);
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если баланс по externalId не существует")
        void depositBalanceEntryNotFoundThrowsException() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("ethereum", new BigDecimal("2.0"));

            when(walletRepository.findById(10L)).thenReturn(Optional.of(testWallet));
            when(walletBalanceRepository.addBalanceByExternalIdNative(10L, "ethereum", new BigDecimal("2.0"))).thenReturn(0);

            assertThatThrownBy(() -> walletService.deposit(1L, 10L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Balance entry not found for externalId: ethereum");
        }
    }

    @Nested
    @DisplayName("Вывод средств (withdraw)")
    class WithdrawTests {

        @Test
        @DisplayName("Успешное списание средств с баланса")
        void withdrawSuccess() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("0.5"));

            when(walletRepository.findById(10L)).thenReturn(Optional.of(testWallet));
            when(walletBalanceRepository.deductBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("0.5"))).thenReturn(1);
            when(walletMapper.toResponseDto(testWallet)).thenReturn(expectedDto);

            WalletResponseDto result = walletService.withdraw(1L, 10L, request);

            assertThat(result).isNotNull();
            verify(walletBalanceRepository).deductBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("0.5"));
        }

        @Test
        @DisplayName("Выбрасывает InsufficientFundsException при недостаточном количестве средств")
        void withdrawInsufficientFundsThrowsException() {
            BalanceOperationRequestDto request = new BalanceOperationRequestDto("bitcoin", new BigDecimal("999.0"));

            when(walletRepository.findById(10L)).thenReturn(Optional.of(testWallet));
            when(walletBalanceRepository.deductBalanceByExternalIdNative(10L, "bitcoin", new BigDecimal("999.0"))).thenReturn(0);

            assertThatThrownBy(() -> walletService.withdraw(1L, 10L, request))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds or balance entry not found for externalId: bitcoin");
        }
    }

    @Nested
    @DisplayName("Получение данных и проверка владельца")
    class ReadAndSecurityTests {

        @Test
        @DisplayName("getUserWallets возвращает список кошельков пользователя")
        void getUserWalletsSuccess() {
            when(walletRepository.findAllByUserId(1L)).thenReturn(List.of(testWallet));
            when(walletMapper.toResponseDto(testWallet)).thenReturn(expectedDto);

            List<WalletResponseDto> result = walletService.getUserWallets(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("getWalletById возвращает кошелек настоящему владельцу")
        void getWalletByIdSuccess() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponseDto(testWallet)).thenReturn(expectedDto);

            WalletResponseDto result = walletService.getWalletById(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("getWalletById выбрасывает ResourceNotFoundException, если запрашивает сторонний пользователь")
        void getWalletByIdForeignUserThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(testWallet));

            assertThatThrownBy(() -> walletService.getWalletById(999L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found with id: 10");
        }

        @Test
        @DisplayName("getWalletByAddress возвращает кошелек по его публичному адресу")
        void getWalletByAddressSuccess() {
            when(walletRepository.findByAddress("0x123456789abcdef")).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponseDto(testWallet)).thenReturn(expectedDto);

            WalletResponseDto result = walletService.getWalletByAddress("0x123456789abcdef");

            assertThat(result).isNotNull();
            verify(walletRepository).findByAddress("0x123456789abcdef");
        }

        @Test
        @DisplayName("getWalletByAddress выбрасывает ResourceNotFoundException при несуществующем адресе")
        void getWalletByAddressNotFoundThrowsException() {
            when(walletRepository.findByAddress("0xInvalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getWalletByAddress("0xInvalid"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found with address: 0xInvalid");
        }

        @Test
        @DisplayName("getAggregatedBalances возвращает список агрегированных остатков")
        void getAggregatedBalancesSuccess() {
            AggregatedBalanceDto dto = new AggregatedBalanceDto(100L, "BTC", "Bitcoin", new BigDecimal("10.5"));

            when(userRepository.existsById(1L)).thenReturn(true);
            when(walletBalanceRepository.getAggregatedBalancesByUserId(1L)).thenReturn(List.of(dto));

            List<AggregatedBalanceDto> result = walletService.getAggregatedBalances(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).totalAmount()).isEqualTo(new BigDecimal("10.5"));
            assertThat(result.get(0).symbol()).isEqualTo("BTC");
        }

        @Test
        @DisplayName("getAggregatedBalances выбрасывает ResourceNotFoundException при несуществующем пользователе")
        void getAggregatedBalancesUserNotFoundThrowsException() {
            when(userRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> walletService.getAggregatedBalances(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 1");
        }
    }
}