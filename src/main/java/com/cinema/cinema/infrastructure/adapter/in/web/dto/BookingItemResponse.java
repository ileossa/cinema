package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.BookingItem;

import java.math.BigDecimal;

public record BookingItemResponse(
        Long id,
        Long screeningId,
        int seatCount,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {
    public static BookingItemResponse fromDomain(BookingItem item) {
        return new BookingItemResponse(
                item.id(),
                item.screeningId(),
                item.seatCount(),
                item.unitPrice(),
                item.subTotal()
        );
    }
}
