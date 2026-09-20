package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;

public interface TransferService {
    TransferResponseDto transfer(Long userId, TransferRequestDto request);
}