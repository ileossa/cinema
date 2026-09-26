package com.cinema.cinema.infrastructure.adapter.out.notification;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.MediaStorageOut;
import com.cinema.cinema.domain.port.out.NotificationOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailNotificationAdapter implements NotificationOut {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);
    private final MediaStorageOut mediaStorageOut;

    public EmailNotificationAdapter() {
        this.mediaStorageOut = null;
    }

    public EmailNotificationAdapter(MediaStorageOut mediaStorageOut) {
        this.mediaStorageOut = mediaStorageOut;
    }

    @Override
    public void sendBookingConfirmationEmail(User user, Booking booking, String qrCodeUrl, byte[] qrCodeData) {
        String effectiveQrCodeUrl = qrCodeUrl;
        if (effectiveQrCodeUrl == null && mediaStorageOut != null && booking != null && booking.id() != null) {
            effectiveQrCodeUrl = mediaStorageOut.generateUrlAccesMedia("tickets/booking-" + booking.id() + "-qr.png");
        }
        log.info("Sending booking confirmation email to {} for bookingId={}, totalAmount={}, qrCodeUrl={}",
                user != null ? user.email() : null,
                booking != null ? booking.id() : null,
                booking != null ? booking.totalAmount() : null,
                effectiveQrCodeUrl);
    }
}
