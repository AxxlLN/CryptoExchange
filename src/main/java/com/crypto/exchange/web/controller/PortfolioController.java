package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.PortfolioService;
import com.crypto.exchange.web.dto.response.DetailedPortfolioDto;
import com.crypto.exchange.web.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio Analytics", description = "Управление портфелем, агрегированными активами и PnL аналитикой")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("isAuthenticated()")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "Получить детальный портфель текущего пользователя")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение детальной аналитики портфеля (балансы по монетам, USD эквиваленты и PnL)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DetailedPortfolioDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<DetailedPortfolioDto> getPortfolio(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        DetailedPortfolioDto portfolio = portfolioService.getDetailedPortfolio(currentUser.getId());
        return ResponseEntity.ok(portfolio);
    }
}