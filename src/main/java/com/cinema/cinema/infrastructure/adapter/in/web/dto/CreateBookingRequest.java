package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import java.util.List;

public record CreateBookingRequest(
        Long userId,
        List<BookingItemRequest> items
) {
}
