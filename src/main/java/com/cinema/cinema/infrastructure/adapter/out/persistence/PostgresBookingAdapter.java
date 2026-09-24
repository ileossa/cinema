package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.Booking;
import com.cinema.cinema.domain.model.BookingItem;
import com.cinema.cinema.domain.model.BookingStatus;
import com.cinema.cinema.domain.port.out.BookingRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.BookingEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.BookingItemEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataBookingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresBookingAdapter implements BookingRepositoryOut {

    private final SpringDataBookingRepository springDataBookingRepository;

    public PostgresBookingAdapter(SpringDataBookingRepository springDataBookingRepository) {
        this.springDataBookingRepository = springDataBookingRepository;
    }

    @Override
    @Transactional
    public Booking save(Booking booking) {
        BookingEntity entity = toEntity(booking);
        BookingEntity saved = springDataBookingRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findById(Long id) {
        return springDataBookingRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByUserId(Long userId) {
        return springDataBookingRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime dateTime) {
        return springDataBookingRepository.findByStatusAndExpiresAtBefore(status, dateTime).stream()
                .map(this::toDomain)
                .toList();
    }

    private BookingEntity toEntity(Booking domain) {
        BookingEntity entity = new BookingEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setTotalAmount(domain.totalAmount());
        entity.setStatus(domain.status());
        entity.setExpiresAt(domain.expiresAt());
        entity.setCreatedAt(domain.createdAt());

        if (domain.items() != null) {
            List<BookingItemEntity> itemEntities = new ArrayList<>();
            for (BookingItem item : domain.items()) {
                BookingItemEntity itemEntity = new BookingItemEntity(
                        item.id(),
                        entity,
                        item.screeningId(),
                        item.seatCount(),
                        item.unitPrice()
                );
                itemEntities.add(itemEntity);
            }
            entity.setItems(itemEntities);
        }

        return entity;
    }

    private Booking toDomain(BookingEntity entity) {
        List<BookingItem> items = entity.getItems() != null
                ? entity.getItems().stream()
                .map(item -> new BookingItem(
                        item.getId(),
                        entity.getId(),
                        item.getScreeningId(),
                        item.getSeatCount(),
                        item.getUnitPrice()
                ))
                .toList()
                : List.of();

        return new Booking(
                entity.getId(),
                entity.getUserId(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                items
        );
    }
}
