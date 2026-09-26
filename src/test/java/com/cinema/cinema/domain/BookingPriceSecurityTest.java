package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.out.BookingRepositoryOut;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.domain.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingPriceSecurityTest {

    private BookingRepositoryOut bookingRepositoryMock;
    private ScreeningRepositoryOut screeningRepositoryMock;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepositoryMock = mock(BookingRepositoryOut.class);
        screeningRepositoryMock = mock(ScreeningRepositoryOut.class);
        bookingService = new BookingService(bookingRepositoryMock, screeningRepositoryMock);
    }

    @Test
    @DisplayName("Should ignore client-tampered unit price and use official screening unit price")
    void shouldIgnoreClientTamperedUnitPriceAndUseScreeningPrice() {
        // GIVEN
        Long userId = 42L;
        Long screeningId = 12L;
        BigDecimal officialPrice = new BigDecimal("14.50");
        BigDecimal tamperedPrice = new BigDecimal("0.01");

        Screening officialScreening = new Screening(
                screeningId,
                1L,
                LocalDateTime.now().plusDays(1),
                "Salle 1",
                50,
                officialPrice
        );

        when(screeningRepositoryMock.findById(screeningId)).thenReturn(Optional.of(officialScreening));
        when(screeningRepositoryMock.reserveSeats(screeningId, 2)).thenReturn(true);
        when(bookingRepositoryMock.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Client attempts to pass 0.01 price
        BookingItem clientItem = new BookingItem(null, null, screeningId, 2, tamperedPrice);

        // WHEN
        Booking booking = bookingService.createBooking(userId, List.of(clientItem));

        // THEN
        assertNotNull(booking);
        assertEquals(BookingStatus.PENDING, booking.status());
        assertEquals(1, booking.items().size());

        BookingItem savedItem = booking.items().getFirst();
        assertEquals(officialPrice, savedItem.unitPrice(), "Unit price must be overridden with the official screening price");
        assertEquals(new BigDecimal("29.00"), savedItem.subTotal());
        assertEquals(new BigDecimal("29.00"), booking.totalAmount(), "Total amount must be calculated from official price");
    }
}
