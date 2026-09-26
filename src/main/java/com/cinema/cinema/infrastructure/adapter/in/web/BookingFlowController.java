package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.BookingFlowContext;
import com.cinema.cinema.domain.port.in.GetBookingFlowStatusUseCase;
import com.cinema.cinema.domain.port.in.ProcessBookingFlowUseCase;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingFlowResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.BookingFlowStepRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/booking-flow")
public class BookingFlowController {

    private final ProcessBookingFlowUseCase processBookingFlowUseCase;
    private final GetBookingFlowStatusUseCase getBookingFlowStatusUseCase;

    public BookingFlowController(
            ProcessBookingFlowUseCase processBookingFlowUseCase,
            GetBookingFlowStatusUseCase getBookingFlowStatusUseCase
    ) {
        this.processBookingFlowUseCase = processBookingFlowUseCase;
        this.getBookingFlowStatusUseCase = getBookingFlowStatusUseCase;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingFlowResponse startFlow() {
        BookingFlowContext context = processBookingFlowUseCase.startFlow();
        return BookingFlowResponse.fromDomain(context);
    }

    @GetMapping("/{flowSessionId}")
    public BookingFlowResponse getFlowStatus(@PathVariable("flowSessionId") String flowSessionId) {
        BookingFlowContext context = getBookingFlowStatusUseCase.getFlowStatus(flowSessionId);
        return BookingFlowResponse.fromDomain(context);
    }

    @PostMapping("/{flowSessionId}/select-movie")
    public BookingFlowResponse selectMovie(
            @PathVariable("flowSessionId") String flowSessionId,
            @RequestBody BookingFlowStepRequest request
    ) {
        if (request == null || request.movieId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "movieId is required");
        }
        BookingFlowContext context = processBookingFlowUseCase.selectMovie(flowSessionId, request.movieId());
        return BookingFlowResponse.fromDomain(context);
    }

    @PostMapping("/{flowSessionId}/select-screening")
    public BookingFlowResponse selectScreening(
            @PathVariable("flowSessionId") String flowSessionId,
            @RequestBody BookingFlowStepRequest request
    ) {
        if (request == null || request.screeningId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "screeningId is required");
        }
        BookingFlowContext context = processBookingFlowUseCase.selectScreening(flowSessionId, request.screeningId());
        return BookingFlowResponse.fromDomain(context);
    }

    @PostMapping("/{flowSessionId}/select-seats")
    public BookingFlowResponse selectSeats(
            @PathVariable("flowSessionId") String flowSessionId,
            @RequestBody BookingFlowStepRequest request
    ) {
        if (request == null || request.seatCount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatCount is required");
        }
        BookingFlowContext context = processBookingFlowUseCase.selectSeats(flowSessionId, request.seatCount(), request.userId());
        return BookingFlowResponse.fromDomain(context);
    }

    @PostMapping("/{flowSessionId}/authenticate")
    public BookingFlowResponse authenticate(
            @PathVariable("flowSessionId") String flowSessionId,
            @RequestBody(required = false) BookingFlowStepRequest request
    ) {
        String token = request != null ? request.token() : null;
        Long userId = request != null ? request.userId() : null;

        BookingFlowContext context;
        if (token != null && !token.isBlank()) {
            context = processBookingFlowUseCase.authenticate(flowSessionId, token);
        } else if (userId != null) {
            context = processBookingFlowUseCase.authenticate(flowSessionId, userId);
        } else {
            context = processBookingFlowUseCase.authenticate(flowSessionId, (String) null);
        }
        return BookingFlowResponse.fromDomain(context);
    }

    @PostMapping("/{flowSessionId}/pay")
    public BookingFlowResponse processPayment(
            @PathVariable("flowSessionId") String flowSessionId,
            @RequestBody BookingFlowStepRequest request
    ) {
        if (request == null || request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required to process payment");
        }
        String paymentToken = request.paymentToken();
        BookingFlowContext context = processBookingFlowUseCase.processPayment(flowSessionId, paymentToken, request.userId());
        return BookingFlowResponse.fromDomain(context);
    }

    @PostMapping("/{flowSessionId}/cancel")
    public BookingFlowResponse cancelFlow(@PathVariable("flowSessionId") String flowSessionId) {
        BookingFlowContext context = processBookingFlowUseCase.cancelFlow(flowSessionId);
        return BookingFlowResponse.fromDomain(context);
    }
}
