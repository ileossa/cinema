package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import com.cinema.cinema.domain.port.out.MediaStorageOut;
import com.cinema.cinema.domain.port.out.MovieRepositoryOut;

import java.util.List;

public class MovieService implements GetMoviesCurrentlyInTheaterUseCase {

    private final MovieRepositoryOut movieRepositoryOut;
    private final MediaStorageOut mediaStorageOut;

    public MovieService(MovieRepositoryOut movieRepositoryOut, MediaStorageOut mediaStorageOut) {
        this.movieRepositoryOut = movieRepositoryOut;
        this.mediaStorageOut = mediaStorageOut;
    }

    @Override
    public List<Movie> getMoviesCurrentlyInTheater() {
        return movieRepositoryOut.getMovies().stream().map(movie -> {
            if( movie.poster() != null || movie.poster().isBlank()) {
                String urlSigned = mediaStorageOut.generateUrlAccesMedia(movie.poster());
                return movie.withPosterUrl(urlSigned);
            }
            return movie;
        }).toList();
    }
}
