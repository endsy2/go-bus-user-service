package com.busapp.userservice.controller;

import com.busapp.userservice.dto.response.ApiResponse;
import com.busapp.userservice.dto.request.TopUpRequest;
import com.busapp.userservice.dto.response.TopUpResponse;
import com.busapp.userservice.model.enums.TopUpStatus;
import com.busapp.userservice.service.TopUpService;
import com.busapp.userservice.util.UserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topups")
@RequiredArgsConstructor
public class TopUpController {

    private final TopUpService topUpService;
    private final UserUtil userUtil;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TopUpResponse>>> getByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.of(
                "TopUps retrieved successfully",
                topUpService.getByUserId(userId)));
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<ApiResponse<List<TopUpResponse>>> getByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable TopUpStatus status) {
        return ResponseEntity.ok(ApiResponse.of(
                "TopUps retrieved successfully",
                topUpService.getByUserIdAndStatus(userId, status)));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<TopUpResponse>> getByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(ApiResponse.of(
                "TopUp retrieved successfully",
                topUpService.getByTransactionId(transactionId)));
    }

    @PostMapping("/user")
    public ResponseEntity<ApiResponse<TopUpResponse>> createTopUp(
            @Valid @RequestBody TopUpRequest request) {
        Long userId=userUtil.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "TopUp created successfully",
                        topUpService.createTopUp(userId, request)));
    }
}
