package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.exception.BadRequestException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.UserMapper;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.service.AdminUserService;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = findUserById(id);
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public void blockUserByUsername(String username) {
        User user = findUserByUsername(username);
        user.setBlocked(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unblockUserByUsername(String username) {
        User user = findUserByUsername(username);
        user.setBlocked(false);
        userRepository.save(user);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с именем '" + username + "' не найден"));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserById(id);

        if (walletBalanceRepository.hasPositiveBalances(id)) {
            throw new BadRequestException("Нельзя удалить аккаунт с активным балансом.");
        }

        user.setDeleted(true);
        user.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setEmail("deleted_" + UUID.randomUUID() + "@deleted.local");
        user.setUsername("deleted_user_" + user.getId());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setAvatarUrl(null);

        userRepository.save(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));
    }
}