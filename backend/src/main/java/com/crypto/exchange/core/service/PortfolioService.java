package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.response.DetailedPortfolioDto;

public interface PortfolioService {
    DetailedPortfolioDto getDetailedPortfolio(Long userId);
    void takeSnapshot(Long userId);
}