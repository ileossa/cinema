package com.cinema.cinema.domain.model.state;

import com.cinema.cinema.domain.model.BookingFlowContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingFlowStateTest {

    private BookingFlowContext context;

    @BeforeEach
    void setUp() {
        context = new BookingFlowContext("session-123");
    }

    @Test
    void nominalFlow_guestToAuthenticatedToPaymentToConfirmed() {
        assertEquals(BookingFlowStateName.SELECT_MOVIE, context.getStateName());

        // Step 1: Select movie
        context.selectMovie(1L);
        assertEquals(1L, context.getSelectedMovieId());
        assertEquals(BookingFlowStateName.SELECT_SCREENING, context.getStateName());

        // Step 2: Select screening
        context.selectScreening(10L);
        assertEquals(10L, context.getSelectedScreeningId());
        assertEquals(BookingFlowStateName.SELECT_SEATS, context.getStateName());

        // Step 3: Select seats as guest -> transitions to LOGIN_REQUIRED
        context.selectSeats(2);
        assertEquals(2, context.getSelectedSeatCount());
        assertEquals(BookingFlowStateName.LOGIN_REQUIRED, context.getStateName());

        // Step 4: Authenticate user -> transitions to AUTHENTICATED
        context.authenticate(42L);
        assertEquals(42L, context.getAuthenticatedUserId());
        assertEquals(BookingFlowStateName.AUTHENTICATED, context.getStateName());

        // Step 5: Process payment -> transitions to CONFIRMED
        context.processPayment("tok_visa");
        assertEquals(BookingFlowStateName.CONFIRMED, context.getStateName());
    }

    @Test
    void directFlow_preAuthenticatedUser() {
        context.setAuthenticatedUserId(42L);
        context.selectMovie(1L);
        context.selectScreening(10L);

        // Select seats when already authenticated -> goes directly to AUTHENTICATED
        context.selectSeats(3);
        assertEquals(BookingFlowStateName.AUTHENTICATED, context.getStateName());
    }

    @Test
    void invalidTransitions_shouldThrowExceptions() {
        // Cannot select screening in SELECT_MOVIE state
        assertThrows(IllegalStateException.class, () -> context.selectScreening(10L));
        // Cannot select seats in SELECT_MOVIE state
        assertThrows(IllegalStateException.class, () -> context.selectSeats(2));
        // Cannot process payment in SELECT_MOVIE state
        assertThrows(IllegalStateException.class, () -> context.processPayment("tok"));

        context.selectMovie(1L);
        // In SELECT_SCREENING, cannot select seats
        assertThrows(IllegalStateException.class, () -> context.selectSeats(2));

        context.selectScreening(10L);
        // In SELECT_SEATS, invalid seat count
        assertThrows(IllegalArgumentException.class, () -> context.selectSeats(0));
        assertThrows(IllegalArgumentException.class, () -> context.selectSeats(-1));
    }

    @Test
    void backwardNavigation_frontendReselection() {
        context.selectMovie(1L);
        context.selectScreening(10L);
        context.selectSeats(2);
        assertEquals(BookingFlowStateName.LOGIN_REQUIRED, context.getStateName());

        // Client chooses another screening
        context.selectScreening(20L);
        assertEquals(20L, context.getSelectedScreeningId());
        assertNull(context.getSelectedSeatCount());
        assertEquals(BookingFlowStateName.SELECT_SEATS, context.getStateName());

        // Client chooses another movie
        context.selectMovie(2L);
        assertEquals(2L, context.getSelectedMovieId());
        assertNull(context.getSelectedScreeningId());
        assertNull(context.getSelectedSeatCount());
        assertEquals(BookingFlowStateName.SELECT_SCREENING, context.getStateName());
    }

    @Test
    void cancellation_shouldTransitionToCancelled() {
        context.selectMovie(1L);
        context.selectScreening(10L);
        context.cancel();
        assertEquals(BookingFlowStateName.CANCELLED, context.getStateName());

        assertThrows(IllegalStateException.class, () -> context.selectMovie(2L));
        assertThrows(IllegalStateException.class, () -> context.processPayment("tok"));
    }
}
