package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.port.in.CancelBookingUseCase;
import com.cinema.cinema.domain.port.in.ConfirmBookingPaymentUseCase;
import com.cinema.cinema.domain.port.in.CreateBookingUseCase;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingItemRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.ConfirmPaymentRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.CreateBookingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final ConfirmBookingPaymentUseCase confirmBookingPaymentUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    public BookingController(
            CreateBookingUseCase createBookingUseCase,
            ConfirmBookingPaymentUseCase confirmBookingPaymentUseCase,
            CancelBookingUseCase cancelBookingUseCase
    ) {
        this.createBookingUseCase = createBookingUseCase;
        this.confirmBookingPaymentUseCase = confirmBookingPaymentUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@RequestBody CreateBookingRequest request) {
        List<BookingItem> items = request.items() != null
                ? request.items().stream().map(BookingItemRequest::toDomain).toList()
                : List.of();

        Booking booking = createBookingUseCase.createBooking(request.userId(), items);
        return BookingResponse.fromDomain(booking);
    }

    @PostMapping("/{id}/confirm-payment")
    public BookingResponse confirmPayment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ConfirmPaymentRequest request
    ) {
        String token = request != null ? request.paymentToken() : null;
        Booking booking = confirmBookingPaymentUseCase.confirmBookingPayment(id, token);
        return BookingResponse.fromDomain(booking);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable("id") Long id) {
        Booking booking = cancelBookingUseCase.cancelBooking(id);
        return BookingResponse.fromDomain(booking);
    }
}
