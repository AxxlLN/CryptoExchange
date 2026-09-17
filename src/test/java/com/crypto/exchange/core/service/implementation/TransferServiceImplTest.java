package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.core.event.TransactionCompletedEvent;
import com.crypto.exchange.core.exception.InsufficientFundsException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.TransactionMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.TransactionRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletBalanceRepository walletBalanceRepository;

    @Mock
    private CryptoCurrencyRepository cryptoCurrencyRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferServiceImpl transferService;

    private User senderUser;
    private User recipientUser;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private CryptoCurrency btcCrypto;
    private TransferRequestDto transferRequestDto;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .email("sender@example.com")
                .build();

        recipientUser = User.builder()
                .id(2L)
                .email("recipient@example.com")
                .build();

        senderWallet = Wallet.builder()
                .id(10L)
                .address("sender-address-123")
                .user(senderUser)
                .build();

        recipientWallet = Wallet.builder()
                .id(20L)
                .address("recipient-address-456")
                .user(recipientUser)
                .build();

        btcCrypto = CryptoCurrency.builder()
                .id(100L)
                .externalId("bitcoin")
                .symbol("BTC")
                .name("Bitcoin")
                .build();

        transferRequestDto = new TransferRequestDto(
                10L,
                "recipient-address-456",
                "bitcoin",
                new BigDecimal("1.5")
        );
    }

    @Nested
    @DisplayName("Успешные сценарии перевода")
    class SuccessScenarios {

        @Test
        @DisplayName("Успешный перевод, когда у получателя уже есть баланс этой криптовалюты")
        void transfer_Success_ExistingRecipientBalance() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByAddress("recipient-address-456")).thenReturn(Optional.of(recipientWallet));
            when(cryptoCurrencyRepository.findByExternalId("bitcoin")).thenReturn(Optional.of(btcCrypto));

            when(walletBalanceRepository.deductBalanceNative(10L, 100L, new BigDecimal("1.5"))).thenReturn(1);
            when(walletBalanceRepository.addBalanceNative(20L, 100L, new BigDecimal("1.5"))).thenReturn(1);

            Transaction mockSavedTransaction = Transaction.builder().id(999L).build();
            when(transactionRepository.save(any(Transaction.class))).thenReturn(mockSavedTransaction);

            TransactionResponseDto mockResponseDto = new TransactionResponseDto(
                    999L, 10L, 20L, "BTC", "BTC",
                    new BigDecimal("1.5"), new BigDecimal("1.5"),
                    Transaction.TransactionType.TRANSFER,
                    TransactionResponseDto.TransactionDirection.OUTGOING,
                    null
            );
            when(transactionMapper.toResponseDto(mockSavedTransaction, 1L)).thenReturn(mockResponseDto);

            TransferResponseDto result = transferService.transfer(1L, transferRequestDto);

            assertThat(result).isNotNull();
            assertThat(result.senderAddress()).isEqualTo("sender-address-123");
            assertThat(result.recipientAddress()).isEqualTo("recipient-address-456");
            assertThat(result.cryptoSymbol()).isEqualTo("BTC");
            assertThat(result.amount()).isEqualTo(new BigDecimal("1.5"));
            assertThat(result.status()).isEqualTo("SUCCESS");

            verify(walletBalanceRepository, never()).save(any(WalletBalance.class));

            ArgumentCaptor<TransactionCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCompletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().recipientEmail()).isEqualTo("sender@example.com");
            assertThat(eventCaptor.getValue().transaction()).isEqualTo(mockResponseDto);
        }

        @Test
        @DisplayName("Успешный перевод, когда у получателя нет записи баланса (создается новая запись)")
        void transfer_Success_CreatesNewRecipientBalance() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByAddress("recipient-address-456")).thenReturn(Optional.of(recipientWallet));
            when(cryptoCurrencyRepository.findByExternalId("bitcoin")).thenReturn(Optional.of(btcCrypto));

            when(walletBalanceRepository.deductBalanceNative(10L, 100L, new BigDecimal("1.5"))).thenReturn(1);
            when(walletBalanceRepository.addBalanceNative(20L, 100L, new BigDecimal("1.5"))).thenReturn(0);

            Transaction mockSavedTransaction = Transaction.builder().id(999L).build();
            when(transactionRepository.save(any(Transaction.class))).thenReturn(mockSavedTransaction);

            transferService.transfer(1L, transferRequestDto);

            ArgumentCaptor<WalletBalance> balanceCaptor = ArgumentCaptor.forClass(WalletBalance.class);
            verify(walletBalanceRepository).save(balanceCaptor.capture());

            WalletBalance savedBalance = balanceCaptor.getValue();
            assertThat(savedBalance.getWallet()).isEqualTo(recipientWallet);
            assertThat(savedBalance.getCryptoCurrency()).isEqualTo(btcCrypto);
            assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("1.5"));
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками и исключениями")
    class ExceptionScenarios {

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если кошелек отправителя не найден")
        void transfer_SenderWalletNotFound_ThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transfer(1L, transferRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found: 10");
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если кошелек не принадлежит пользователю")
        void transfer_SenderWalletBelongsToAnotherUser_ThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));

            assertThatThrownBy(() -> transferService.transfer(999L, transferRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet not found: 10");
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если кошелек получателя не найден по адресу")
        void transfer_RecipientWalletNotFound_ThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByAddress("recipient-address-456")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transfer(1L, transferRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipient wallet not found with address: recipient-address-456");
        }

        @Test
        @DisplayName("Выбрасывает IllegalArgumentException при попытке перевести средства на тот же кошелек")
        void transfer_SameWallet_ThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByAddress("recipient-address-456")).thenReturn(Optional.of(senderWallet));

            assertThatThrownBy(() -> transferService.transfer(1L, transferRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot transfer funds to the same wallet");
        }

        @Test
        @DisplayName("Выбрасывает ResourceNotFoundException, если криптовалюта не найдена")
        void transfer_CryptoNotFound_ThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByAddress("recipient-address-456")).thenReturn(Optional.of(recipientWallet));
            when(cryptoCurrencyRepository.findByExternalId("bitcoin")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transfer(1L, transferRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Crypto currency not found with externalId: bitcoin");
        }

        @Test
        @DisplayName("Выбрасывает InsufficientFundsException, если не удалось списать баланс (недостаточно средств)")
        void transfer_InsufficientFunds_ThrowsException() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByAddress("recipient-address-456")).thenReturn(Optional.of(recipientWallet));
            when(cryptoCurrencyRepository.findByExternalId("bitcoin")).thenReturn(Optional.of(btcCrypto));

            when(walletBalanceRepository.deductBalanceNative(10L, 100L, new BigDecimal("1.5"))).thenReturn(0);

            assertThatThrownBy(() -> transferService.transfer(1L, transferRequestDto))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds or balance entry not found for: BTC");

            verify(walletBalanceRepository, never()).addBalanceNative(any(), any(), any());
            verify(transactionRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}