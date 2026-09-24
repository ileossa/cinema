package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Booking;

public interface CancelBookingUseCase {
    Booking cancelBooking(Long bookingId);
}
