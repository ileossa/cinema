package com.cinema.cinema.domain.model;

import java.time.LocalDateTime;

public record Screening(
        Long id,
        Long movieId,
        LocalDateTime startDateTime,
        String room,
        int availableSeats,
        double unitPrice
) {}