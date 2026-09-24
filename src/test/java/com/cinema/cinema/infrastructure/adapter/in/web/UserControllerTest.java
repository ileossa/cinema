package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.in.GetUserBookingsUseCase;
import com.cinema.cinema.domain.port.in.GetUserProfileUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetUserProfileUseCase getUserProfileUseCase;

    @MockitoBean
    private GetUserBookingsUseCase getUserBookingsUseCase;

    @Test
    @DisplayName("doit retourner le profil utilisateur quand il existe")
    void shouldReturnUserProfileWhenExists() throws Exception {
        User user = new User(1L, "alice@cinema.fr", "Alice Dupont", LocalDateTime.of(2026, 9, 24, 10, 0));
        given(getUserProfileUseCase.getUserProfile(1L)).willReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@cinema.fr"))
                .andExpect(jsonPath("$.fullName").value("Alice Dupont"));
    }

    @Test
    @DisplayName("doit retourner 404 quand l'utilisateur n'existe pas")
    void shouldReturn404WhenUserNotFound() throws Exception {
        given(getUserProfileUseCase.getUserProfile(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("doit retourner les réservations d'un utilisateur via /users/{id}/bookings")
    void shouldReturnUserBookings() throws Exception {
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

        mockMvc.perform(get("/api/v1/users/1/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }
}
