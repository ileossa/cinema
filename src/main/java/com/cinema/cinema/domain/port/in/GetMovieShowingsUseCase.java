package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Screening;
import java.util.List;

public interface GetMovieShowingsUseCase {
    List<Screening> getMovieShowings(Long id);
}
