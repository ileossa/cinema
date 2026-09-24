package com.cinema.cinema.infrastructure.adapter.out.notification;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.NotificationOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailNotificationAdapter implements NotificationOut {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    @Override
    public void sendBookingConfirmationEmail(User user, Booking booking) {
        log.info("Sending booking confirmation email to {} for bookingId={}, totalAmount={}",
                user.email(), booking.id(), booking.totalAmount());
    }
}
