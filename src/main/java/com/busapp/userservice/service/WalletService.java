package com.busapp.userservice.service;

import com.busapp.userservice.dto.admin.PagedResponse;
import com.busapp.userservice.dto.request.TransactionFilterRequest;
import com.busapp.userservice.dto.request.WalletFilterRequest;
import com.busapp.userservice.dto.request.WalletLoginRequest;
import com.busapp.userservice.dto.response.WalletBalanceResponse;
import com.busapp.userservice.dto.response.WalletResponse;
import com.busapp.userservice.dto.response.WalletTransactionResponse;
import com.busapp.userservice.model.enums.TransactionType;

import java.util.UUID;

public interface WalletService {

    WalletResponse createWallet(WalletLoginRequest walletLoginRequest);

    WalletResponse userCurrentWallet();

    WalletResponse getWalletById(UUID walletId);

    WalletResponse getWalletByUserId(Long userId);

    PagedResponse<WalletResponse> getWallets(WalletFilterRequest filter, int page, int size);

    PagedResponse<WalletTransactionResponse> getTransactions(TransactionFilterRequest filter, int page, int size);

    WalletTransactionResponse doTransactionInternal(Long userId, String walletSessionToken, TransactionType transactionType, Double amount);

    WalletResponse walletLogin(WalletLoginRequest walletLoginRequest);

    WalletBalanceResponse getCurrentUserBalance(Long userId);

    WalletTransactionResponse refundTransaction(Long userId, Double amount, String description);
}
