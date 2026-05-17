package com.busapp.userservice.service.impl;

import com.busapp.userservice.dto.mapper.UserMapper;
import com.busapp.userservice.dto.mapper.WalletMapper;
import com.busapp.userservice.dto.request.AuthRequest;
import com.busapp.userservice.dto.request.CreateUserWalletRequest;
import com.busapp.userservice.dto.request.RefreshTokenRequest;
import com.busapp.userservice.dto.response.AuthResponse;
import com.busapp.userservice.dto.response.RefreshTokenResponse;
import com.busapp.userservice.dto.response.RegisterResponse;
import com.busapp.userservice.dto.request.UserRequest;
import com.busapp.userservice.exception.DuplicateResourceException;
import com.busapp.userservice.exception.ResourceNotFoundException;
import com.busapp.userservice.model.Permission;
import com.busapp.userservice.model.Role;
import com.busapp.userservice.model.User;
import com.busapp.userservice.model.UserWallet;
import com.busapp.userservice.model.enums.Currency;
import com.busapp.userservice.repository.UserRepository;
import com.busapp.userservice.repository.UserWalletRepository;
import com.busapp.userservice.security.JwtUtil;
import com.busapp.userservice.security.RedisTokenService;
import com.busapp.userservice.service.AuthService;
import com.busapp.userservice.service.RolePermissionNameCacheService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository    userRepository;
    private final JwtUtil           jwtUtil;
    private final RedisTokenService redisTokenService;
    private final PasswordEncoder   passwordEncoder;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final UserWalletRepository userWalletRepository;
    private final WalletServiceImpl walletService;
    private final RolePermissionNameCacheService rolePermissionNameCacheService;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RegisterResponse register(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUserName());
        }

        if(userRepository.findByPhone(request.getPhone()).isPresent()){
            throw new DuplicateResourceException("Phone already registered: " + request.getPhone());
        }
        request.setIsEmployee(false);
        User user = userRepository.save(userMapper.toEntity(request));


//        walletService.createWallet(user.getId());

        return RegisterResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password.");
        }

        return issueTokenPair(user);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    public RefreshTokenResponse refresh(String refreshToken) {
        // 1. Validate JWT signature + expiry
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (JwtException e) {
            throw new ResourceNotFoundException("Invalid or expired refresh token.");
        }

        if (!"refresh".equals(claims.get("type"))) {
            throw new ResourceNotFoundException("Provided token is not a refresh token.");
        }

        Long userId = Long.parseLong(claims.getSubject());

        // 2. Verify against Redis (one active refresh token per user)
        String stored = redisTokenService.getRefreshToken(userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new ResourceNotFoundException("Refresh token has been revoked or replaced.");
        }

        // 3. Load user and rotate tokens
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return new RefreshTokenResponse(jwtUtil.generateAccessToken(
                userId
                ,user.getEmail()
                ,user.getUserName(),user.getRoles().stream().map(Role::getName).toList()
                ,user.getRoles().stream().flatMap(role -> role.getPermissions().stream()).map(Permission::getName).toList()));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    public void logout(String accessToken, String refreshToken) {
        // Blacklist the access token for its remaining lifetime
        if (accessToken != null && jwtUtil.isValid(accessToken)) {
            long ttl = jwtUtil.getRemainingTtlMs(accessToken);
            redisTokenService.blacklistAccessToken(accessToken, ttl);

            Long userId = jwtUtil.getUserId(accessToken);
            redisTokenService.deleteRefreshToken(userId);
        }
        // If only refresh token is provided (access token already expired)
        else if (refreshToken != null) {
            try {
                Long userId = jwtUtil.getUserId(refreshToken);
                redisTokenService.deleteRefreshToken(userId);
            } catch (JwtException ignored) {
                // Token already invalid — nothing to revoke
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AuthResponse issueTokenPair(User user) {
        Set<Role> roles = user.getRoles() != null ? user.getRoles() : Collections.emptySet();
        
        // Use cached role and permission names to avoid expensive stream operations
        Set<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        Map<String, List<String>> names = rolePermissionNameCacheService.getRoleAndPermissionNames(roleIds, roles);
        
        List<String> roleNames = names.get("roleNames");
        List<String> permissionNames = names.get("permissionNames");

        String accessToken  = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getUserName(), roleNames, permissionNames);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Store refresh token asynchronously (non-blocking)
        redisTokenService.storeRefreshToken(
                user.getId(), refreshToken, jwtUtil.getRefreshTokenExpiry());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtil.getAccessTokenExpiry())
                .refreshTokenExpiresIn(jwtUtil.getRefreshTokenExpiry())
                .userId(user.getId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .build();
    }
}
