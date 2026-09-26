package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.*;
import com.cinema.cinema.domain.model.state.BookingFlowStateName;
import com.cinema.cinema.domain.port.out.*;
import com.cinema.cinema.domain.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class BookingServiceTest {

    private BookingRepositoryOut bookingRepositoryMock;
    private ScreeningRepositoryOut screeningRepositoryMock;
    private PaymentGatewayOut paymentGatewayMock;
    private NotificationOut notificationMock;
    private UserRepositoryOut userRepositoryMock;
    private QrCodeGeneratorOut qrCodeGeneratorMock;
    private MediaStorageOut mediaStorageMock;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepositoryMock = mock(BookingRepositoryOut.class);
        screeningRepositoryMock = mock(ScreeningRepositoryOut.class);
        paymentGatewayMock = mock(PaymentGatewayOut.class);
        notificationMock = mock(NotificationOut.class);
        userRepositoryMock = mock(UserRepositoryOut.class);
        qrCodeGeneratorMock = mock(QrCodeGeneratorOut.class);
        mediaStorageMock = mock(MediaStorageOut.class);

        bookingService = new BookingService(
                bookingRepositoryMock,
                screeningRepositoryMock,
                paymentGatewayMock,
                notificationMock,
                userRepositoryMock,
                qrCodeGeneratorMock,
                mediaStorageMock
        );
    }

    @Test
    void should_create_booking_successfully() {
        // GIVEN
        Long userId = 1L;
        BookingItem item1 = new BookingItem(null, null, 10L, 2, new BigDecimal("12.50"));
        BookingItem item2 = new BookingItem(null, null, 20L, 1, new BigDecimal("10.00"));
        List<BookingItem> items = List.of(item1, item2);

        Screening s1 = new Screening(10L, 1L, LocalDateTime.now().plusDays(1), "Salle 1", 50, new BigDecimal("12.50"));
        Screening s2 = new Screening(20L, 1L, LocalDateTime.now().plusDays(1), "Salle 2", 50, new BigDecimal("10.00"));

        when(screeningRepositoryMock.findById(10L)).thenReturn(Optional.of(s1));
        when(screeningRepositoryMock.findById(20L)).thenReturn(Optional.of(s2));
        when(screeningRepositoryMock.reserveSeats(10L, 2)).thenReturn(true);
        when(screeningRepositoryMock.reserveSeats(20L, 1)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Booking result = bookingService.createBooking(userId, items);

        // THEN
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(new BigDecimal("35.00"), result.totalAmount());
        assertEquals(BookingStatus.PENDING, result.status());
        assertNotNull(result.expiresAt());
        assertEquals(2, result.items().size());

        verify(screeningRepositoryMock).reserveSeats(10L, 2);
        verify(screeningRepositoryMock).reserveSeats(20L, 1);
        verify(bookingRepositoryMock).save(any(Booking.class));
    }

    @Test
    void should_rollback_reserved_seats_if_subsequent_reservation_fails() {
        // GIVEN
        Long userId = 1L;
        BookingItem item1 = new BookingItem(null, null, 10L, 2, new BigDecimal("12.50"));
        BookingItem item2 = new BookingItem(null, null, 20L, 5, new BigDecimal("10.00"));
        List<BookingItem> items = List.of(item1, item2);

        when(screeningRepositoryMock.reserveSeats(10L, 2)).thenReturn(true);
        when(screeningRepositoryMock.reserveSeats(20L, 5)).thenReturn(false);

        // WHEN & THEN
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(userId, items)
        );

        assertTrue(exception.getMessage().contains("Not enough available seats"));
        verify(screeningRepositoryMock).reserveSeats(10L, 2);
        verify(screeningRepositoryMock).reserveSeats(20L, 5);
        verify(screeningRepositoryMock).releaseSeats(10L, 2);
        verify(bookingRepositoryMock, never()).save(any());
    }

    @Test
    void should_confirm_payment_when_pending_and_not_expired() {
        // GIVEN
        Long bookingId = 100L;
        Long userId = 1L;
        Booking pendingBooking = new Booking(
                bookingId,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, bookingId, 10L, 2, new BigDecimal("12.50")))
        );

        User user = new User(userId, "user@cinema.com", "Test User", LocalDateTime.now());

        when(bookingRepositoryMock.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayMock.processPayment(eq(bookingId), eq(new BigDecimal("25.00")), any())).thenReturn(true);
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));

        // WHEN
        Booking confirmed = bookingService.confirmBookingPayment(bookingId);

        // THEN
        assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        verify(paymentGatewayMock).processPayment(bookingId, new BigDecimal("25.00"), null);
        verify(bookingRepositoryMock).save(any(Booking.class));
        verify(notificationMock).sendBookingConfirmationEmail(eq(user), any(Booking.class));
    }

    @Test
    void should_confirm_booking_and_send_email_when_stripe_payment_succeeds() {
        // GIVEN
        Long bookingId = 100L;
        Long userId = 1L;
        String paymentToken = "tok_visa";
        Booking pendingBooking = new Booking(
                bookingId,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, bookingId, 10L, 2, new BigDecimal("12.50")))
        );

        User user = new User(userId, "alice@cinema.com", "Alice Dupont", LocalDateTime.now());

        when(bookingRepositoryMock.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayMock.processPayment(bookingId, new BigDecimal("25.00"), paymentToken)).thenReturn(true);
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));

        // WHEN
        Booking confirmed = bookingService.confirmBookingPayment(bookingId, paymentToken);

        // THEN
        assertNotNull(confirmed);
        assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        verify(paymentGatewayMock).processPayment(bookingId, new BigDecimal("25.00"), paymentToken);
        verify(notificationMock).sendBookingConfirmationEmail(eq(user), any(Booking.class));
    }

    @Test
    void should_not_confirm_booking_when_payment_fails() {
        // GIVEN
        Long bookingId = 100L;
        Long userId = 1L;
        String paymentToken = "tok_chargeDeclined";
        Booking pendingBooking = new Booking(
                bookingId,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, bookingId, 10L, 2, new BigDecimal("12.50")))
        );

        when(bookingRepositoryMock.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(paymentGatewayMock.processPayment(bookingId, new BigDecimal("25.00"), paymentToken)).thenReturn(false);

        // WHEN & THEN
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.confirmBookingPayment(bookingId, paymentToken)
        );

        assertTrue(exception.getMessage().contains("Payment failed"));
        verify(screeningRepositoryMock).releaseSeats(10L, 2);
        verify(bookingRepositoryMock).save(argThat(b -> b.status() == BookingStatus.CANCELLED));
        verify(notificationMock, never()).sendBookingConfirmationEmail(any(), any());
    }

    @Test
    void should_release_seats_when_booking_expires() {
        // GIVEN
        Long bookingId = 100L;
        Booking expiredBooking = new Booking(
                bookingId,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().minusMinutes(20),
                List.of(new BookingItem(1L, bookingId, 10L, 2, new BigDecimal("12.50")))
        );

        when(bookingRepositoryMock.findById(bookingId)).thenReturn(Optional.of(expiredBooking));

        // WHEN & THEN
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.confirmBookingPayment(bookingId)
        );

        assertTrue(exception.getMessage().contains("Booking has expired"));
        verify(screeningRepositoryMock).releaseSeats(10L, 2);
        verify(bookingRepositoryMock).save(argThat(b -> b.status() == BookingStatus.CANCELLED));
        verify(paymentGatewayMock, never()).processPayment(any(), any(), any());
        verify(notificationMock, never()).sendBookingConfirmationEmail(any(), any());
    }

    @Test
    void should_cancel_booking_and_release_seats() {
        // GIVEN
        Long bookingId = 100L;
        Booking booking = new Booking(
                bookingId,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, bookingId, 10L, 2, new BigDecimal("12.50")))
        );

        when(bookingRepositoryMock.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Booking cancelled = bookingService.cancelBooking(bookingId);

        // THEN
        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        verify(screeningRepositoryMock).releaseSeats(10L, 2);
        verify(bookingRepositoryMock).save(any(Booking.class));
    }

    @Test
    void should_release_expired_bookings() {
        // GIVEN
        Booking expired1 = new Booking(
                101L,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().minusMinutes(17),
                List.of(new BookingItem(1L, 101L, 10L, 2, new BigDecimal("12.50")))
        );

        when(bookingRepositoryMock.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(expired1));

        // WHEN
        bookingService.releaseExpiredBookings();

        // THEN
        verify(screeningRepositoryMock).releaseSeats(10L, 2);
        verify(bookingRepositoryMock).save(argThat(b -> b.status() == BookingStatus.CANCELLED));
    }

    @Test
    void should_get_user_bookings() {
        // GIVEN
        Long userId = 1L;
        Booking b = new Booking(
                100L,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.CONFIRMED,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, 100L, 10L, 2, new BigDecimal("12.50")))
        );

        when(bookingRepositoryMock.findByUserId(userId)).thenReturn(List.of(b));

        // WHEN
        List<Booking> bookings = bookingService.getUserBookings(userId);

        // THEN
        assertEquals(1, bookings.size());
        assertEquals(100L, bookings.get(0).id());
    }

    @Test
    void should_require_booking_to_be_in_pending_status_before_payment() {
        // GIVEN
        Long bookingId = 100L;
        Long userId = 1L;
        Booking alreadyConfirmedBooking = new Booking(
                bookingId,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.CONFIRMED,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, bookingId, 10L, 2, new BigDecimal("12.50")))
        );

        when(bookingRepositoryMock.findById(bookingId)).thenReturn(Optional.of(alreadyConfirmedBooking));

        // WHEN & THEN
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.confirmBookingPayment(bookingId)
        );

        assertEquals("Only PENDING bookings can be confirmed", exception.getMessage());
        verify(paymentGatewayMock, never()).processPayment(any(), any(), any());
        verify(notificationMock, never()).sendBookingConfirmationEmail(any(), any());
    }

    @Test
    void should_require_user_to_be_connected_to_block_seats_for_15_minutes() {
        // GIVEN
        Long unauthenticatedUserId = null;
        BookingItem item = new BookingItem(null, null, 10L, 2, new BigDecimal("12.50"));
        List<BookingItem> items = List.of(item);

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking(unauthenticatedUserId, items)
        );

        assertEquals("User id cannot be null", exception.getMessage());
        verify(screeningRepositoryMock, never()).reserveSeats(anyLong(), anyInt());
        verify(bookingRepositoryMock, never()).save(any());
    }

    @Test
    void should_ensure_seats_are_not_blocked_for_less_than_15_minutes() {
        // GIVEN
        Long userId = 1L;
        BookingItem item = new BookingItem(null, null, 10L, 2, new BigDecimal("12.50"));
        List<BookingItem> items = List.of(item);

        when(screeningRepositoryMock.reserveSeats(10L, 2)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Booking createdBooking = bookingService.createBooking(userId, items);

        // THEN
        assertNotNull(createdBooking.expiresAt());
        assertNotNull(createdBooking.createdAt());
        assertFalse(createdBooking.expiresAt().isBefore(createdBooking.createdAt().plusMinutes(15)));
        long minutesBetween = Duration.between(createdBooking.createdAt(), createdBooking.expiresAt()).toMinutes();
        assertTrue(minutesBetween >= 15);
    }

    @Test
    void should_ensure_seats_are_not_blocked_for_more_than_15_minutes() {
        // GIVEN
        Long userId = 1L;
        BookingItem item = new BookingItem(null, null, 10L, 2, new BigDecimal("12.50"));
        List<BookingItem> items = List.of(item);

        when(screeningRepositoryMock.reserveSeats(10L, 2)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Booking createdBooking = bookingService.createBooking(userId, items);

        // THEN
        assertNotNull(createdBooking.expiresAt());
        assertNotNull(createdBooking.createdAt());
        assertFalse(createdBooking.expiresAt().isAfter(createdBooking.createdAt().plusMinutes(15)));
        long minutesBetween = Duration.between(createdBooking.createdAt(), createdBooking.expiresAt()).toMinutes();
        assertEquals(15, minutesBetween);
    }

    @Test
    void should_throw_exception_when_no_seats_are_available_for_screening() {
        // GIVEN
        Long userId = 1L;
        BookingItem item = new BookingItem(null, null, 10L, 3, new BigDecimal("12.50"));
        List<BookingItem> items = List.of(item);

        when(screeningRepositoryMock.reserveSeats(10L, 3)).thenReturn(false);

        // WHEN & THEN
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(userId, items)
        );

        assertTrue(exception.getMessage().contains("Not enough available seats for screening id: 10"));
        verify(screeningRepositoryMock).reserveSeats(10L, 3);
        verify(bookingRepositoryMock, never()).save(any());
    }

    @Test
    void should_allow_booking_after_cancelling_previous_booking_that_held_last_available_seats() {
        // GIVEN
        Long userId = 1L;
        Long screeningId = 10L;
        Long firstBookingId = 100L;
        Long secondBookingId = 101L;

        BookingItem firstBookingItem = new BookingItem(null, null, screeningId, 2, new BigDecimal("12.50"));
        BookingItem secondBookingItem = new BookingItem(null, null, screeningId, 1, new BigDecimal("12.50"));

        // Step 1: User reserves the last 2 seats
        when(screeningRepositoryMock.reserveSeats(screeningId, 2)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking firstBooking = bookingService.createBooking(userId, List.of(firstBookingItem));
        assertEquals(BookingStatus.PENDING, firstBooking.status());
        verify(screeningRepositoryMock).reserveSeats(screeningId, 2);

        // Step 2: Since all seats are held, reserving another seat would fail
        when(screeningRepositoryMock.reserveSeats(screeningId, 1)).thenReturn(false);
        assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(userId, List.of(secondBookingItem))
        );

        // Step 3: User cancels the first booking holding the 2 seats
        Booking firstBookingWithId = new Booking(
                firstBookingId,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                firstBooking.expiresAt(),
                firstBooking.createdAt(),
                List.of(new BookingItem(1L, firstBookingId, screeningId, 2, new BigDecimal("12.50")))
        );
        when(bookingRepositoryMock.findById(firstBookingId)).thenReturn(Optional.of(firstBookingWithId));

        Booking cancelledBooking = bookingService.cancelBooking(firstBookingId);
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.status());
        verify(screeningRepositoryMock).releaseSeats(screeningId, 2);

        // Step 4: Now that 2 seats were released back, reserving 1 seat succeeds
        when(screeningRepositoryMock.reserveSeats(screeningId, 1)).thenReturn(true);
        Booking secondBooking = bookingService.createBooking(userId, List.of(secondBookingItem));
        assertNotNull(secondBooking);
        assertEquals(BookingStatus.PENDING, secondBooking.status());

        // Step 5: User proceeds to payment and confirms the 1-seat booking
        Booking secondBookingWithId = new Booking(
                secondBookingId,
                userId,
                new BigDecimal("12.50"),
                BookingStatus.PENDING,
                secondBooking.expiresAt(),
                secondBooking.createdAt(),
                List.of(new BookingItem(2L, secondBookingId, screeningId, 1, new BigDecimal("12.50")))
        );
        User user = new User(userId, "user@cinema.com", "User Name", LocalDateTime.now());
        when(bookingRepositoryMock.findById(secondBookingId)).thenReturn(Optional.of(secondBookingWithId));
        when(paymentGatewayMock.processPayment(eq(secondBookingId), eq(new BigDecimal("12.50")), any())).thenReturn(true);
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));

        Booking confirmedBooking = bookingService.confirmBookingPayment(secondBookingId, "tok_visa");

        // THEN
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking.status());
        verify(paymentGatewayMock).processPayment(secondBookingId, new BigDecimal("12.50"), "tok_visa");
        verify(notificationMock).sendBookingConfirmationEmail(eq(user), any(Booking.class));
    }

    @Test
    void should_orchestrate_booking_flow_nominal_scenario() {
        // GIVEN
        Long userId = 42L;
        Long movieId = 5L;
        Long screeningId = 10L;
        int seatCount = 2;
        String paymentToken = "tok_visa";
        User user = new User(userId, "john@cinema.com", "John Doe", LocalDateTime.now());

        when(screeningRepositoryMock.reserveSeats(screeningId, seatCount)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            return new Booking(
                    b.id() != null ? b.id() : 100L,
                    b.userId(),
                    b.totalAmount(),
                    b.status(),
                    b.expiresAt(),
                    b.createdAt(),
                    b.items()
            );
        });
        when(bookingRepositoryMock.findById(100L)).thenAnswer(inv -> Optional.of(new Booking(
                100L,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, 100L, screeningId, seatCount, new BigDecimal("12.50")))
        )));
        when(paymentGatewayMock.processPayment(eq(100L), eq(new BigDecimal("25.00")), eq(paymentToken))).thenReturn(true);
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(mediaStorageMock.generateUrlAccesMedia(anyString())).thenReturn("https://storage/ticket-100-qr.png");
        when(qrCodeGeneratorMock.generateQrCodeImage(anyString(), anyInt(), anyInt())).thenReturn(new byte[]{1, 2, 3});

        // Step 1: Start flow
        BookingFlowContext context = bookingService.startFlow();
        assertNotNull(context.getFlowSessionId());
        assertEquals(BookingFlowStateName.SELECT_MOVIE, context.getStateName());

        // Step 2: Select movie
        context = bookingService.selectMovie(context.getFlowSessionId(), movieId);
        assertEquals(movieId, context.getSelectedMovieId());
        assertEquals(BookingFlowStateName.SELECT_SCREENING, context.getStateName());

        // Step 3: Select screening
        context = bookingService.selectScreening(context.getFlowSessionId(), screeningId);
        assertEquals(screeningId, context.getSelectedScreeningId());
        assertEquals(BookingFlowStateName.SELECT_SEATS, context.getStateName());

        // Step 4: Select seats without auth -> transitions to LOGIN_REQUIRED
        context = bookingService.selectSeats(context.getFlowSessionId(), seatCount, null);
        assertEquals(seatCount, context.getSelectedSeatCount());
        assertEquals(BookingFlowStateName.LOGIN_REQUIRED, context.getStateName());

        // Step 5: Authenticate -> holds seats and transitions to PAYMENT
        context = bookingService.authenticate(context.getFlowSessionId(), userId);
        assertEquals(userId, context.getAuthenticatedUserId());
        assertEquals(BookingFlowStateName.PAYMENT, context.getStateName());
        assertNotNull(context.getBooking());
        assertEquals(100L, context.getBookingId());

        // Step 6: Process payment -> transitions to CONFIRMED and attaches QR code & URL
        context = bookingService.processPayment(context.getFlowSessionId(), paymentToken, userId);
        assertEquals(BookingFlowStateName.CONFIRMED, context.getStateName());
        assertEquals("https://storage/ticket-100-qr.png", context.getQrCodeUrl());
        assertNotNull(context.getQrCodeData());

        verify(screeningRepositoryMock).reserveSeats(screeningId, seatCount);
        verify(paymentGatewayMock).processPayment(100L, new BigDecimal("25.00"), paymentToken);
        verify(mediaStorageMock).generateUrlAccesMedia("tickets/booking-100-qr.png");
        verify(qrCodeGeneratorMock).generateQrCodeImage(eq("CINEMA-TICKET:100:42"), eq(250), eq(250));
        verify(notificationMock).sendBookingConfirmationEmail(eq(user), any(Booking.class), eq("https://storage/ticket-100-qr.png"), any());
    }

    @Test
    void should_cancel_flow_and_release_held_seats() {
        // GIVEN
        Long userId = 42L;
        Long screeningId = 10L;
        int seatCount = 2;

        when(screeningRepositoryMock.reserveSeats(screeningId, seatCount)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            return new Booking(
                    100L,
                    b.userId(),
                    b.totalAmount(),
                    b.status(),
                    b.expiresAt(),
                    b.createdAt(),
                    b.items()
            );
        });
        when(bookingRepositoryMock.findById(100L)).thenReturn(Optional.of(new Booking(
                100L,
                userId,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now(),
                List.of(new BookingItem(1L, 100L, screeningId, seatCount, new BigDecimal("12.50")))
        )));

        BookingFlowContext context = bookingService.startFlow();
        bookingService.selectMovie(context.getFlowSessionId(), 1L);
        bookingService.selectScreening(context.getFlowSessionId(), screeningId);
        bookingService.selectSeats(context.getFlowSessionId(), seatCount, userId);

        // WHEN: Cancel flow
        BookingFlowContext cancelledContext = bookingService.cancelFlow(context.getFlowSessionId());

        // THEN
        assertEquals(BookingFlowStateName.CANCELLED, cancelledContext.getStateName());
        verify(screeningRepositoryMock).releaseSeats(screeningId, seatCount);
    }

    @Test
    void should_authenticate_with_valid_jwt_and_hold_seats_with_expiration() {
        // GIVEN
        Long userId = 42L;
        Long screeningId = 10L;
        int seatCount = 2;
        JwtTokenProvider jwtProvider = new JwtTokenProvider("test-secret-key-cinema-test-1234567890", 3600);
        String token = jwtProvider.generateToken(userId, "alice@cinema.fr", "Alice Dupont");

        BookingService serviceWithJwt = new BookingService(
                bookingRepositoryMock,
                screeningRepositoryMock,
                paymentGatewayMock,
                notificationMock,
                userRepositoryMock,
                qrCodeGeneratorMock,
                mediaStorageMock,
                jwtProvider
        );

        when(screeningRepositoryMock.reserveSeats(screeningId, seatCount)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            return new Booking(
                    100L,
                    b.userId(),
                    b.totalAmount(),
                    b.status(),
                    b.expiresAt(),
                    b.createdAt(),
                    b.items()
            );
        });

        BookingFlowContext context = serviceWithJwt.startFlow();
        serviceWithJwt.selectMovie(context.getFlowSessionId(), 1L);
        serviceWithJwt.selectScreening(context.getFlowSessionId(), screeningId);
        serviceWithJwt.selectSeats(context.getFlowSessionId(), seatCount, null);
        assertEquals(BookingFlowStateName.LOGIN_REQUIRED, context.getStateName());

        // WHEN
        context = serviceWithJwt.authenticate(context.getFlowSessionId(), token);

        // THEN
        assertEquals(BookingFlowStateName.PAYMENT, context.getStateName());
        assertEquals(userId, context.getAuthenticatedUserId());
        assertNotNull(context.getBooking());
        assertNotNull(context.getBooking().expiresAt());
        assertTrue(context.getBooking().expiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
    }

    @Test
    void should_stay_in_login_required_when_invalid_jwt_token() {
        JwtTokenProvider jwtProvider = new JwtTokenProvider("test-secret-key-cinema-test-1234567890", 3600);
        BookingService serviceWithJwt = new BookingService(
                bookingRepositoryMock,
                screeningRepositoryMock,
                paymentGatewayMock,
                notificationMock,
                userRepositoryMock,
                qrCodeGeneratorMock,
                mediaStorageMock,
                jwtProvider
        );

        BookingFlowContext context = serviceWithJwt.startFlow();
        serviceWithJwt.selectMovie(context.getFlowSessionId(), 1L);
        serviceWithJwt.selectScreening(context.getFlowSessionId(), 10L);
        serviceWithJwt.selectSeats(context.getFlowSessionId(), 2, null);

        // WHEN: Authenticate with invalid token
        context = serviceWithJwt.authenticate(context.getFlowSessionId(), "invalid.token.here");

        // THEN
        assertEquals(BookingFlowStateName.LOGIN_REQUIRED, context.getStateName());
    }

    @Test
    void should_reject_payment_when_user_is_not_authenticated() {
        BookingFlowContext context = bookingService.startFlow();
        bookingService.selectMovie(context.getFlowSessionId(), 1L);
        bookingService.selectScreening(context.getFlowSessionId(), 10L);
        bookingService.selectSeats(context.getFlowSessionId(), 2, null);

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.processPayment(context.getFlowSessionId(), "tok_visa", null)
        );
    }
}
