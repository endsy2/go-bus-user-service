package com.busapp.userservice.controller;

import com.busapp.userservice.dto.request.CheckTopUpRequest;
import com.busapp.userservice.dto.request.TopUpBakongRequest;
import com.busapp.userservice.dto.response.ApiResponse;
import com.busapp.userservice.dto.response.TopUpBakongResponse;
import com.busapp.userservice.exception.WalletNotAuthenticatedException;
import com.busapp.userservice.service.BakongTopUpService;
import com.busapp.userservice.service.WalletSessionService;
import com.busapp.userservice.util.UserUtil;
import com.busapp.userservice.util.WalletSessionUtil;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets/top-up/bakong")
@RequiredArgsConstructor
@Slf4j
public class BakongTopUpController {

    private final BakongTopUpService bakongTopUpService;
    private final WalletSessionUtil walletSessionUtil;
    private final UserUtil userUtil;

    /**
     * Generate Bakong KHQR for wallet top-up - Requires wallet session
     * POST /api/wallets/top-up/bakong/generateKHQR
     */
    @PostMapping("/generateKHQR")
    public ResponseEntity<ApiResponse<KHQRResponse<KHQRData>>> generateKhqr(
            @RequestHeader("X-Wallet-Session") String walletSessionToken,
            @Validated @RequestBody TopUpBakongRequest request) {
        
        walletSessionUtil.validateAndRefreshSession(walletSessionToken);
        
        Long userId = userUtil.getCurrentUserId();
        log.info("[API] Generate top-up KHQR - userId={}, amount={}", userId, request.getAmount());
        
        KHQRResponse<KHQRData> khqrResponse = bakongTopUpService.generateTopUpKhqr(userId, request);
        
        ApiResponse<KHQRResponse<KHQRData>> response = ApiResponse.of(
                HttpStatus.OK.value(),
                "KHQR generated successfully",
                khqrResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check top-up transaction status and complete if paid - Requires wallet session
     * POST /api/wallets/top-up/bakong/checking-transaction
     */
    @PostMapping("/checking-transaction")
    public ResponseEntity<ApiResponse<TopUpBakongResponse>> checkingTransaction(
            @RequestHeader("X-Wallet-Session") String walletSessionToken,
            @Validated @RequestBody CheckTopUpRequest checkTopUpRequest) {
        
        walletSessionUtil.validateAndRefreshSession(walletSessionToken);
        
        Long userId = userUtil.getCurrentUserId();
        log.info("[API] Check top-up transaction - userId={}, hash={}", userId, checkTopUpRequest.getHash());
        
        TopUpBakongResponse bakongResponse = bakongTopUpService.checkTopUpTransaction(userId, checkTopUpRequest);
        
        ApiResponse<TopUpBakongResponse> response = ApiResponse.of(
                HttpStatus.OK.value(),
                "Transaction check completed",
                bakongResponse);
        
        return ResponseEntity.ok(response);
    }
}
