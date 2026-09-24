package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long userId,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        List<BookingItemResponse> items
) {
    public static BookingResponse fromDomain(Booking booking) {
        List<BookingItemResponse> items = booking.items() != null
                ? booking.items().stream().map(BookingItemResponse::fromDomain).toList()
                : List.of();

        return new BookingResponse(
                booking.id(),
                booking.userId(),
                booking.totalAmount(),
                booking.status(),
                booking.expiresAt(),
                booking.createdAt(),
                items
        );
    }
}
