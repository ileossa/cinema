package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.Movie;

public record MovieResponse(
        Long id,
        String title,
        String description,
        String synopsis,
        int durationMinutes,
        String posterUrl,
        String trailerUrl
) {
    public static MovieResponse fromDomain(Movie movie) {
        if (movie == null) {
            return null;
        }
        return new MovieResponse(
                movie.id(),
                movie.title(),
                movie.description(),
                movie.synopsis(),
                movie.durationMinutes(),
                movie.posterUrl(),
                movie.trailerUrl()
        );
    }
}
