package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.BookingFlowContext;

public interface ProcessBookingFlowUseCase {
    BookingFlowContext startFlow();

    BookingFlowContext selectMovie(String flowSessionId, Long movieId);

    BookingFlowContext selectScreening(String flowSessionId, Long screeningId);

    BookingFlowContext selectSeats(String flowSessionId, int seatCount, Long userId);

    BookingFlowContext authenticate(String flowSessionId, Long userId);

    BookingFlowContext authenticate(String flowSessionId, String token);

    BookingFlowContext processPayment(String flowSessionId, String paymentToken, Long authenticatedUserId);

    BookingFlowContext cancelFlow(String flowSessionId);
}
