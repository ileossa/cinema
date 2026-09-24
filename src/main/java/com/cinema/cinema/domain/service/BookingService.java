package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.domain.port.in.CancelBookingUseCase;
import com.cinema.cinema.domain.port.in.ConfirmBookingPaymentUseCase;
import com.cinema.cinema.domain.port.in.CreateBookingUseCase;
import com.cinema.cinema.domain.port.in.GetUserBookingsUseCase;
import com.cinema.cinema.domain.port.in.ReleaseExpiredBookingsUseCase;
import com.cinema.cinema.domain.port.out.BookingRepositoryOut;
import com.cinema.cinema.domain.port.out.NotificationOut;
import com.cinema.cinema.domain.port.out.PaymentGatewayOut;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingService implements
        CreateBookingUseCase,
        ConfirmBookingPaymentUseCase,
        CancelBookingUseCase,
        ReleaseExpiredBookingsUseCase,
        GetUserBookingsUseCase {

    private static final int EXPIRATION_MINUTES = 15;

    private final BookingRepositoryOut bookingRepositoryOut;
    private final ScreeningRepositoryOut screeningRepositoryOut;
    private final PaymentGatewayOut paymentGatewayOut;
    private final NotificationOut notificationOut;
    private final UserRepositoryOut userRepositoryOut;

    public BookingService(
            BookingRepositoryOut bookingRepositoryOut,
            ScreeningRepositoryOut screeningRepositoryOut,
            PaymentGatewayOut paymentGatewayOut,
            NotificationOut notificationOut,
            UserRepositoryOut userRepositoryOut
    ) {
        this.bookingRepositoryOut = bookingRepositoryOut;
        this.screeningRepositoryOut = screeningRepositoryOut;
        this.paymentGatewayOut = paymentGatewayOut;
        this.notificationOut = notificationOut;
        this.userRepositoryOut = userRepositoryOut;
    }

    public BookingService(BookingRepositoryOut bookingRepositoryOut, ScreeningRepositoryOut screeningRepositoryOut) {
        this(bookingRepositoryOut, screeningRepositoryOut, null, null, null);
    }

    @Override
    public Booking createBooking(Long userId, List<BookingItem> bookingItems) {
        if (bookingItems == null || bookingItems.isEmpty()) {
            throw new IllegalArgumentException("Booking items cannot be empty");
        }

        List<BookingItem> reservedItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BookingItem item : bookingItems) {
            boolean reserved = screeningRepositoryOut.reserveSeats(item.screeningId(), item.seatCount());
            if (!reserved) {
                // Rollback previously reserved seats in this request
                for (BookingItem rollbackItem : reservedItems) {
                    screeningRepositoryOut.releaseSeats(rollbackItem.screeningId(), rollbackItem.seatCount());
                }
                throw new IllegalStateException("Not enough available seats for screening id: " + item.screeningId());
            }
            reservedItems.add(item);
            totalAmount = totalAmount.add(item.subTotal());
        }

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(
                null,
                userId,
                totalAmount,
                BookingStatus.PENDING,
                now.plusMinutes(EXPIRATION_MINUTES),
                now,
                reservedItems
        );

        return bookingRepositoryOut.save(booking);
    }

    @Override
    public Booking confirmBookingPayment(Long bookingId) {
        return confirmBookingPayment(bookingId, null);
    }

    @Override
    public Booking confirmBookingPayment(Long bookingId, String paymentToken) {
        Booking booking = bookingRepositoryOut.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (booking.status() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed");
        }

        if (booking.isExpired()) {
            for (BookingItem item : booking.items()) {
                screeningRepositoryOut.releaseSeats(item.screeningId(), item.seatCount());
            }
            Booking cancelledBooking = new Booking(
                    booking.id(),
                    booking.userId(),
                    booking.totalAmount(),
                    BookingStatus.CANCELLED,
                    booking.expiresAt(),
                    booking.createdAt(),
                    booking.items()
            );
            bookingRepositoryOut.save(cancelledBooking);
            throw new IllegalStateException("Booking has expired and cannot be confirmed");
        }

        if (paymentGatewayOut != null) {
            boolean paymentSuccess = paymentGatewayOut.processPayment(booking.id(), booking.totalAmount(), paymentToken);
            if (!paymentSuccess) {
                for (BookingItem item : booking.items()) {
                    screeningRepositoryOut.releaseSeats(item.screeningId(), item.seatCount());
                }
                Booking cancelledBooking = new Booking(
                        booking.id(),
                        booking.userId(),
                        booking.totalAmount(),
                        BookingStatus.CANCELLED,
                        booking.expiresAt(),
                        booking.createdAt(),
                        booking.items()
                );
                bookingRepositoryOut.save(cancelledBooking);
                throw new IllegalStateException("Payment failed for booking id: " + bookingId);
            }
        }

        Booking confirmedBooking = new Booking(
                booking.id(),
                booking.userId(),
                booking.totalAmount(),
                BookingStatus.CONFIRMED,
                booking.expiresAt(),
                booking.createdAt(),
                booking.items()
        );

        Booking saved = bookingRepositoryOut.save(confirmedBooking);

        if (notificationOut != null && userRepositoryOut != null && booking.userId() != null) {
            userRepositoryOut.findById(booking.userId())
                    .ifPresent(user -> notificationOut.sendBookingConfirmationEmail(user, saved));
        }

        return saved;
    }

    @Override
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepositoryOut.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (booking.status() == BookingStatus.CANCELLED) {
            return booking;
        }

        for (BookingItem item : booking.items()) {
            screeningRepositoryOut.releaseSeats(item.screeningId(), item.seatCount());
        }

        Booking cancelledBooking = new Booking(
                booking.id(),
                booking.userId(),
                booking.totalAmount(),
                BookingStatus.CANCELLED,
                booking.expiresAt(),
                booking.createdAt(),
                booking.items()
        );

        return bookingRepositoryOut.save(cancelledBooking);
    }

    @Override
    public void releaseExpiredBookings() {
        List<Booking> expiredBookings = bookingRepositoryOut.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING,
                LocalDateTime.now()
        );

        for (Booking booking : expiredBookings) {
            for (BookingItem item : booking.items()) {
                screeningRepositoryOut.releaseSeats(item.screeningId(), item.seatCount());
            }

            Booking cancelledBooking = new Booking(
                    booking.id(),
                    booking.userId(),
                    booking.totalAmount(),
                    BookingStatus.CANCELLED,
                    booking.expiresAt(),
                    booking.createdAt(),
                    booking.items()
            );

            bookingRepositoryOut.save(cancelledBooking);
        }
    }

    @Override
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepositoryOut.findByUserId(userId);
    }
}
