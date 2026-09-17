package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Промежуточная сущность для связи Many-to-Many между Wallet и CryptoCurrency.
 * Хранит конкретный остаток (количество) определенной криптовалюты в кошельке.
 */
@Entity
@Table(name = "wallet_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "crypto_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletBalance {

    /**
     * Уникальный идентификатор записи баланса (Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Связь N:1 с кошельком (Wallet), которому принадлежит данный баланс.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * Связь N:1 с криптовалютой (CryptoCurrency), к которой относится данный баланс.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crypto_id", nullable = false)
    private CryptoCurrency cryptoCurrency;

    /**
     * Количество монеты на счету с точностью до 8 знаков (криптографическая точность).
     */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;
}