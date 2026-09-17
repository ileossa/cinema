package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.in.GetMovieShowingsUseCase;
import com.cinema.cinema.domain.service.ScreeningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningController {

    private final GetMovieShowingsUseCase getMovieShowingsUseCase;

    public ScreeningController(GetMovieShowingsUseCase getMovieShowingsUseCase) {
        this.getMovieShowingsUseCase = getMovieShowingsUseCase;
    }

    @GetMapping
    public List<Screening> getScreeningsByMovie(@RequestParam("filmId") Long filmId){
        return getMovieShowingsUseCase.getMovieShowings(filmId);
    }
}
