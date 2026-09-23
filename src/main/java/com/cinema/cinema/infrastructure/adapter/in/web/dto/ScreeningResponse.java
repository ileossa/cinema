package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScreeningResponse(
        Long id,
        Long movieId,
        LocalDateTime screeningDateTime,
        String room,
        int availableSeats,
        BigDecimal price
) {
    public static ScreeningResponse fromDomain(Screening screening) {
        return new ScreeningResponse(
                screening.id(),
                screening.movieId(),
                screening.startDateTime(),
                screening.room(),
                screening.availableSeats(),
                screening.unitPrice()
        );
    }
}
