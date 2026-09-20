package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Сущность мультивалютного кошелька пользователя.
 * Один пользователь может владеть несколькими такими кошельками.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    /**
     * Уникальный идентификатор кошелька (Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный публичный хэш-адрес кошелька для внешних и внутренних переводов (например: "0x7a9f...").
     */
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String address;

    /**
     * Понятное пользователю название кошелька (например: "Основной", "Сберегательный").
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Флаг главного кошелька по умолчанию (true/false).
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    /**
     * Связь N:1 с пользователем-владельцем (User).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Связь 1:N с балансами конкретных криптовалют в этом кошельке.
     */
    @Builder.Default
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<WalletBalance> balances = new HashSet<>();

    @PrePersist
    public void generateAddress() {
        if (this.address == null) {
            this.address = "0x" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
        }
    }
}