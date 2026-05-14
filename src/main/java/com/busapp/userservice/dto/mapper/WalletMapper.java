package com.busapp.userservice.dto.mapper;

import com.busapp.userservice.dto.request.CreateUserWalletRequest;
import com.busapp.userservice.dto.response.WalletResponse;
import com.busapp.userservice.dto.response.WalletTransactionResponse;
import com.busapp.userservice.model.User;
import com.busapp.userservice.model.UserWallet;
import com.busapp.userservice.model.WalletTransaction;
import com.busapp.userservice.model.enums.Currency;
import com.busapp.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@RequiredArgsConstructor
public class WalletMapper {
    private final UserRepository userRepository;

    public WalletResponse toResponse(UserWallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setId(wallet.getId());
        response.setUserId(wallet.getUser().getId());
        response.setUserName(wallet.getUser().getUserName());
        response.setFullName(wallet.getUser().getFullName());
        response.setBalance(wallet.getBalance());
        response.setCurrency(wallet.getCurrency());
        response.setStatus(wallet.getStatus());
        response.setLastTransaction(wallet.getLastTransaction());
        response.setCreatedAt(wallet.getCreatedAt());
        response.setUpdatedAt(wallet.getUpdatedAt());
        return response;
    }
    public WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        WalletTransactionResponse response = new WalletTransactionResponse();
        response.setId(tx.getId());
        response.setWalletId(tx.getWallet().getId());
        response.setAmount(tx.getAmount());
        response.setType(tx.getType());
        response.setStatus(tx.getStatus());
        response.setReferenceId(tx.getReferenceId());
        response.setDescription(tx.getDescription());
        response.setBalanceBefore(tx.getBalanceBefore());
        response.setBalanceAfter(tx.getBalanceAfter());
        response.setMetadata(tx.getMetadata());
        response.setCreatedAt(tx.getCreatedAt());
        response.setCompletedAt(tx.getCompletedAt());
        return response;
    }
    public UserWallet toEntity(CreateUserWalletRequest request){
        return UserWallet.builder()
                .user(request.getUser())
                .balance(request.getAmount())
                .currency(Currency.USD)
                .build();
    }
}
