package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.*;
import com.cinema.cinema.domain.model.state.BookingFlowStateName;
import com.cinema.cinema.domain.model.state.LoginRequiredState;
import com.cinema.cinema.domain.model.state.PaymentState;
import com.cinema.cinema.domain.port.in.*;
import com.cinema.cinema.domain.port.out.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BookingService implements
        CreateBookingUseCase,
        ConfirmBookingPaymentUseCase,
        CancelBookingUseCase,
        ReleaseExpiredBookingsUseCase,
        GetUserBookingsUseCase,
        ProcessBookingFlowUseCase,
        GetBookingFlowStatusUseCase {

    private static final int EXPIRATION_MINUTES = 15;
    private static final BigDecimal DEFAULT_UNIT_PRICE = new BigDecimal("12.50");

    private final BookingRepositoryOut bookingRepositoryOut;
    private final ScreeningRepositoryOut screeningRepositoryOut;
    private final PaymentGatewayOut paymentGatewayOut;
    private final NotificationOut notificationOut;
    private final UserRepositoryOut userRepositoryOut;
    private final QrCodeGeneratorOut qrCodeGeneratorOut;
    private final MediaStorageOut mediaStorageOut;
    private final JwtTokenProvider jwtTokenProvider;

    private final Map<String, BookingFlowContext> flowSessions = new ConcurrentHashMap<>();

    public BookingService(
            BookingRepositoryOut bookingRepositoryOut,
            ScreeningRepositoryOut screeningRepositoryOut,
            PaymentGatewayOut paymentGatewayOut,
            NotificationOut notificationOut,
            UserRepositoryOut userRepositoryOut,
            QrCodeGeneratorOut qrCodeGeneratorOut,
            MediaStorageOut mediaStorageOut,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.bookingRepositoryOut = bookingRepositoryOut;
        this.screeningRepositoryOut = screeningRepositoryOut;
        this.paymentGatewayOut = paymentGatewayOut;
        this.notificationOut = notificationOut;
        this.userRepositoryOut = userRepositoryOut;
        this.qrCodeGeneratorOut = qrCodeGeneratorOut;
        this.mediaStorageOut = mediaStorageOut;
        this.jwtTokenProvider = jwtTokenProvider != null ? jwtTokenProvider : new JwtTokenProvider();
    }

    public BookingService(
            BookingRepositoryOut bookingRepositoryOut,
            ScreeningRepositoryOut screeningRepositoryOut,
            PaymentGatewayOut paymentGatewayOut,
            NotificationOut notificationOut,
            UserRepositoryOut userRepositoryOut,
            QrCodeGeneratorOut qrCodeGeneratorOut,
            MediaStorageOut mediaStorageOut
    ) {
        this(bookingRepositoryOut, screeningRepositoryOut, paymentGatewayOut, notificationOut, userRepositoryOut, qrCodeGeneratorOut, mediaStorageOut, null);
    }

    public BookingService(
            BookingRepositoryOut bookingRepositoryOut,
            ScreeningRepositoryOut screeningRepositoryOut,
            PaymentGatewayOut paymentGatewayOut,
            NotificationOut notificationOut,
            UserRepositoryOut userRepositoryOut
    ) {
        this(bookingRepositoryOut, screeningRepositoryOut, paymentGatewayOut, notificationOut, userRepositoryOut, null, null, null);
    }

    public BookingService(BookingRepositoryOut bookingRepositoryOut, ScreeningRepositoryOut screeningRepositoryOut) {
        this(bookingRepositoryOut, screeningRepositoryOut, null, null, null, null, null, null);
    }

    @Override
    public Booking createBooking(Long userId, List<BookingItem> bookingItems) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        if (bookingItems == null || bookingItems.isEmpty()) {
            throw new IllegalArgumentException("Booking items cannot be empty");
        }

        List<BookingItem> reservedItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BookingItem item : bookingItems) {
            Optional<Screening> screeningOpt = screeningRepositoryOut.findById(item.screeningId());
            Screening screening = screeningOpt.orElse(null);
            boolean reserved = screeningRepositoryOut.reserveSeats(item.screeningId(), item.seatCount());
            if (!reserved) {
                // Rollback previously reserved seats in this request
                for (BookingItem rollbackItem : reservedItems) {
                    screeningRepositoryOut.releaseSeats(rollbackItem.screeningId(), rollbackItem.seatCount());
                }
                throw new IllegalStateException("Not enough available seats for screening id: " + item.screeningId());
            }
            BigDecimal unitPrice = (screening != null && screening.unitPrice() != null) ? screening.unitPrice() : DEFAULT_UNIT_PRICE;
            BookingItem secureItem = new BookingItem(item.id(), item.bookingId(), item.screeningId(), item.seatCount(), unitPrice);
            reservedItems.add(secureItem);
            totalAmount = totalAmount.add(secureItem.subTotal());
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
        return confirmBookingPaymentInternal(booking, paymentToken, null);
    }

    private Booking confirmBookingPaymentInternal(Booking booking, String paymentToken, BookingFlowContext context) {
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
                throw new IllegalStateException("Payment failed for booking id: " + booking.id());
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

        String qrCodeUrl = null;
        if (mediaStorageOut != null) {
            qrCodeUrl = mediaStorageOut.generateUrlAccesMedia("tickets/booking-" + saved.id() + "-qr.png");
        }
        byte[] qrCodeBytes = null;
        if (qrCodeGeneratorOut != null && saved.userId() != null) {
            qrCodeBytes = qrCodeGeneratorOut.generateQrCodeImage("CINEMA-TICKET:" + saved.id() + ":" + saved.userId(), 250, 250);
        }

        if (context != null) {
            context.setQrCodeUrl(qrCodeUrl);
            context.setQrCodeData(qrCodeBytes);
        }

        if (notificationOut != null && userRepositoryOut != null && booking.userId() != null) {
            final String finalQrUrl = qrCodeUrl;
            final byte[] finalQrBytes = qrCodeBytes;
            userRepositoryOut.findById(booking.userId())
                    .ifPresent(user -> {
                        if (finalQrUrl != null || finalQrBytes != null) {
                            notificationOut.sendBookingConfirmationEmail(user, saved, finalQrUrl, finalQrBytes);
                        } else {
                            notificationOut.sendBookingConfirmationEmail(user, saved);
                        }
                    });
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

    // --- ProcessBookingFlowUseCase & GetBookingFlowStatusUseCase Implementations ---

    @Override
    public BookingFlowContext startFlow() {
        BookingFlowContext context = new BookingFlowContext();
        flowSessions.put(context.getFlowSessionId(), context);
        return context;
    }

    @Override
    public BookingFlowContext getFlowStatus(String flowSessionId) {
        BookingFlowContext context = flowSessions.get(flowSessionId);
        if (context == null) {
            throw new IllegalArgumentException("Booking flow session not found: " + flowSessionId);
        }
        return context;
    }

    @Override
    public BookingFlowContext selectMovie(String flowSessionId, Long movieId) {
        BookingFlowContext context = getOrCreateSession(flowSessionId);
        context.selectMovie(movieId);
        return context;
    }

    @Override
    public BookingFlowContext selectScreening(String flowSessionId, Long screeningId) {
        BookingFlowContext context = getOrCreateSession(flowSessionId);
        Optional<Screening> screeningOpt = screeningRepositoryOut.findById(screeningId);
        screeningOpt.ifPresent(s -> {
            if (s.unitPrice() != null) {
                context.setUnitPrice(s.unitPrice());
            }
        });
        context.selectScreening(screeningId);
        return context;
    }

    @Override
    public BookingFlowContext selectSeats(String flowSessionId, int seatCount, Long userId) {
        BookingFlowContext context = getOrCreateSession(flowSessionId);
        if (userId != null) {
            context.setAuthenticatedUserId(userId);
        }
        context.selectSeats(seatCount);
        if (context.getStateName() == BookingFlowStateName.AUTHENTICATED) {
            holdSeatsAndCreatePendingBooking(context);
        }
        return context;
    }

    @Override
    public BookingFlowContext authenticate(String flowSessionId, Long userId) {
        BookingFlowContext context = getOrCreateSession(flowSessionId);
        if (userId != null) {
            context.authenticate(userId);
            if (context.getStateName() == BookingFlowStateName.AUTHENTICATED) {
                holdSeatsAndCreatePendingBooking(context);
            }
        } else {
            context.setState(new LoginRequiredState());
        }
        return context;
    }

    @Override
    public BookingFlowContext authenticate(String flowSessionId, String token) {
        BookingFlowContext context = getOrCreateSession(flowSessionId);
        if (token != null && !token.isBlank() && jwtTokenProvider != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            context.authenticate(userId);
            if (context.getStateName() == BookingFlowStateName.AUTHENTICATED) {
                holdSeatsAndCreatePendingBooking(context);
            }
        } else {
            context.setState(new LoginRequiredState());
        }
        return context;
    }

    @Override
    public BookingFlowContext processPayment(String flowSessionId, String paymentToken, Long authenticatedUserId) {
        BookingFlowContext context = getOrCreateSession(flowSessionId);
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("User authentication required to process payment");
        }
        if (context.getAuthenticatedUserId() == null || !authenticatedUserId.equals(context.getAuthenticatedUserId())) {
            throw new IllegalArgumentException("Authenticated user does not match booking context");
        }
        Booking booking = context.getBooking();
        if (booking == null) {
            throw new IllegalStateException("No active booking to pay for in session: " + flowSessionId);
        }

        Booking confirmed = confirmBookingPaymentInternal(booking, paymentToken, context);
        context.setBooking(confirmed);
        context.processPayment(paymentToken);
        return context;
    }

    @Override
    public BookingFlowContext cancelFlow(String flowSessionId) {
        BookingFlowContext context = flowSessions.get(flowSessionId);
        if (context != null) {
            if (context.getBooking() != null && context.getBooking().id() != null) {
                cancelBooking(context.getBooking().id());
            }
            context.cancel();
        }
        return context;
    }

    private BookingFlowContext getOrCreateSession(String flowSessionId) {
        if (flowSessionId == null || flowSessionId.isBlank()) {
            return startFlow();
        }
        return flowSessions.computeIfAbsent(flowSessionId, BookingFlowContext::new);
    }

    private void holdSeatsAndCreatePendingBooking(BookingFlowContext context) {
        if (context.getSelectedScreeningId() == null || context.getSelectedSeatCount() == null) {
            return;
        }
        if (context.getBooking() != null) {
            return;
        }

        boolean reserved = screeningRepositoryOut.reserveSeats(context.getSelectedScreeningId(), context.getSelectedSeatCount());
        if (!reserved) {
            throw new IllegalStateException("Not enough available seats for screening id: " + context.getSelectedScreeningId());
        }

        BigDecimal unitPrice = context.getUnitPrice();
        if (unitPrice == null) {
            Optional<Screening> screeningOpt = screeningRepositoryOut.findById(context.getSelectedScreeningId());
            unitPrice = screeningOpt.map(Screening::unitPrice).filter(p -> p != null).orElse(DEFAULT_UNIT_PRICE);
            context.setUnitPrice(unitPrice);
        }
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(context.getSelectedSeatCount()));
        BookingItem item = new BookingItem(null, null, context.getSelectedScreeningId(), context.getSelectedSeatCount(), unitPrice);

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(
                null,
                context.getAuthenticatedUserId(),
                totalAmount,
                BookingStatus.PENDING,
                now.plusMinutes(EXPIRATION_MINUTES),
                now,
                List.of(item)
        );

        Booking savedBooking = bookingRepositoryOut.save(booking);
        context.setBooking(savedBooking);
        context.setState(new PaymentState());
    }
}
