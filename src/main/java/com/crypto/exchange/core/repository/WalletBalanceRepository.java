package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с промежуточной таблицей-связкой Many-to-Many (WalletBalance).
 */
@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {

    /**
     * Поиск конкретного баланса монеты в кошельке.
     */
    Optional<WalletBalance> findByWalletIdAndCryptoCurrencyId(Long walletId, Long cryptoId);

    /**
     * Получить все активные балансы конкретного кошелька.
     */
    List<WalletBalance> findAllByWalletId(Long walletId);

    /**
     * Нативный SQL-запрос: атомарное списание средств с баланса (защита от "Lost Update").
     * Проверяет, чтобы остаток был >= списываемой сумме на уровне БД.
     */
    @Modifying
    @Query(value = """
            UPDATE wallet_balances
            SET amount = amount - :amount
            WHERE wallet_id = :walletId 
              AND crypto_id = :cryptoId 
              AND amount >= :amount
            """, nativeQuery = true)
    int deductBalanceNative(@Param("walletId") Long walletId,
                            @Param("cryptoId") Long cryptoId,
                            @Param("amount") BigDecimal amount);

    /**
     * Нативный SQL-запрос: атомарное пополнение баланса.
     * Если запись существует — увеличивает `amount`, иначе вернет 0 (потребуется INSERT).
     */
    @Modifying
    @Query(value = """
            UPDATE wallet_balances
            SET amount = amount + :amount
            WHERE wallet_id = :walletId AND crypto_id = :cryptoId
            """, nativeQuery = true)
    int addBalanceNative(@Param("walletId") Long walletId,
                         @Param("cryptoId") Long cryptoId,
                         @Param("amount") BigDecimal amount);

    @Query("""
        SELECT new com.crypto.exchange.web.dto.response.AggregatedBalanceDto(
            c.id,
            c.symbol,
            c.name,
            SUM(wb.amount)
        )
        FROM WalletBalance wb
        JOIN wb.cryptoCurrency c
        JOIN wb.wallet w
        WHERE w.user.id = :userId
        GROUP BY c.id, c.symbol, c.name
        ORDER BY c.symbol ASC
    """)
    List<AggregatedBalanceDto> getAggregatedBalancesByUserId(@Param("userId") Long userId);
    /**
     * Атомарное списание по externalId монеты.
     */
    @Modifying
    @Query(value = """
            UPDATE wallet_balances wb
            SET amount = wb.amount - :amount
            FROM crypto_currencies c
            WHERE wb.crypto_id = c.id
              AND wb.wallet_id = :walletId
              AND c.external_id = :externalId
              AND wb.amount >= :amount
            """, nativeQuery = true)
    int deductBalanceByExternalIdNative(@Param("walletId") Long walletId,
                                        @Param("externalId") String externalId,
                                        @Param("amount") BigDecimal amount);

    /**
     * Атомарное пополнение по externalId монеты.
     */
    @Modifying
    @Query(value = """
            UPDATE wallet_balances wb
            SET amount = wb.amount + :amount
            FROM crypto_currencies c
            WHERE wb.crypto_id = c.id
              AND wb.wallet_id = :walletId
              AND c.external_id = :externalId
            """, nativeQuery = true)
    int addBalanceByExternalIdNative(@Param("walletId") Long walletId,
                                     @Param("externalId") String externalId,
                                     @Param("amount") BigDecimal amount);

}