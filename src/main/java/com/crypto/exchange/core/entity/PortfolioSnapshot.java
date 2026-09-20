package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshots", indexes = {
        @Index(name = "idx_user_recorded_at", columnList = "user_id, recorded_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_balance_usd", nullable = false, precision = 18, scale = 8)
    private BigDecimal totalBalanceUsd;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}