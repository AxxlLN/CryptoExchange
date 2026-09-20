package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для управления кошельками (Wallet).
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Найти все кошельки конкретного пользователя.
     */
    List<Wallet> findAllByUserId(Long userId);

    /**
     * Найти дефолтный (основной) кошелек пользователя.
     */
    Optional<Wallet> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Поиск кошелька по его уникальному адресу (для P2P переводов).
     */
    Optional<Wallet> findByAddress(String address);

    /**
     * Нативный SQL-запрос с пессимистической блокировкой на уровне БД (FOR UPDATE).
     * Используется при финансовом обмене/списании, чтобы избежать Race Condition при параллельных запросах.
     */
    @Query(value = "SELECT * FROM wallets w WHERE w.id = :walletId FOR UPDATE", nativeQuery = true)
    Optional<Wallet> findByIdForUpdateNative(@Param("walletId") Long walletId);
}