package com.cinema.cinema.domain.model.state;

import com.cinema.cinema.domain.model.BookingFlowContext;

public class SelectSeatsState implements BookingFlowState {

    @Override
    public BookingFlowStateName getName() {
        return BookingFlowStateName.SELECT_SEATS;
    }

    @Override
    public void selectMovie(BookingFlowContext context, Long movieId) {
        if (movieId == null) {
            throw new IllegalArgumentException("Movie id cannot be null");
        }
        context.setSelectedMovieId(movieId);
        context.setSelectedScreeningId(null);
        context.setSelectedSeatCount(null);
        context.setState(new SelectScreeningState());
    }

    @Override
    public void selectScreening(BookingFlowContext context, Long screeningId) {
        if (screeningId == null) {
            throw new IllegalArgumentException("Screening id cannot be null");
        }
        context.setSelectedScreeningId(screeningId);
        context.setSelectedSeatCount(null);
    }

    @Override
    public void selectSeats(BookingFlowContext context, int seatCount) {
        if (seatCount <= 0) {
            throw new IllegalArgumentException("Seat count must be greater than 0");
        }
        context.setSelectedSeatCount(seatCount);
        if (context.getAuthenticatedUserId() == null) {
            context.setState(new LoginRequiredState());
        } else {
            context.setState(new AuthenticatedState());
        }
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
        throw new IllegalStateException("Cannot process payment before selecting seats and authenticating");
    }

    @Override
    public void cancel(BookingFlowContext context) {
        context.setState(new CancelledState());
    }
}
