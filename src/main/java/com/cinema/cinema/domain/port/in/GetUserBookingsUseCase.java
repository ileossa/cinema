package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Booking;

import java.util.List;

public interface GetUserBookingsUseCase {
    List<Booking> getUserBookings(Long userId);
}
