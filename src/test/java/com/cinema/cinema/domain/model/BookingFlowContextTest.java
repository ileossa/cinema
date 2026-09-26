package com.cinema.cinema.domain.model;

import com.cinema.cinema.domain.model.state.BookingFlowStateName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BookingFlowContextTest {

    @Test
    void newContext_shouldInitializeDefaults() {
        BookingFlowContext context = new BookingFlowContext();

        assertNotNull(context.getFlowSessionId());
        assertEquals(BookingFlowStateName.SELECT_MOVIE, context.getStateName());
    }

    @Test
    void contextProperties_gettersAndSetters() {
        BookingFlowContext context = new BookingFlowContext("custom-session-id");

        assertEquals("custom-session-id", context.getFlowSessionId());

        context.setSelectedMovieId(5L);
        context.setSelectedScreeningId(15L);
        context.setSelectedSeatCount(4);
        context.setUnitPrice(BigDecimal.valueOf(12.50));
        context.setAuthenticatedUserId(99L);
        context.setBookingId(101L);
        context.setQrCodeUrl("https://storage/qr.png");
        byte[] qrBytes = new byte[]{1, 2, 3};
        context.setQrCodeData(qrBytes);

        assertEquals(5L, context.getSelectedMovieId());
        assertEquals(15L, context.getSelectedScreeningId());
        assertEquals(4, context.getSelectedSeatCount());
        assertEquals(BigDecimal.valueOf(12.50), context.getUnitPrice());
        assertEquals(99L, context.getAuthenticatedUserId());
        assertEquals(101L, context.getBookingId());
        assertEquals("https://storage/qr.png", context.getQrCodeUrl());
        assertArrayEquals(qrBytes, context.getQrCodeData());
    }
}
