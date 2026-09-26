package com.cinema.cinema.domain.model.state;

import com.cinema.cinema.domain.model.BookingFlowContext;

public interface BookingFlowState {
    BookingFlowStateName getName();

    void selectMovie(BookingFlowContext context, Long movieId);

    void selectScreening(BookingFlowContext context, Long screeningId);

    void selectSeats(BookingFlowContext context, int seatCount);

    void authenticate(BookingFlowContext context, Long userId);

    void processPayment(BookingFlowContext context, String paymentToken);

    void cancel(BookingFlowContext context);
}
