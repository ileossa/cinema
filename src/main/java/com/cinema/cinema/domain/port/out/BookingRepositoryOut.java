package com.cinema.cinema.domain.port.out;


import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepositoryOut {
    Booking save(Booking booking);
    Optional<Booking> findById(Long id);
    List<Booking> findByUserId(Long userId);
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime dateTime);
}