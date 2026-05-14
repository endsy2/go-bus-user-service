package com.busapp.userservice.dto.response;

import com.busapp.userservice.model.enums.Currency;
import com.busapp.userservice.model.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID          id;
    private Long          userId;
    private String        userName;
    private String        fullName;
    private Double        balance;
    private Currency      currency;
    private WalletStatus  status;
    private LocalDateTime lastTransaction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String        walletSessionToken; // Session token for wallet operations
}
