package com.busapp.userservice.service.impl;

import com.busapp.userservice.client.BookingClient;
import com.busapp.userservice.dto.admin.*;
import com.busapp.userservice.exception.BadRequestException;
import com.busapp.userservice.exception.ResourceNotFoundException;
import com.busapp.userservice.model.Role;
import com.busapp.userservice.model.User;
import com.busapp.userservice.model.UserWallet;
import com.busapp.userservice.model.WalletTransaction;
import com.busapp.userservice.model.enums.TransactionStatus;
import com.busapp.userservice.model.enums.TransactionType;
import com.busapp.userservice.model.enums.WalletStatus;
import com.busapp.userservice.repository.RoleRepository;
import com.busapp.userservice.repository.UserRepository;
import com.busapp.userservice.repository.UserWalletRepository;
import com.busapp.userservice.repository.WalletTransactionRepository;
import com.busapp.userservice.service.AdminUserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository            userRepository;
    private final RoleRepository            roleRepository;
    private final UserWalletRepository      userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PasswordEncoder           passwordEncoder;
    private final BookingClient             bookingClient;

    // ── List / Filter ─────────────────────────────────────────────────────────

    @Override
    public PagedResponse<AdminUserResponse> getUsers(AdminUserFilterRequest filter) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // full-text search across userName, fullName, email
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String like = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("userName")), like),
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }

            // active flag filter
            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.getActive()));
            }

            // wallet status filter — left-join so users without a wallet still appear
            if (filter.getWalletStatus() != null && !filter.getWalletStatus().isBlank()) {
                try {
                    WalletStatus ws = WalletStatus.valueOf(filter.getWalletStatus().toUpperCase());
                    Join<Object, Object> walletJoin = root.join("wallet", JoinType.LEFT);
                    predicates.add(cb.equal(walletJoin.get("status"), ws));
                } catch (IllegalArgumentException ignored) {
                    // unknown status value — skip predicate
                }
            }

            // createdAt range filter
            if (filter.getFromDate() != null && !filter.getFromDate().isBlank()) {
                LocalDateTime from = LocalDate.parse(filter.getFromDate()).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (filter.getToDate() != null && !filter.getToDate().isBlank()) {
                LocalDateTime to = LocalDate.parse(filter.getToDate()).atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec, pageable);
        List<AdminUserResponse> content = page.getContent()
                .stream().map(this::toAdminResponse).collect(Collectors.toList());

        return PagedResponse.<AdminUserResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        // Kick off the cross-service stats call in parallel with the DB load.
        // The Feign interceptor reads from RequestContextHolder (thread-local), so we
        // hand the request attributes to the worker thread explicitly.
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<UserBookingStatsResponse> statsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                }
                return fetchBookingStats(userId);
            } finally {
                if (requestAttributes != null) {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        });

        // One round-trip: user + roles + permissions + wallet (no N+1, no lazy wallet hit).
        User user = userRepository.findDetailById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        AdminUserResponse response = toAdminResponse(user);

        // Join with the in-flight Feign call. join() rethrows any unchecked exception.
        response.setBookingStats(statsFuture.join());
        return response;
    }

    // ── Update User Info ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        
        // Update only provided fields
        if (request.getUserName() != null && !request.getUserName().isBlank()) {
            // Check if username is already taken by another user
            if (userRepository.existsByUserNameAndIdNot(request.getUserName(), userId)) {
                throw new BadRequestException("Username already exists");
            }
            user.setUserName(request.getUserName());
        }
        
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getImage() != null) {
            user.setImage(request.getImage());
        }
        
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        
        return toAdminResponse(userRepository.save(user));
    }

    // ── Set Active ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse setActive(Long userId, boolean active) {
        User user = findUser(userId);
        user.setActive(active);
        return toAdminResponse(userRepository.save(user));
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }
        User user = findUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return toAdminResponse(userRepository.save(user));
    }

    // ── Unlink Google ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse unlinkGoogle(Long userId) {
        User user = findUser(userId);
        user.setGoogleId(null);
        return toAdminResponse(userRepository.save(user));
    }

    // ── Assign Roles ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse assignRoles(Long userId, List<String> roleNames) {
        User user = findUser(userId);
        Set<Role> roles = new HashSet<>(roleRepository.findByNameIn(new HashSet<>(roleNames)));
        user.setRoles(roles);
        return toAdminResponse(userRepository.save(user));
    }

    // ── Adjust Wallet ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse adjustWallet(Long userId, WalletAdjustRequest request) {
        UserWallet wallet = userWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        double amount = request.getAmount();
        double before = wallet.getBalance();
        double after;
        TransactionType type;

        if ("ADD".equalsIgnoreCase(request.getOperation())) {
            after = before + amount;
            type = TransactionType.TOP_UP;
        } else if ("DEDUCT".equalsIgnoreCase(request.getOperation())) {
            if (before < amount) {
                throw new BadRequestException("Insufficient wallet balance.");
            }
            after = before - amount;
            type = TransactionType.PAYMENT;
        } else {
            throw new BadRequestException("Operation must be ADD or DEDUCT.");
        }

        wallet.setBalance(after);
        wallet.setLastTransaction(LocalDateTime.now());
        userWalletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .description(request.getReason() != null ? request.getReason() : "Admin adjustment")
                .balanceBefore(before)
                .balanceAfter(after)
                .completedAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(tx);

        return toAdminResponse(findUser(userId));
    }

    // ── Set Wallet Status ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminUserResponse setWalletStatus(Long userId, String status) {
        UserWallet wallet = userWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
        try {
            wallet.setStatus(WalletStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid wallet status: " + status);
        }
        userWalletRepository.save(wallet);
        return toAdminResponse(findUser(userId));
    }

    // ── Delete User ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Fetches lifetime booking + ticket stats from booking-service.
     * Returns a zero-filled object on failure so the detail view stays usable
     * if booking-service is briefly unreachable.
     */
    private UserBookingStatsResponse fetchBookingStats(Long userId) {
        try {
            Map<String, Object> raw = bookingClient.getUserDetailStats(userId);
            if (raw == null) {
                return emptyBookingStats();
            }
            return UserBookingStatsResponse.builder()
                    .totalBookings(asLong(raw.get("totalBookings")))
                    .totalSpent(asBigDecimal(raw.get("totalSpent")))
                    .activeTickets(asLong(raw.get("activeTickets")))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch booking stats for user {}: {}", userId, e.getMessage());
            return emptyBookingStats();
        }
    }

    private UserBookingStatsResponse emptyBookingStats() {
        return UserBookingStatsResponse.builder()
                .totalBookings(0L)
                .totalSpent(BigDecimal.ZERO)
                .activeTickets(0L)
                .build();
    }

    private Long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private BigDecimal asBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    private AdminUserResponse toAdminResponse(User user) {
        AdminUserResponse.AdminUserResponseBuilder builder = AdminUserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .googleId(user.getGoogleId())
                .active(Boolean.TRUE.equals(user.getActive()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            builder.roles(user.getRoles().stream()
                    .map(r -> r.getName()).collect(Collectors.toList()));
            builder.permissions(user.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(p -> p.getName())
                    .distinct()
                    .collect(Collectors.toList()));
        }

        if (user.getWallet() != null) {
            UserWallet wallet = user.getWallet();
            builder.walletBalance(wallet.getBalance())
                    .walletStatus(wallet.getStatus().name())
                    .walletCurrency(wallet.getCurrency().toString());
        }

        return builder.build();
    }
}
