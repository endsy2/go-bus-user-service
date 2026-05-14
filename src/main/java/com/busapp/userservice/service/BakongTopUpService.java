package com.busapp.userservice.service;

import com.busapp.userservice.dto.request.CheckTopUpRequest;
import com.busapp.userservice.dto.request.TopUpBakongRequest;
import com.busapp.userservice.dto.response.TopUpBakongResponse;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongTopUpService {
    
    /**
     * Generate Bakong KHQR for wallet top-up
     */
    KHQRResponse<KHQRData> generateTopUpKhqr(Long userId, TopUpBakongRequest request);
    
    /**
     * Check top-up transaction status
     */
    TopUpBakongResponse checkTopUpTransaction(Long userId, CheckTopUpRequest request);
}
