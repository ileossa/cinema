package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.out.MovieRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.MovieEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataMovieRepository;

import java.util.List;
import java.util.Optional;

public class PostgresMovieAdapter implements MovieRepositoryOut {

    private final SpringDataMovieRepository springDataMovieRepository;

    public PostgresMovieAdapter(SpringDataMovieRepository springDataMovieRepository) {
        this.springDataMovieRepository = springDataMovieRepository;
    }

    @Override
    public List<Movie> getMovies() {
        return findAllPublished();
    }

    @Override
    public List<Movie> findAll() {
        return springDataMovieRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Movie> findAllPublished() {
        return springDataMovieRepository.findByIsPublishedTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Movie> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return springDataMovieRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Movie> findPublishedById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return springDataMovieRepository.findByIdAndIsPublishedTrue(id)
                .map(this::toDomain);
    }

    @Override
    public Movie save(Movie movie) {
        if (movie == null) {
            return null;
        }
        MovieEntity entity = toEntity(movie);
        MovieEntity saved = springDataMovieRepository.save(entity);
        return toDomain(saved);
    }

    private Movie toDomain(MovieEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Movie(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getSynopsis(),
                entity.getDurationMinutes(),
                entity.getPosterFileName(),
                null,
                entity.getTrailerUrl(),
                entity.isPublished()
        );
    }

    private MovieEntity toEntity(Movie movie) {
        if (movie == null) {
            return null;
        }
        MovieEntity entity = new MovieEntity();
        entity.setId(movie.id());
        entity.setTitle(movie.title());
        entity.setDescription(movie.description());
        entity.setSynopsis(movie.synopsis());
        entity.setDurationMinutes(movie.durationMinutes());
        entity.setPosterFileName(movie.poster());
        entity.setTrailerUrl(movie.trailerUrl());
        entity.setPublished(movie.isPublished());
        return entity;
    }
}
