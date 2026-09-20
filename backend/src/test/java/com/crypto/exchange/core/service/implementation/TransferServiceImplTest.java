package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.entity.Transaction.TransactionType;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private CryptoCurrency crypto;
    private TransferRequestDto requestDto;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .email("sender@example.com")
                .build();

        senderWallet = Wallet.builder()
                .id(10L)
                .address("0xSENDER")
                .user(senderUser)
                .build();

        recipientWallet = Wallet.builder()
                .id(20L)
                .address("0xRECIPIENT")
                .user(User.builder().id(2L).email("recipient@example.com").build())
                .build();

        crypto = CryptoCurrency.builder()
                .id(100L)
                .symbol("BTC")
                .build();

        requestDto = new TransferRequestDto(
                "0xSENDER",
                "0xRECIPIENT",
                "bitcoin-ext-id",
                new BigDecimal("1.5")
        );
    }

    @Test
    void transferShouldThrowResourceNotFoundExceptionWhenSenderWalletNotFound() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(1L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sender wallet not found with address: 0xSENDER");

        verify(walletBalanceRepository, never()).deductBalanceNative(any(), any(), any());
    }

    @Test
    void transferShouldThrowResourceNotFoundExceptionWhenSenderWalletBelongsToAnotherUser() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));

        assertThatThrownBy(() -> transferService.transfer(999L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sender wallet not found with address: 0xSENDER");

        verify(walletBalanceRepository, never()).deductBalanceNative(any(), any(), any());
    }

    @Test
    void transferShouldThrowResourceNotFoundExceptionWhenRecipientWalletNotFound() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAddress("0xRECIPIENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(1L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipient wallet not found with address: 0xRECIPIENT");

        verify(walletBalanceRepository, never()).deductBalanceNative(any(), any(), any());
    }

    @Test
    void transferShouldThrowIllegalArgumentExceptionWhenTransferringToSameWallet() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAddress("0xRECIPIENT")).thenReturn(Optional.of(senderWallet));

        assertThatThrownBy(() -> transferService.transfer(1L, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer funds to the same wallet");

        verify(walletBalanceRepository, never()).deductBalanceNative(any(), any(), any());
    }

    @Test
    void transferShouldThrowResourceNotFoundExceptionWhenCryptoNotFound() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAddress("0xRECIPIENT")).thenReturn(Optional.of(recipientWallet));
        when(cryptoCurrencyRepository.findByExternalId("bitcoin-ext-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(1L, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Crypto currency not found with externalId: bitcoin-ext-id");

        verify(walletBalanceRepository, never()).deductBalanceNative(any(), any(), any());
    }

    @Test
    void transferShouldThrowInsufficientFundsExceptionWhenDeductFails() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAddress("0xRECIPIENT")).thenReturn(Optional.of(recipientWallet));
        when(cryptoCurrencyRepository.findByExternalId("bitcoin-ext-id")).thenReturn(Optional.of(crypto));
        when(walletBalanceRepository.deductBalanceNative(10L, 100L, new BigDecimal("1.5"))).thenReturn(0);

        assertThatThrownBy(() -> transferService.transfer(1L, requestDto))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds or balance entry not found for: BTC");

        verify(walletBalanceRepository, never()).addBalanceNative(any(), any(), any());
    }

    @Test
    void transferShouldSuccessfullyTransferWhenRecipientAlreadyHasBalanceEntry() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAddress("0xRECIPIENT")).thenReturn(Optional.of(recipientWallet));
        when(cryptoCurrencyRepository.findByExternalId("bitcoin-ext-id")).thenReturn(Optional.of(crypto));
        when(walletBalanceRepository.deductBalanceNative(10L, 100L, new BigDecimal("1.5"))).thenReturn(1);
        when(walletBalanceRepository.addBalanceNative(20L, 100L, new BigDecimal("1.5"))).thenReturn(1);

        Transaction savedTx = Transaction.builder()
                .id(500L)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponseDto txResponseDto = new TransactionResponseDto(
                500L, "0xSENDER", "0xRECIPIENT", "BTC", "BTC",
                new BigDecimal("1.5"), new BigDecimal("1.5"),
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                TransactionResponseDto.TransactionDirection.OUTGOING, OffsetDateTime.now()
        );
        when(transactionMapper.toResponseDto(savedTx, 1L)).thenReturn(txResponseDto);

        TransferResponseDto expectedTransferResponse = new TransferResponseDto(
                500L, "0xSENDER", "0xRECIPIENT", "BTC", new BigDecimal("1.5"), OffsetDateTime.now(), TransactionStatus.SUCCESS
        );
        when(transactionMapper.toTransferResponseDto(savedTx)).thenReturn(expectedTransferResponse);

        TransferResponseDto result = transferService.transfer(1L, requestDto);

        assertThat(result).isEqualTo(expectedTransferResponse);

        verify(walletBalanceRepository, never()).save(any(WalletBalance.class));

        ArgumentCaptor<TransactionCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TransactionCompletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.recipientEmail()).isEqualTo("sender@example.com");
        assertThat(capturedEvent.transaction()).isEqualTo(txResponseDto);
    }

    @Test
    void transferShouldCreateNewWalletBalanceForRecipientWhenAddBalanceNativeReturnsZero() {
        when(walletRepository.findByAddress("0xSENDER")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAddress("0xRECIPIENT")).thenReturn(Optional.of(recipientWallet));
        when(cryptoCurrencyRepository.findByExternalId("bitcoin-ext-id")).thenReturn(Optional.of(crypto));
        when(walletBalanceRepository.deductBalanceNative(10L, 100L, new BigDecimal("1.5"))).thenReturn(1);
        when(walletBalanceRepository.addBalanceNative(20L, 100L, new BigDecimal("1.5"))).thenReturn(0);

        Transaction savedTx = Transaction.builder().id(501L).build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponseDto txResponseDto = new TransactionResponseDto(
                501L, "0xSENDER", "0xRECIPIENT", "BTC", "BTC",
                new BigDecimal("1.5"), new BigDecimal("1.5"),
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                TransactionResponseDto.TransactionDirection.OUTGOING, OffsetDateTime.now()
        );
        when(transactionMapper.toResponseDto(savedTx, 1L)).thenReturn(txResponseDto);

        transferService.transfer(1L, requestDto);

        ArgumentCaptor<WalletBalance> balanceCaptor = ArgumentCaptor.forClass(WalletBalance.class);
        verify(walletBalanceRepository).save(balanceCaptor.capture());

        WalletBalance savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getWallet()).isEqualTo(recipientWallet);
        assertThat(savedBalance.getCryptoCurrency()).isEqualTo(crypto);
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("1.5"));
    }
}