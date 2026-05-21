package com.busapp.userservice.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class BakongCheckTopUpResponse {
    private Double amount;
    private String currency;
    private String transactionId;
    private Long status;
    private Instant paidAt;


}
