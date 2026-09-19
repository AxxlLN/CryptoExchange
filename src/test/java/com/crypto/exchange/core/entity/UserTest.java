package com.crypto.exchange.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("addWallet должен добавлять кошелек в список и устанавливать обратную ссылку на пользователя")
    void addWalletShouldAddWalletAndSetUserReference() {
        User user = new User();
        Wallet wallet = new Wallet();

        user.addWallet(wallet);

        assertThat(user.getWallets()).contains(wallet);
        assertThat(wallet.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("removeWallet должен удалять кошелек из списка и обнулять ссылку на пользователя")
    void removeWalletShouldRemoveWalletAndClearUserReference() {
        User user = new User();
        Wallet wallet = new Wallet();
        user.addWallet(wallet);

        user.removeWallet(wallet);

        assertThat(user.getWallets()).doesNotContain(wallet);
        assertThat(wallet.getUser()).isNull();
    }

    @Test
    @DisplayName("onCreate должен проставлять createdAt в UTC и роль по умолчанию ROLE_USER, если она не задана")
    void onCreateShouldSetCreatedAtAndDefaultRoleWhenRoleIsNull() {
        User user = new User();

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("onCreate не должен перезаписывать уже существующую роль")
    void onCreate_ShouldPreserveExistingRoleWhenRoleIsAlreadySet() {
        User user = User.builder()
                .role(Role.ROLE_ADMIN)
                .build();

        user.onCreate();

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("equals и hashCode должны сравнивать сущности по id")
    void equalsAndHashCodeShouldBeBasedOnId() {
        User user1 = User.builder().id(1L).username("alice").build();
        User user2 = User.builder().id(1L).username("bob").build();
        User user3 = User.builder().id(2L).username("alice").build();

        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());

        assertThat(user1).isNotEqualTo(user3);
    }
}