package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.BookingRepositoryOut;
import com.cinema.cinema.domain.port.out.NotificationOut;
import com.cinema.cinema.domain.port.out.PaymentGatewayOut;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.domain.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepositoryMock = mock(BookingRepositoryOut.class);
        screeningRepositoryMock = mock(ScreeningRepositoryOut.class);
        paymentGatewayMock = mock(PaymentGatewayOut.class);
        notificationMock = mock(NotificationOut.class);
        userRepositoryMock = mock(UserRepositoryOut.class);

        bookingService = new BookingService(
                bookingRepositoryMock,
                screeningRepositoryMock,
                paymentGatewayMock,
                notificationMock,
                userRepositoryMock
        );
    }

    @Test
    void should_create_booking_successfully() {
        // GIVEN
        Long userId = 1L;
        BookingItem item1 = new BookingItem(null, null, 10L, 2, new BigDecimal("12.50"));
        BookingItem item2 = new BookingItem(null, null, 20L, 1, new BigDecimal("10.00"));
        List<BookingItem> items = List.of(item1, item2);

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
}
