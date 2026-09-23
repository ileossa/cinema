package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.Movie;

public record MovieResponse(
        Long id,
        String title,
        String description,
        int durationMinutes,
        String posterUrl
) {
    public static MovieResponse fromDomain(Movie movie) {
        return new MovieResponse(
                movie.id(),
                movie.title(),
                movie.description(),
                movie.durationMinutes(),
                movie.posterUrl()
        );
    }
}
