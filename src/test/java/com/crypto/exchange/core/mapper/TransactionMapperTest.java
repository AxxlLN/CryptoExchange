package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    void toResponseDtoShouldMapAllFieldsCorrectlyAndSetOutgoingDirection() {
        Long currentUserId = 100L;
        Long otherUserId = 200L;

        User currentUser = User.builder().id(currentUserId).build();
        User otherUser = User.builder().id(otherUserId).build();

        Wallet fromWallet = Wallet.builder().address("0xFROM").user(currentUser).build();
        Wallet toWallet = Wallet.builder().address("0xTO").user(otherUser).build();

        CryptoCurrency btc = CryptoCurrency.builder().symbol("BTC").build();
        CryptoCurrency usdt = CryptoCurrency.builder().symbol("USDT").build();

        OffsetDateTime now = OffsetDateTime.now();

        Transaction transaction = Transaction.builder()
                .id(1L)
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .fromCrypto(btc)
                .toCrypto(usdt)
                .fromAmount(new BigDecimal("1.0"))
                .toAmount(new BigDecimal("50000.0"))
                .type(Transaction.TransactionType.EXCHANGE)
                .status(Transaction.TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, currentUserId);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.fromWalletAddress()).isEqualTo("0xFROM");
        assertThat(dto.toWalletAddress()).isEqualTo("0xTO");
        assertThat(dto.fromCryptoSymbol()).isEqualTo("BTC");
        assertThat(dto.toCryptoSymbol()).isEqualTo("USDT");
        assertThat(dto.fromAmount()).isEqualTo(new BigDecimal("1.0"));
        assertThat(dto.toAmount()).isEqualTo(new BigDecimal("50000.0"));
        assertThat(dto.type()).isEqualTo(Transaction.TransactionType.EXCHANGE);
        assertThat(dto.status()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
        assertThat(dto.direction()).isEqualTo(TransactionDirection.OUTGOING);
        assertThat(dto.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponseDtoShouldSetIncomingDirectionWhenReceivingFromOtherUser() {
        Long currentUserId = 100L;
        Long otherUserId = 200L;

        User currentUser = User.builder().id(currentUserId).build();
        User otherUser = User.builder().id(otherUserId).build();

        Wallet fromWallet = Wallet.builder().address("0xFROM").user(otherUser).build();
        Wallet toWallet = Wallet.builder().address("0xTO").user(currentUser).build();

        Transaction transaction = Transaction.builder()
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .build();

        TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, currentUserId);

        assertThat(dto).isNotNull();
        assertThat(dto.direction()).isEqualTo(TransactionDirection.INCOMING);
    }

    @Test
    void toResponseDtoShouldSetSelfDirectionWhenTransferringBetweenOwnWallets() {
        Long currentUserId = 100L;

        User currentUser = User.builder().id(currentUserId).build();

        Wallet fromWallet = Wallet.builder().address("0xMY_WALLET_1").user(currentUser).build();
        Wallet toWallet = Wallet.builder().address("0xMY_WALLET_2").user(currentUser).build();

        Transaction transaction = Transaction.builder()
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .build();

        TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, currentUserId);

        assertThat(dto).isNotNull();
        assertThat(dto.direction()).isEqualTo(TransactionDirection.SELF);
    }

    @Test
    void toResponseDtoShouldSetIncomingDirectionWhenFromWalletIsNull() {
        Long currentUserId = 100L;

        User currentUser = User.builder().id(currentUserId).build();
        Wallet toWallet = Wallet.builder().address("0xTO").user(currentUser).build();

        Transaction transaction = Transaction.builder()
                .fromWallet(null)
                .toWallet(toWallet)
                .build();

        TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, currentUserId);

        assertThat(dto).isNotNull();
        assertThat(dto.fromWalletAddress()).isNull();
        assertThat(dto.direction()).isEqualTo(TransactionDirection.INCOMING);
    }

    @Test
    void toResponseDtoShouldSetIncomingDirectionWhenToWalletIsNull() {
        Long currentUserId = 100L;

        User otherUser = User.builder().id(200L).build();
        Wallet fromWallet = Wallet.builder().address("0xFROM").user(otherUser).build();

        Transaction transaction = Transaction.builder()
                .fromWallet(fromWallet)
                .toWallet(null)
                .build();

        TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, currentUserId);

        assertThat(dto).isNotNull();
        assertThat(dto.toWalletAddress()).isNull();
        assertThat(dto.direction()).isEqualTo(TransactionDirection.INCOMING);
    }

    @Test
    void toResponseDtoShouldReturnNullWhenEntityIsNull() {
        TransactionResponseDto dto = transactionMapper.toResponseDto(null, 100L);

        assertThat(dto).isNull();
    }

    @Test
    void toTransferResponseDtoShouldMapAllFieldsCorrectly() {
        Wallet fromWallet = Wallet.builder().address("0xSENDER").build();
        Wallet toWallet = Wallet.builder().address("0xRECIPIENT").build();
        CryptoCurrency eth = CryptoCurrency.builder().symbol("ETH").build();
        OffsetDateTime now = OffsetDateTime.now();

        Transaction transaction = Transaction.builder()
                .id(10L)
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .fromCrypto(eth)
                .fromAmount(new BigDecimal("2.5"))
                .status(Transaction.TransactionStatus.SUCCESS)
                .createdAt(now)
                .build();

        TransferResponseDto dto = transactionMapper.toTransferResponseDto(transaction);

        assertThat(dto).isNotNull();
        assertThat(dto.transactionId()).isEqualTo(10L);
        assertThat(dto.senderAddress()).isEqualTo("0xSENDER");
        assertThat(dto.recipientAddress()).isEqualTo("0xRECIPIENT");
        assertThat(dto.cryptoSymbol()).isEqualTo("ETH");
        assertThat(dto.amount()).isEqualTo(new BigDecimal("2.5"));
        assertThat(dto.status()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
        assertThat(dto.timestamp()).isEqualTo(now);
    }

    @Test
    void toTransferResponseDtoShouldReturnNullWhenEntityIsNull() {
        TransferResponseDto dto = transactionMapper.toTransferResponseDto(null);

        assertThat(dto).isNull();
    }
}