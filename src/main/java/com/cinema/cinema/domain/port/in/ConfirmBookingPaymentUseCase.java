package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Booking;

public interface ConfirmBookingPaymentUseCase {
    Booking confirmBookingPayment(Long bookingId);
    Booking confirmBookingPayment(Long bookingId, String paymentToken);
}
