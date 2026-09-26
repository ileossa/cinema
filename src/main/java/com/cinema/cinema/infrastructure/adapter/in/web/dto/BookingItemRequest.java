package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.BookingItem;

import java.math.BigDecimal;

public record BookingItemRequest(
        Long screeningId,
        int seatCount
) {
    public BookingItem toDomain() {
        return new BookingItem(null, null, screeningId, seatCount, BigDecimal.ZERO);
    }
}
