package com.busapp.userservice.dto.response;

import com.busapp.userservice.model.enums.Currency;
import com.busapp.userservice.model.enums.TopUpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpBakongResponse {
    private Long topUpId;
    private String qrCode;
    private String md5;
    private Double amount;
    private Currency currency;
    private TopUpStatus status;
    private String message;
    private int responseCode;
}
