package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.domain.port.in.CancelBookingUseCase;
import com.cinema.cinema.domain.port.in.ConfirmBookingPaymentUseCase;
import com.cinema.cinema.domain.port.in.CreateBookingUseCase;
import com.cinema.cinema.domain.port.in.GetUserBookingsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBookingUseCase createBookingUseCase;

    @MockitoBean
    private ConfirmBookingPaymentUseCase confirmBookingPaymentUseCase;

    @MockitoBean
    private CancelBookingUseCase cancelBookingUseCase;

    @MockitoBean
    private GetUserBookingsUseCase getUserBookingsUseCase;

    @Test
    @DisplayName("doit créer une réservation et retourner 201 Created")
    void shouldCreateBooking() throws Exception {
        BookingItem item = new BookingItem(1L, 10L, 5L, 2, new BigDecimal("12.50"));
        Booking booking = new Booking(
                10L,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.PENDING,
                LocalDateTime.of(2026, 9, 24, 10, 15),
                LocalDateTime.of(2026, 9, 24, 10, 0),
                List.of(item)
        );

        given(createBookingUseCase.createBooking(eq(1L), any())).willReturn(booking);

        String jsonRequest = """
                {
                    "userId": 1,
                    "items": [
                        {
                            "screeningId": 5,
                            "seatCount": 2,
                            "unitPrice": 12.50
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].screeningId").value(5))
                .andExpect(jsonPath("$.items[0].seatCount").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.50))
                .andExpect(jsonPath("$.items[0].subTotal").value(25.00));
    }

    @Test
    @DisplayName("doit confirmer le paiement d'une réservation sans token")
    void shouldConfirmBookingPayment() throws Exception {
        BookingItem item = new BookingItem(1L, 10L, 5L, 2, new BigDecimal("12.50"));
        Booking booking = new Booking(
                10L,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.CONFIRMED,
                LocalDateTime.of(2026, 9, 24, 10, 15),
                LocalDateTime.of(2026, 9, 24, 10, 0),
                List.of(item)
        );

        given(confirmBookingPaymentUseCase.confirmBookingPayment(10L, null)).willReturn(booking);

        mockMvc.perform(post("/api/v1/bookings/10/confirm-payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("doit confirmer le paiement d'une réservation avec token de paiement")
    void shouldConfirmBookingPaymentWithToken() throws Exception {
        BookingItem item = new BookingItem(1L, 10L, 5L, 2, new BigDecimal("12.50"));
        Booking booking = new Booking(
                10L,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.CONFIRMED,
                LocalDateTime.of(2026, 9, 24, 10, 15),
                LocalDateTime.of(2026, 9, 24, 10, 0),
                List.of(item)
        );

        given(confirmBookingPaymentUseCase.confirmBookingPayment(10L, "tok_visa")).willReturn(booking);

        String jsonRequest = """
                {
                    "paymentToken": "tok_visa"
                }
                """;

        mockMvc.perform(post("/api/v1/bookings/10/confirm-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("doit annuler une réservation")
    void shouldCancelBooking() throws Exception {
        BookingItem item = new BookingItem(1L, 10L, 5L, 2, new BigDecimal("12.50"));
        Booking booking = new Booking(
                10L,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.CANCELLED,
                LocalDateTime.of(2026, 9, 24, 10, 15),
                LocalDateTime.of(2026, 9, 24, 10, 0),
                List.of(item)
        );

        given(cancelBookingUseCase.cancelBooking(10L)).willReturn(booking);

        mockMvc.perform(post("/api/v1/bookings/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("doit récupérer les réservations d'un utilisateur")
    void shouldGetUserBookings() throws Exception {
        BookingItem item = new BookingItem(1L, 10L, 5L, 2, new BigDecimal("12.50"));
        Booking booking = new Booking(
                10L,
                1L,
                new BigDecimal("25.00"),
                BookingStatus.CONFIRMED,
                LocalDateTime.of(2026, 9, 24, 10, 15),
                LocalDateTime.of(2026, 9, 24, 10, 0),
                List.of(item)
        );

        given(getUserBookingsUseCase.getUserBookings(1L)).willReturn(List.of(booking));

        mockMvc.perform(get("/api/v1/bookings/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(1));
    }
}
