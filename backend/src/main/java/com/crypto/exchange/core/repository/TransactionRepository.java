package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий истории транзакций (Transaction).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Получить историю транзакций пользователя с пагинацией.
     */
    Page<Transaction> findAllByUserId(Long userId, Pageable pageable);

    /**
     * Нативный SQL-запрос: топ-5 самых крупных транзакций пользователя по эквиваленту в USD за всё время.
     */
    @Query(value = """
            SELECT t.* 
            FROM transactions t
            LEFT JOIN crypto_currencies cc ON cc.id = t.from_crypto_id
            LEFT JOIN wallets fw ON fw.id = t.from_wallet_id
            LEFT JOIN wallets tw ON tw.id = t.to_wallet_id
            WHERE fw.user_id = :userId OR tw.user_id = :userId
            ORDER BY (t.from_amount * COALESCE(cc.price_usd, 0)) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Transaction> findTopSubstantialTransactionsNative(@Param("userId") Long userId, @Param("limit") int limit);

    @Query("""
            SELECT DISTINCT t FROM Transaction t
            LEFT JOIN t.fromWallet fw
            LEFT JOIN t.toWallet tw
            WHERE fw.user.id = :userId OR tw.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findAllByUserIdInvolvedList(@Param("userId") Long userId);

    /**
     * Поиск транзакций пользователя с пагинацией:
     * находит транзакции, где пользователь является владельцем fromWallet ИЛИ toWallet.
     */
    @Query("""
        SELECT DISTINCT t FROM Transaction t
        LEFT JOIN t.fromWallet fw
        LEFT JOIN t.toWallet tw
        WHERE fw.user.id = :userId OR tw.user.id = :userId
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> findAllByUserIdInvolved(@Param("userId") Long userId, Pageable pageable);

}