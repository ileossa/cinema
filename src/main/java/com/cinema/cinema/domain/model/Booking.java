package com.cinema.cinema.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Booking(
        Long id,
        Long userId,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        List<BookingItem> items
) {
    public Booking {
        if (null == userId) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        if (null == status) {
            status = BookingStatus.PENDING;
        }
        if (null == items || items.isEmpty()) {
            throw new IllegalArgumentException("Booking items cannot be empty");
        }
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
    }

    public boolean isExpired() {
        return status == BookingStatus.PENDING && expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
