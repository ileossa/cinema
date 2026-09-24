package com.cinema.cinema.domain.model;

import java.math.BigDecimal;

public record BookingItem(
        Long id,
        Long bookingId,
        Long screeningId,
        int seatCount,
        BigDecimal unitPrice
) {
    public BookingItem {
        if (seatCount <= 0) {
            throw new IllegalArgumentException("Seat count must be greater than 0");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
    }

    public BigDecimal subTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(seatCount));
    }
}
