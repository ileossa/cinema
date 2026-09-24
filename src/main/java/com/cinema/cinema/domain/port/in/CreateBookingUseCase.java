package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;

import java.util.List;

public interface CreateBookingUseCase {
    Booking createBooking(Long userId, List<BookingItem> bookingItems);
}
