package com.cinema.cinema.infrastructure.adapter.out.persistence.repository;

import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataBookingRepository extends JpaRepository<BookingEntity, Long> {
    List<BookingEntity> findByUserId(Long userId);
    List<BookingEntity> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime dateTime);
}
