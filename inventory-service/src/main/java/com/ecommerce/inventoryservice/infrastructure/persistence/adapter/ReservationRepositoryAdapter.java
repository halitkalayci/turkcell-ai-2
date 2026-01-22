package com.ecommerce.inventoryservice.infrastructure.persistence.adapter;

import com.ecommerce.inventoryservice.domain.model.Reservation;
import com.ecommerce.inventoryservice.domain.port.ReservationRepository;
import com.ecommerce.inventoryservice.infrastructure.persistence.entity.ReservationEntity;
import com.ecommerce.inventoryservice.infrastructure.persistence.mapper.ReservationMapper;
import com.ecommerce.inventoryservice.infrastructure.persistence.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing ReservationRepository domain port.
 * Bridges domain layer with JPA persistence infrastructure.
 * 
 * Uses:
 * - ReservationJpaRepository for persistence operations
 * - ReservationMapper for domain ↔ entity conversion
 * 
 * Contains NO business logic - only coordinates persistence.
 */
@Component
@RequiredArgsConstructor
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;
    private final ReservationMapper mapper;

    @Override
    public Reservation save(Reservation reservation) {
        // Check if reservation already exists in DB
        Optional<ReservationEntity> existingEntity = jpaRepository.findById(reservation.getId());
        
        ReservationEntity entityToSave;
        if (existingEntity.isPresent()) {
            // Update existing entity (preserves version for optimistic lock)
            entityToSave = existingEntity.get();
            mapper.updateEntity(reservation, entityToSave);
        } else {
            // Create new entity (Hibernate will generate ID and version)
            entityToSave = mapper.toEntity(reservation);
        }
        
        ReservationEntity savedEntity = jpaRepository.save(entityToSave);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Reservation> findExpiredReservations(Instant currentTime) {
        List<ReservationEntity> entities = jpaRepository.findExpiredReservations(currentTime);
        return mapper.toDomainList(entities);
    }
}
