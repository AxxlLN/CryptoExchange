package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.exception.BadRequestException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.UserMapper;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.service.UserService;
import com.crypto.exchange.web.dto.request.ChangePasswordRequest;
import com.crypto.exchange.web.dto.request.UpdateAvatarRequest;
import com.crypto.exchange.web.dto.request.UpdateProfileRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto getUserProfile(Long userId) {
        User user = findUserById(userId);
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public void deleteOwnProfile(Long userId) {
        User user = findUserById(userId);

        if (walletBalanceRepository.hasPositiveBalances(userId)) {
            throw new BadRequestException("Cannot delete account with active funds. Please withdraw your balance first.");
        }

        user.setDeleted(true);
        user.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setEmail("deleted_" + UUID.randomUUID() + "@deleted.local");
        user.setUsername("deleted_user_" + user.getId());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setAvatarUrl(null);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateAvatar(Long userId, UpdateAvatarRequest request) {
        User user = findUserById(userId);
        user.setAvatarUrl(request.avatarUrl());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Имя пользователя '" + request.username() + "' уже занято");
        }

        user.setUsername(request.username());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Неверный текущий пароль");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("Новый пароль не должен совпадать со старым паролем");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));
    }
}