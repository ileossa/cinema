package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.out.MovieRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataMovieRepository;

import java.util.List;


public class PostgresMovieAdapter implements MovieRepositoryOut {

    private final SpringDataMovieRepository springDataMovieRepository;

    public PostgresMovieAdapter(SpringDataMovieRepository springDataMovieRepository) {
        this.springDataMovieRepository = springDataMovieRepository;
    }

    @Override
    public List<Movie> getMovies() {
        return springDataMovieRepository.findAll().stream().map(
                entity -> new Movie(
                        entity.getId(),
                        entity.getTitle(),
                        entity.getDescription(),
                        entity.getDurationMinutes(),
                        entity.getPosterFileName(),
                        null
                )
        ).toList();
    }
}
