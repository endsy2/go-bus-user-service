package com.busapp.userservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingStatsResponse {
    private Long totalBookings;
    private Long confirmedBookings;
    private Long cancelledBookings;
    private Long refundedBookings;
    private Long pendingBookings;
    private BigDecimal totalSpent;
    private BigDecimal averageBookingValue;
    private Long activeTickets;
    private Long usedTickets;
    private LocalDateTime firstBookingDate;
    private LocalDateTime lastBookingDate;
}
