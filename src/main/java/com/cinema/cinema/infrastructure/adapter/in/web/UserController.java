package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.port.in.GetUserBookingsUseCase;
import com.cinema.cinema.domain.port.in.GetUserProfileUseCase;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final GetUserBookingsUseCase getUserBookingsUseCase;

    public UserController(GetUserProfileUseCase getUserProfileUseCase, GetUserBookingsUseCase getUserBookingsUseCase) {
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.getUserBookingsUseCase = getUserBookingsUseCase;
    }

    // TODO: il faut rendre l'appel contraingnant derrière une sécurité.
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable("id") Long id) {
        return getUserProfileUseCase.getUserProfile(id)
                .map(UserResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> getUserBookings(@PathVariable("id") Long id) {
        return getUserBookingsUseCase.getUserBookings(id).stream()
                .map(BookingResponse::fromDomain)
                .toList();
    }
}
