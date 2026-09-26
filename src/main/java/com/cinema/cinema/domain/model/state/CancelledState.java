package com.cinema.cinema.domain.model.state;

import com.cinema.cinema.domain.model.BookingFlowContext;

public class CancelledState implements BookingFlowState {

    @Override
    public BookingFlowStateName getName() {
        return BookingFlowStateName.CANCELLED;
    }

    @Override
    public void selectMovie(BookingFlowContext context, Long movieId) {
        throw new IllegalStateException("Booking flow has been cancelled");
    }

    @Override
    public void selectScreening(BookingFlowContext context, Long screeningId) {
        throw new IllegalStateException("Booking flow has been cancelled");
    }

    @Override
    public void selectSeats(BookingFlowContext context, int seatCount) {
        throw new IllegalStateException("Booking flow has been cancelled");
    }

    @Override
    public void authenticate(BookingFlowContext context, Long userId) {
        throw new IllegalStateException("Booking flow has been cancelled");
    }

    @Override
    public void processPayment(BookingFlowContext context, String paymentToken) {
        throw new IllegalStateException("Booking flow has been cancelled");
    }

    @Override
    public void cancel(BookingFlowContext context) {
        // idempotent cancel
    }
}
