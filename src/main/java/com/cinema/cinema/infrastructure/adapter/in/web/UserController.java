package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.in.ChangeUserRoleUseCase;
import com.cinema.cinema.domain.port.in.GetUserBookingsUseCase;
import com.cinema.cinema.domain.port.in.GetUserProfileUseCase;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.ChangeRoleRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final GetUserBookingsUseCase getUserBookingsUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;

    public UserController(GetUserProfileUseCase getUserProfileUseCase,
                          GetUserBookingsUseCase getUserBookingsUseCase,
                          ChangeUserRoleUseCase changeUserRoleUseCase) {
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.getUserBookingsUseCase = getUserBookingsUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
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

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ChangeRoleRequest request
    ) {
        if (changeUserRoleUseCase == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role management not configured");
        }
        if (request == null || request.role() == null || request.role().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        try {
            User updated = changeUserRoleUseCase.assignRole(id, request.role());
            return ResponseEntity.ok(UserResponse.fromDomain(updated));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
