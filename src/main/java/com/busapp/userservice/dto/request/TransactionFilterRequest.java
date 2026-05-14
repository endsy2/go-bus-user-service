package com.busapp.userservice.dto.request;

import com.busapp.userservice.model.enums.TransactionStatus;
import com.busapp.userservice.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterRequest {
    private UUID walletId;
    private TransactionType type;
    private TransactionStatus status;
    private String referenceId;
}
