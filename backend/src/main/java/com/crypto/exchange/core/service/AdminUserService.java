package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.response.UserResponseDto;

import java.util.List;

public interface AdminUserService {

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUserById(Long id);

    void blockUserByUsername(String username);

    void unblockUserByUsername(String username);

    void deleteUser(Long id);
}