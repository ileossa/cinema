package com.cinema.cinema.integration;

import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.AuthResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingFlowResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingFlowStepRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.BookingEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.MovieEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.ScreeningEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class CinemaBookingEndToEndIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("E2E Nominal Flow: Browse movies -> Select screening & seats -> Register & Login -> Authenticate with JWT -> Pay -> Confirm Booking")
    void fullNominalBookingJourneyE2E() throws Exception {
        // -------------------------------------------------------------------------
        // 0. GIVEN - Seed Database with Movie and Screening
        // -------------------------------------------------------------------------
        MovieEntity savedMovie = createAndSaveMovie(
                "Inception",
                "A thief who steals corporate secrets through the use of dream-sharing technology.",
                "Dom Cobb is a skilled thief, the absolute best in the dangerous art of extraction...",
                148,
                "inception.jpg",
                "https://www.youtube.com/watch?v=YoHD9XEInc0"
        );
        Long movieId = savedMovie.getId();

        LocalDateTime screeningTime = LocalDateTime.now().plusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0);
        BigDecimal unitPrice = new BigDecimal("12.50");
        int initialSeats = 50;

        ScreeningEntity savedScreening = createAndSaveScreening(
                movieId,
                screeningTime,
                "Salle 1 IMAX",
                initialSeats,
                unitPrice
        );
        Long screeningId = savedScreening.getId();

        // -------------------------------------------------------------------------
        // 1. GET /api/v1/movies & GET /api/v1/movies/{id} -> Discover Movie
        // -------------------------------------------------------------------------
        MvcResult movieListResult = mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(movieId))
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[0].synopsis").exists())
                .andExpect(jsonPath("$[0].trailerUrl").value("https://www.youtube.com/watch?v=YoHD9XEInc0"))
                .andReturn();

        mockMvc.perform(get("/api/v1/movies/" + movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movieId))
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.durationMinutes").value(148))
                .andExpect(jsonPath("$.synopsis").isNotEmpty())
                .andExpect(jsonPath("$.trailerUrl").value("https://www.youtube.com/watch?v=YoHD9XEInc0"));

        // -------------------------------------------------------------------------
        // 2. GET /api/v1/screenings?movieId={movieId} -> Find Available Screenings
        // -------------------------------------------------------------------------
        MvcResult screeningListResult = mockMvc.perform(get("/api/v1/screenings").param("movieId", movieId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(screeningId))
                .andExpect(jsonPath("$[0].movieId").value(movieId))
                .andExpect(jsonPath("$[0].room").value("Salle 1 IMAX"))
                .andExpect(jsonPath("$[0].availableSeats").value(50))
                .andExpect(jsonPath("$[0].price").value(12.50))
                .andReturn();

        // -------------------------------------------------------------------------
        // 3. POST /api/v1/booking-flow/start -> Start Booking Flow (SELECT_MOVIE)
        // -------------------------------------------------------------------------
        MvcResult startFlowResult = mockMvc.perform(post("/api/v1/booking-flow/start")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSessionId").isNotEmpty())
                .andExpect(jsonPath("$.state").value("SELECT_MOVIE"))
                .andReturn();

        BookingFlowResponse startResponse = fromJson(startFlowResult, BookingFlowResponse.class);
        String flowSessionId = startResponse.flowSessionId();
        assertNotNull(flowSessionId);

        // -------------------------------------------------------------------------
        // 4. POST /api/v1/booking-flow/{id}/select-movie -> Transition to SELECT_SCREENING
        // -------------------------------------------------------------------------
        BookingFlowStepRequest selectMovieRequest = new BookingFlowStepRequest(null, movieId, null, null, null, null, null, null);
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(selectMovieRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SELECT_SCREENING"))
                .andExpect(jsonPath("$.selectedMovieId").value(movieId));

        // -------------------------------------------------------------------------
        // 5. POST /api/v1/booking-flow/{id}/select-screening -> Transition to SELECT_SEATS
        // -------------------------------------------------------------------------
        BookingFlowStepRequest selectScreeningRequest = new BookingFlowStepRequest(null, null, screeningId, null, null, null, null, null);
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(selectScreeningRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SELECT_SEATS"))
                .andExpect(jsonPath("$.selectedScreeningId").value(screeningId))
                .andExpect(jsonPath("$.unitPrice").value(12.50));

        // -------------------------------------------------------------------------
        // 6. POST /api/v1/booking-flow/{id}/select-seats -> Transition to LOGIN_REQUIRED
        // -------------------------------------------------------------------------
        int seatsToBook = 2;
        BookingFlowStepRequest selectSeatsRequest = new BookingFlowStepRequest(null, null, null, seatsToBook, null, null, null, null);
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(selectSeatsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.selectedSeatCount").value(2))
                .andExpect(jsonPath("$.loginUrl").value("/login?redirect=/booking/" + flowSessionId));

        // -------------------------------------------------------------------------
        // 7. POST /api/v1/auth/register -> User Registration & JWT Generation
        // -------------------------------------------------------------------------
        RegisterRequest registerRequest = new RegisterRequest("alice@cinema.com", "Alice Wonderland", "password123");
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("alice@cinema.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Wonderland"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        AuthResponse authResponse = fromJson(registerResult, AuthResponse.class);
        Long userId = authResponse.userId();
        String jwtToken = authResponse.token();
        assertNotNull(userId);
        assertNotNull(jwtToken);

        // -------------------------------------------------------------------------
        // 8. POST /api/v1/booking-flow/{id}/authenticate (with JWT) -> Transition to PAYMENT
        //    Holds 2 seats, calculates expiresAt (now + 15m), creates PENDING booking in DB.
        // -------------------------------------------------------------------------
        BookingFlowStepRequest authStepRequest = new BookingFlowStepRequest(null, null, null, null, null, null, jwtToken, null);
        MvcResult authStepResult = mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(authStepRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PAYMENT"))
                .andExpect(jsonPath("$.authenticatedUserId").value(userId))
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.paymentPageUrl").value("/checkout?session=" + flowSessionId))
                .andReturn();

        BookingFlowResponse authStepResponse = fromJson(authStepResult, BookingFlowResponse.class);
        Long bookingId = authStepResponse.bookingId();
        assertNotNull(bookingId);
        assertNotNull(authStepResponse.expiresAt());

        // Check DB state after hold
        ScreeningEntity updatedScreeningAfterHold = screeningRepository.findById(screeningId).orElseThrow();
        assertEquals(48, updatedScreeningAfterHold.getAvailableSeats(), "2 seats must be reserved in DB");

        BookingEntity pendingBookingInDb = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.PENDING, pendingBookingInDb.getStatus());
        assertEquals(new BigDecimal("25.00"), pendingBookingInDb.getTotalAmount());
        assertEquals(userId, pendingBookingInDb.getUserId());
        assertEquals(1, pendingBookingInDb.getItems().size());
        assertEquals(screeningId, pendingBookingInDb.getItems().get(0).getScreeningId());
        assertEquals(2, pendingBookingInDb.getItems().get(0).getSeatCount());
        assertEquals(unitPrice, pendingBookingInDb.getItems().get(0).getUnitPrice());

        // -------------------------------------------------------------------------
        // 9. POST /api/v1/booking-flow/{id}/payment -> Payment & Confirmation
        //    Generates QR code, triggers notification, marks booking CONFIRMED.
        // -------------------------------------------------------------------------
        BookingFlowStepRequest paymentRequest = new BookingFlowStepRequest(null, null, null, null, null, userId, null, "tok_stripe_success_123");
        MvcResult paymentResult = mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/payment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.qrCodeUrl").isNotEmpty())
                .andReturn();

        BookingFlowResponse paymentResponse = fromJson(paymentResult, BookingFlowResponse.class);
        assertNotNull(paymentResponse.qrCodeUrl());
        assertTrue(paymentResponse.qrCodeUrl().startsWith("data:image/png;base64,") || paymentResponse.qrCodeUrl().contains("tickets/"));

        // Check DB state after payment
        BookingEntity confirmedBookingInDb = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, confirmedBookingInDb.getStatus(), "Booking must be marked CONFIRMED in DB");

        // -------------------------------------------------------------------------
        // 10. GET /api/v1/users/{userId}/bookings -> User Order History Verification
        // -------------------------------------------------------------------------
        MvcResult userBookingsResult = mockMvc.perform(get("/api/v1/users/" + userId + "/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(bookingId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].totalAmount").value(25.00))
                .andExpect(jsonPath("$[0].items[0].screeningId").value(screeningId))
                .andExpect(jsonPath("$[0].items[0].seatCount").value(2))
                .andExpect(jsonPath("$[0].items[0].unitPrice").value(12.50))
                .andReturn();

        // -------------------------------------------------------------------------
        // 11. GET /api/v1/screenings?movieId={movieId} -> Ensure remaining stock is strictly 48
        // -------------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/screenings").param("movieId", movieId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableSeats").value(48));
    }
}
