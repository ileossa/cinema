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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class BookingFlowEdgeCasesIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Edge Case 1: Invalid JWT token during authenticate step keeps state in LOGIN_REQUIRED and does not hold seats")
    void shouldStayInLoginRequiredWhenAuthenticatingWithInvalidJwt() throws Exception {
        // GIVEN
        MovieEntity movie = createAndSaveMovie("Dune", "Sci-fi epic", "Paul Atreides...", 155, "dune.jpg", "https://trailer");
        ScreeningEntity screening = createAndSaveScreening(movie.getId(), LocalDateTime.now().plusDays(1), "Salle 2", 50, new BigDecimal("10.00"));

        MvcResult startResult = mockMvc.perform(post("/api/v1/booking-flow/start").with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        String flowSessionId = fromJson(startResult, BookingFlowResponse.class).flowSessionId();

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, movie.getId(), null, null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, screening.getId(), null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, 3, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("LOGIN_REQUIRED"));

        // WHEN: authenticate with forged / invalid JWT
        BookingFlowStepRequest invalidAuthRequest = new BookingFlowStepRequest(null, null, null, null, null, null, "invalid.garbage.jwt.token", null);
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(invalidAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.loginUrl").value("/login?redirect=/booking/" + flowSessionId))
                .andExpect(jsonPath("$.authenticatedUserId").doesNotExist())
                .andExpect(jsonPath("$.bookingId").doesNotExist());

        // THEN: no seats are reserved, no bookings created in DB
        ScreeningEntity screeningInDb = screeningRepository.findById(screening.getId()).orElseThrow();
        assertEquals(50, screeningInDb.getAvailableSeats(), "Seats must remain 50");
        assertEquals(0, bookingRepository.count(), "No booking record should be created");
    }

    @Test
    @DisplayName("Edge Case 2: Cancellation of flow releases held seats and marks booking CANCELLED in DB")
    void shouldCancelFlowAndReleaseReservedSeats() throws Exception {
        // GIVEN
        MovieEntity movie = createAndSaveMovie("Interstellar", "Space exploration", "Cooper voyages...", 169, "interstellar.jpg", "https://trailer");
        ScreeningEntity screening = createAndSaveScreening(movie.getId(), LocalDateTime.now().plusDays(1), "Salle IMAX", 50, new BigDecimal("14.00"));

        MvcResult startResult = mockMvc.perform(post("/api/v1/booking-flow/start").with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        String flowSessionId = fromJson(startResult, BookingFlowResponse.class).flowSessionId();

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, movie.getId(), null, null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, screening.getId(), null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, 4, null, null, null, null))))
                .andExpect(status().isOk());

        RegisterRequest registerReq = new RegisterRequest("bob@cinema.com", "Bob Builder", "bobpass");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authRes = fromJson(regResult, AuthResponse.class);

        MvcResult authStepResult = mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, null, null, null, authRes.token(), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PAYMENT"))
                .andReturn();

        BookingFlowResponse authStepResponse = fromJson(authStepResult, BookingFlowResponse.class);
        Long bookingId = authStepResponse.bookingId();
        assertNotNull(bookingId);

        // Verify seats reserved in DB (50 - 4 = 46)
        ScreeningEntity screeningHeld = screeningRepository.findById(screening.getId()).orElseThrow();
        assertEquals(46, screeningHeld.getAvailableSeats());

        // WHEN: Cancel flow
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED"));

        // THEN: seats released back to 50, booking marked CANCELLED
        ScreeningEntity screeningAfterCancel = screeningRepository.findById(screening.getId()).orElseThrow();
        assertEquals(50, screeningAfterCancel.getAvailableSeats(), "Seats must be fully restored to 50");

        BookingEntity bookingAfterCancel = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, bookingAfterCancel.getStatus(), "Booking status in DB must be CANCELLED");
    }

    @Test
    @DisplayName("Edge Case 3: Reject booking when requested seats exceed remaining capacity")
    void shouldRejectBookingWhenRequestedSeatsExceedRemainingCapacity() throws Exception {
        // GIVEN: Screening with only 2 seats remaining
        MovieEntity movie = createAndSaveMovie("Oppenheimer", "Biographical drama", "J. Robert Oppenheimer...", 180, "opp.jpg", "https://trailer");
        ScreeningEntity screening = createAndSaveScreening(movie.getId(), LocalDateTime.now().plusDays(1), "Salle 3", 2, new BigDecimal("11.00"));

        MvcResult startResult = mockMvc.perform(post("/api/v1/booking-flow/start").with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        String flowSessionId = fromJson(startResult, BookingFlowResponse.class).flowSessionId();

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, movie.getId(), null, null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, screening.getId(), null, null, null, null, null))))
                .andExpect(status().isOk());

        // Request 5 seats (more than available 2)
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, 5, null, null, null, null))))
                .andExpect(status().isOk());

        RegisterRequest registerReq = new RegisterRequest("charlie@cinema.com", "Charlie Chaplin", "charliepass");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authRes = fromJson(regResult, AuthResponse.class);

        // WHEN & THEN: Authenticate fails due to insufficient seats
        try {
            mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/authenticate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(new BookingFlowStepRequest(null, null, null, null, null, null, authRes.token(), null))));
        } catch (Exception ignored) {
            // IllegalStateException caught
        }

        // Available seats must still be 2
        ScreeningEntity screeningInDb = screeningRepository.findById(screening.getId()).orElseThrow();
        assertEquals(2, screeningInDb.getAvailableSeats(), "Available seats must remain intact at 2");
    }

    @Test
    @DisplayName("Edge Case 4: Server ignores client-tampered unitPrice and calculates totalAmount strictly from official screening price")
    void shouldEnforceOfficialScreeningPriceAndIgnoreClientPriceTampering() throws Exception {
        // GIVEN: Official screening price = 16.50
        MovieEntity movie = createAndSaveMovie("Gladiator II", "Epic action", "Lucius enters the Colosseum...", 150, "glad.jpg", "https://trailer");
        ScreeningEntity screening = createAndSaveScreening(movie.getId(), LocalDateTime.now().plusDays(1), "Salle Dolby", 50, new BigDecimal("16.50"));

        MvcResult startResult = mockMvc.perform(post("/api/v1/booking-flow/start").with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        String flowSessionId = fromJson(startResult, BookingFlowResponse.class).flowSessionId();

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, movie.getId(), null, null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, screening.getId(), null, null, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, 2, null, null, null, null))))
                .andExpect(status().isOk());

        RegisterRequest registerReq = new RegisterRequest("david@cinema.com", "David Copperfield", "davidpass");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authRes = fromJson(regResult, AuthResponse.class);

        // Malicious client tries to send unitPrice = 0.01 in the step request
        BookingFlowStepRequest tamperedRequest = new BookingFlowStepRequest(null, null, null, null, new BigDecimal("0.01"), null, authRes.token(), null);

        MvcResult authResult = mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(tamperedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(33.00)) // 2 * 16.50 = 33.00, NOT 0.02
                .andReturn();

        BookingFlowResponse authResponse = fromJson(authResult, BookingFlowResponse.class);
        BookingEntity bookingInDb = bookingRepository.findById(authResponse.bookingId()).orElseThrow();
        assertEquals(new BigDecimal("33.00"), bookingInDb.getTotalAmount(), "Total amount in DB must strictly be 33.00");
        assertEquals(new BigDecimal("16.50"), bookingInDb.getItems().get(0).getUnitPrice(), "Item unit price in DB must be official 16.50");
    }

    @Test
    @DisplayName("Edge Case 5: HTTP Error handling for missing required parameters (400) and missing resources (404)")
    void shouldReturnAppropriateHttpErrorStatuses() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/booking-flow/start").with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        String flowSessionId = fromJson(startResult, BookingFlowResponse.class).flowSessionId();

        // 400 Bad Request on select-movie without movieId
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, null, null, null, null, null))))
                .andExpect(status().isBadRequest());

        // 400 Bad Request on select-screening without screeningId
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, null, null, null, null, null))))
                .andExpect(status().isBadRequest());

        // 400 Bad Request on select-seats without seatCount
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, null, null, null, null, null))))
                .andExpect(status().isBadRequest());

        // 401 Unauthorized on payment without userId
        mockMvc.perform(post("/api/v1/booking-flow/" + flowSessionId + "/payment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new BookingFlowStepRequest(null, null, null, null, null, null, null, "tok_123"))))
                .andExpect(status().isUnauthorized());

        // 404 Not Found on non-existent movie
        mockMvc.perform(get("/api/v1/movies/999999"))
                .andExpect(status().isNotFound());
    }
}
