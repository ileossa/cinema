package com.cinema.cinema.domain.model.state;

import com.cinema.cinema.domain.model.BookingFlowContext;

public class SelectScreeningState implements BookingFlowState {

    @Override
    public BookingFlowStateName getName() {
        return BookingFlowStateName.SELECT_SCREENING;
    }

    @Override
    public void selectMovie(BookingFlowContext context, Long movieId) {
        if (movieId == null) {
            throw new IllegalArgumentException("Movie id cannot be null");
        }
        context.setSelectedMovieId(movieId);
        context.setSelectedScreeningId(null);
        context.setSelectedSeatCount(null);
    }

    @Override
    public void selectScreening(BookingFlowContext context, Long screeningId) {
        if (screeningId == null) {
            throw new IllegalArgumentException("Screening id cannot be null");
        }
        context.setSelectedScreeningId(screeningId);
        context.setState(new SelectSeatsState());
    }

    @Override
    public void selectSeats(BookingFlowContext context, int seatCount) {
        throw new IllegalStateException("Cannot select seats before selecting a screening");
    }

    @Override
    public void authenticate(BookingFlowContext context, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        context.setAuthenticatedUserId(userId);
    }

    @Override
    public void processPayment(BookingFlowContext context, String paymentToken) {
        throw new IllegalStateException("Cannot process payment in SELECT_SCREENING state");
    }

    @Override
    public void cancel(BookingFlowContext context) {
        context.setState(new CancelledState());
    }
}
