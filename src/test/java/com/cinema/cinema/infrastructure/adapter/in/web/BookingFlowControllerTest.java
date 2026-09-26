package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.BookingFlowContext;
import com.cinema.cinema.domain.model.state.*;
import com.cinema.cinema.domain.port.in.GetBookingFlowStatusUseCase;
import com.cinema.cinema.domain.port.in.ProcessBookingFlowUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingFlowController.class)
@WithMockUser
class BookingFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessBookingFlowUseCase processBookingFlowUseCase;

    @MockitoBean
    private GetBookingFlowStatusUseCase getBookingFlowStatusUseCase;

    @Test
    @DisplayName("should start a new booking flow")
    void shouldStartFlow() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        given(processBookingFlowUseCase.startFlow()).willReturn(context);

        mockMvc.perform(post("/api/v1/booking-flow/start").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSessionId").value("session-123"))
                .andExpect(jsonPath("$.state").value("SELECT_MOVIE"));
    }

    @Test
    @DisplayName("should get flow status")
    void shouldGetFlowStatus() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setSelectedMovieId(5L);
        context.setState(new SelectScreeningState());
        given(getBookingFlowStatusUseCase.getFlowStatus("session-123")).willReturn(context);

        mockMvc.perform(get("/api/v1/booking-flow/session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowSessionId").value("session-123"))
                .andExpect(jsonPath("$.selectedMovieId").value(5))
                .andExpect(jsonPath("$.state").value("SELECT_SCREENING"));
    }

    @Test
    @DisplayName("should select movie")
    void shouldSelectMovie() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setSelectedMovieId(5L);
        context.setState(new SelectScreeningState());
        given(processBookingFlowUseCase.selectMovie("session-123", 5L)).willReturn(context);

        String json = """
                {
                    "movieId": 5
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/select-movie")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedMovieId").value(5))
                .andExpect(jsonPath("$.state").value("SELECT_SCREENING"));
    }

    @Test
    @DisplayName("should select screening")
    void shouldSelectScreening() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setSelectedMovieId(5L);
        context.setSelectedScreeningId(10L);
        context.setState(new SelectSeatsState());
        given(processBookingFlowUseCase.selectScreening("session-123", 10L)).willReturn(context);

        String json = """
                {
                    "screeningId": 10
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/select-screening")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedScreeningId").value(10))
                .andExpect(jsonPath("$.state").value("SELECT_SEATS"));
    }

    @Test
    @DisplayName("should select seats as guest and transition to LOGIN_REQUIRED")
    void shouldSelectSeatsAsGuest() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setSelectedMovieId(5L);
        context.setSelectedScreeningId(10L);
        context.setSelectedSeatCount(2);
        context.setState(new LoginRequiredState());
        given(processBookingFlowUseCase.selectSeats(eq("session-123"), eq(2), isNull())).willReturn(context);

        String json = """
                {
                    "seatCount": 2
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/select-seats")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedSeatCount").value(2))
                .andExpect(jsonPath("$.state").value("LOGIN_REQUIRED"));
    }

    @Test
    @DisplayName("authenticate should return LOGIN_REQUIRED state with loginUrl when unauthenticated")
    void authenticate_shouldReturnLoginRequiredWhenNoTokenOrUserId() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setState(new LoginRequiredState());
        given(processBookingFlowUseCase.authenticate(eq("session-123"), isNull(String.class))).willReturn(context);

        String json = """
                {
                    "token": null
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.loginUrl").value("/login?redirect=/booking/session-123"));
    }

    @Test
    @DisplayName("authenticate should succeed with valid JWT token and return paymentPageUrl and expiresAt")
    void authenticate_shouldSucceedWithJwtToken() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setAuthenticatedUserId(42L);
        context.setState(new PaymentState());
        given(processBookingFlowUseCase.authenticate("session-123", "valid.jwt.token")).willReturn(context);

        String json = """
                {
                    "token": "valid.jwt.token"
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedUserId").value(42))
                .andExpect(jsonPath("$.state").value("PAYMENT"))
                .andExpect(jsonPath("$.paymentPageUrl").value("/checkout?session=session-123"));
    }

    @Test
    @DisplayName("authenticate should succeed with valid userId")
    void authenticate_shouldSucceedWithUserId() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setAuthenticatedUserId(42L);
        context.setState(new PaymentState());
        given(processBookingFlowUseCase.authenticate("session-123", 42L)).willReturn(context);

        String json = """
                {
                    "userId": 42
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedUserId").value(42))
                .andExpect(jsonPath("$.state").value("PAYMENT"))
                .andExpect(jsonPath("$.paymentPageUrl").value("/checkout?session=session-123"));
    }

    @Test
    @DisplayName("pay should reject unauthenticated request with 401")
    void pay_shouldRejectWithoutUserId() throws Exception {
        String json = """
                {
                    "paymentToken": "tok_visa",
                    "userId": null
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/pay")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("pay should succeed with authenticated userId and payment token")
    void pay_shouldSucceedWithAuthenticatedUser() throws Exception {
        BookingFlowContext context = new BookingFlowContext("session-123");
        context.setAuthenticatedUserId(42L);
        context.setBookingId(100L);
        context.setQrCodeUrl("https://storage/qr.png");
        context.setState(new ConfirmedState());
        given(processBookingFlowUseCase.processPayment("session-123", "tok_visa", 42L)).willReturn(context);

        String json = """
                {
                    "userId": 42,
                    "paymentToken": "tok_visa"
                }
                """;

        mockMvc.perform(post("/api/v1/booking-flow/session-123/pay")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedUserId").value(42))
                .andExpect(jsonPath("$.bookingId").value(100))
                .andExpect(jsonPath("$.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.qrCodeUrl").value("https://storage/qr.png"));
    }
}
