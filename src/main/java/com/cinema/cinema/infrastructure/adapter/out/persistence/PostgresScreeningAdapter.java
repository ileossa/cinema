package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.ScreeningEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataScreeningRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<Screening> findById(Long id) {
        return springDataScreeningRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public boolean reserveSeats(Long screeningId, int seatCount) {
        int rowsUpdated = springDataScreeningRepository.reserveSeats(screeningId, seatCount);
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    public void releaseSeats(Long screeningId, int seatCount) {
        springDataScreeningRepository.releaseSeats(screeningId, seatCount);
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
