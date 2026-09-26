package com.cinema.cinema.domain.model.state;

public enum BookingFlowStateName {
    SELECT_MOVIE,
    SELECT_SCREENING,
    SELECT_SEATS,
    LOGIN_REQUIRED,
    AUTHENTICATED,
    PAYMENT,
    CONFIRMED,
    CANCELLED
}
