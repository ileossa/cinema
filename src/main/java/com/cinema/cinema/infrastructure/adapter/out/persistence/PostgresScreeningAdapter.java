package com.cinema.cinema.infrastructure.adapter.out.persistence;


import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.ScreeningEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataScreeningRepository;

import java.math.BigDecimal;
import java.util.List;

public class PostgresScreeningAdapter implements ScreeningRepositoryOut {

    private final SpringDataScreeningRepository springDataScreeningRepository;

    public PostgresScreeningAdapter(SpringDataScreeningRepository springDataScreeningRepository) {
        this.springDataScreeningRepository = springDataScreeningRepository;
    }


    @Override
    public List<Screening> findByMovieId(Long movieId) {
        return springDataScreeningRepository.findByMovieId(movieId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Screening toDomain(ScreeningEntity entity) {
        BigDecimal unitPrice = entity.getUnitPrice() != null
                ? entity.getUnitPrice().abs()
                : BigDecimal.ZERO;

        return new Screening(
                entity.getId(),
                entity.getMovieId(),
                entity.getStartDateTime(),
                entity.getRoomName(),
                entity.getAvailableSeats(),
                unitPrice
        );
    }
}
