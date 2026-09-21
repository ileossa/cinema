package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.in.GetMovieShowingsUseCase;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;

import java.util.List;

public class ScreeningService implements GetMovieShowingsUseCase {
    private final ScreeningRepositoryOut screeningRepositoryOut;

    public ScreeningService(ScreeningRepositoryOut screeningRepositoryOut) {
        this.screeningRepositoryOut = screeningRepositoryOut;
    }

    @Override
    public List<Screening> getMovieShowings(Long movieId) {
        return screeningRepositoryOut.findByMovieId(movieId);
    }
}
