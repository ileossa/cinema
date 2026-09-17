package com.cinema.cinema.infrastructure.adapter.out.persistence;


import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataScreeningRepository;

import java.util.List;

public class PostgresScreeningAdapter implements ScreeningRepositoryOut {

    private final SpringDataScreeningRepository springDataScreeningRepository;

    public PostgresScreeningAdapter(SpringDataScreeningRepository springDataScreeningRepository) {
        this.springDataScreeningRepository = springDataScreeningRepository;
    }


    @Override
    public List<Screening> findByMovieId(Long movieId) {
        return springDataScreeningRepository.findByMovieId(movieId).stream()
                .map(entity -> new Screening(
                        entity.getId(),
                        entity.getMovieId(),
                        entity.getStartDateTime(),
                        entity.getRoomName(),
                        entity.getAvailableSeats(),
                        entity.getUnitPrice() != null ? entity.getUnitPrice().doubleValue() : 0.0
                )).toList();
    }
}
