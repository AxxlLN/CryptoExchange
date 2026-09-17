package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.response.UserResponseDto;
import java.util.List;

public interface UserService {
    UserResponseDto getUserProfile(Long userId);
    List<UserResponseDto> getAllUsers();
    UserResponseDto getUserById(Long id);
    void deleteUser(Long id);
}