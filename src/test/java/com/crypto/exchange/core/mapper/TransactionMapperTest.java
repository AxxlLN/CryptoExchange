package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    private Wallet walletUser1;

    private Wallet walletUser1Second;

    private Wallet walletUser2;

    private CryptoCurrency btcCrypto;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();

        walletUser1 = Wallet.builder().id(10L).user(user1).build();
        walletUser1Second = Wallet.builder().id(11L).user(user1).build();
        walletUser2 = Wallet.builder().id(20L).user(user2).build();

        btcCrypto = CryptoCurrency.builder().id(100L).symbol("BTC").build();
        now = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("Маппинг основных полей и направления (Direction)")
    class DirectionAndMappingTests {

        @Test
        @DisplayName("Определяет направление OUTGOING, если отправитель - текущий пользователь")
        void toResponseDtoOutgoingDirection() {
            Transaction transaction = Transaction.builder()
                    .id(1000L)
                    .fromWallet(walletUser1)
                    .toWallet(walletUser2)
                    .fromCrypto(btcCrypto)
                    .toCrypto(btcCrypto)
                    .fromAmount(new BigDecimal("1.0"))
                    .toAmount(new BigDecimal("1.0"))
                    .type(Transaction.TransactionType.TRANSFER)
                    .createdAt(now)
                    .build();

            TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, 1L);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1000L);
            assertThat(dto.fromWalletId()).isEqualTo(10L);
            assertThat(dto.toWalletId()).isEqualTo(20L);
            assertThat(dto.fromCryptoSymbol()).isEqualTo("BTC");
            assertThat(dto.toCryptoSymbol()).isEqualTo("BTC");
            assertThat(dto.direction()).isEqualTo(TransactionDirection.OUTGOING);
        }

        @Test
        @DisplayName("Определяет направление INCOMING, если получатель - текущий пользователь")
        void toResponseDtoIncomingDirection() {
            Transaction transaction = Transaction.builder()
                    .id(1001L)
                    .fromWallet(walletUser2)
                    .toWallet(walletUser1)
                    .fromCrypto(btcCrypto)
                    .toCrypto(btcCrypto)
                    .fromAmount(new BigDecimal("2.0"))
                    .toAmount(new BigDecimal("2.0"))
                    .type(Transaction.TransactionType.TRANSFER)
                    .createdAt(now)
                    .build();

            TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, 1L);

            assertThat(dto).isNotNull();
            assertThat(dto.direction()).isEqualTo(TransactionDirection.INCOMING);
        }

        @Test
        @DisplayName("Определяет направление SELF, если оба кошелька принадлежат текущему пользователю")
        void toResponseDtoSelfDirection() {
            Transaction transaction = Transaction.builder()
                    .id(1002L)
                    .fromWallet(walletUser1)
                    .toWallet(walletUser1Second)
                    .fromCrypto(btcCrypto)
                    .toCrypto(btcCrypto)
                    .fromAmount(new BigDecimal("0.5"))
                    .toAmount(new BigDecimal("0.5"))
                    .type(Transaction.TransactionType.TRANSFER)
                    .createdAt(now)
                    .build();

            TransactionResponseDto dto = transactionMapper.toResponseDto(transaction, 1L);

            assertThat(dto).isNotNull();
            assertThat(dto.direction()).isEqualTo(TransactionDirection.SELF);
        }
    }

    @Nested
    @DisplayName("Корректность обработки null значений в кошельках")
    class NullSafetyTests {

        @Test
        @DisplayName("Корректно обрабатывает null в fromWallet (например, внешнее пополнение)")
        void calculateDirectionNullFromWalletReturnsIncoming() {
            Transaction transaction = Transaction.builder()
                    .fromWallet(null)
                    .toWallet(walletUser1)
                    .build();

            TransactionDirection direction = transactionMapper.calculateDirection(transaction, 1L);

            assertThat(direction).isEqualTo(TransactionDirection.INCOMING);
        }

        @Test
        @DisplayName("Корректно обрабатывает null в toWallet (например, вывод на внешний адрес)")
        void calculateDirectionNullToWalletReturnsOutgoing() {
            Transaction transaction = Transaction.builder()
                    .fromWallet(walletUser1)
                    .toWallet(null)
                    .build();

            TransactionDirection direction = transactionMapper.calculateDirection(transaction, 1L);

            assertThat(direction).isEqualTo(TransactionDirection.OUTGOING);
        }

        @Test
        @DisplayName("Возвращает null при передаче null сущности Transaction")
        void toResponseDtoNullEntityReturnsNull() {
            assertThat(transactionMapper.toResponseDto(null, 1L)).isNull();
        }
    }
}