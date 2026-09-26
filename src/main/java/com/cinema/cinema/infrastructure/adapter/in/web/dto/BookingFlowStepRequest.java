package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

public record BookingFlowStepRequest(
        String flowSessionId,
        Long movieId,
        Long screeningId,
        Integer seatCount,
        BigDecimal unitPrice,
        Long userId,
        String token,
        String paymentToken
) {
}
