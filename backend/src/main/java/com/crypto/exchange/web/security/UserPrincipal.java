package com.crypto.exchange.web.security;

import com.crypto.exchange.core.entity.Role;
import com.crypto.exchange.core.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean blocked;
    private final boolean deleted;

    public UserPrincipal(Long id, String username, String email, String password, Role role, boolean blocked, boolean deleted) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority(role.name()));
        this.blocked = blocked;
        this.deleted = deleted;
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isBlocked(),
                user.isDeleted()
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !blocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !deleted && !blocked;
    }
}