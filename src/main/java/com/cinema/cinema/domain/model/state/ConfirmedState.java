package com.cinema.cinema.domain.model.state;

import com.cinema.cinema.domain.model.BookingFlowContext;

public class ConfirmedState implements BookingFlowState {

    @Override
    public BookingFlowStateName getName() {
        return BookingFlowStateName.CONFIRMED;
    }

    @Override
    public void selectMovie(BookingFlowContext context, Long movieId) {
        throw new IllegalStateException("Booking flow is already confirmed");
    }

    @Override
    public void selectScreening(BookingFlowContext context, Long screeningId) {
        throw new IllegalStateException("Booking flow is already confirmed");
    }

    @Override
    public void selectSeats(BookingFlowContext context, int seatCount) {
        throw new IllegalStateException("Booking flow is already confirmed");
    }

    @Override
    public void authenticate(BookingFlowContext context, Long userId) {
        throw new IllegalStateException("Booking flow is already confirmed");
    }

    @Override
    public void processPayment(BookingFlowContext context, String paymentToken) {
        throw new IllegalStateException("Booking flow is already confirmed");
    }

    @Override
    public void cancel(BookingFlowContext context) {
        throw new IllegalStateException("Booking flow is already confirmed");
    }
}
