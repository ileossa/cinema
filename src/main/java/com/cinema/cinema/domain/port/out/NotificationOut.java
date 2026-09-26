package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.User;

public interface NotificationOut {
    void sendBookingConfirmationEmail(User user, Booking booking, String qrCodeUrl, byte[] qrCodeData);

    default void sendBookingConfirmationEmail(User user, Booking booking) {
        sendBookingConfirmationEmail(user, booking, null, null);
    }
}
