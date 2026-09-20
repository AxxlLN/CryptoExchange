package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Сущность пользователя системы.
 * Содержит учетные данные и список привязанных кошельков.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Уникальный идентификатор пользователя (Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальное имя пользователя для входа в систему.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Уникальный адрес электронной почты.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Хешированный пароль пользователя (BCrypt).
     */
    @Column(nullable = false)
    private String password;

    /**
     * Роль пользователя в системе для RBAC.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Аватар пользователя
     */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Статус аккаунта (удален - не удален)
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * Время удаления
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Блокировка аккаунта
     */
    @Builder.Default
    @Column(name = "is_blocked", nullable = false)
    private boolean blocked = false;

    /**
     * Дата и время создания аккаунта.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Связь 1:N — Пользователь может иметь несколько кошельков.
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Wallet> wallets = new ArrayList<>();

    /**
     * Связь 1:N с историей транзакций пользователя.
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.role == null) {
            this.role = Role.ROLE_USER;
        }
    }

    /**
     * Методы для управления двунаправленной связью
     * @param wallet
     */
    public void addWallet(Wallet wallet) {
        wallets.add(wallet);
        wallet.setUser(this);
    }

    public void removeWallet(Wallet wallet) {
        wallets.remove(wallet);
        wallet.setUser(null);
    }

    /**
     * Методы equals и hashcode (для предотвращения преждевременной инициализации)
     * @param o   the reference object with which to compare.
     * @return
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        User user = (User) o;
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}