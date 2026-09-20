package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.request.ChangePasswordRequest;
import com.crypto.exchange.web.dto.request.UpdateAvatarRequest;
import com.crypto.exchange.web.dto.request.UpdateProfileRequest;
import com.crypto.exchange.web.dto.response.UserResponseDto;
import java.util.List;

public interface UserService {
    UserResponseDto getUserProfile(Long userId);;

    void deleteOwnProfile(Long userId);

    UserResponseDto updateAvatar(Long userId, UpdateAvatarRequest request);

    UserResponseDto updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}