package com.ecommerce.inventoryservice.infrastructure.persistence.entity;

import com.ecommerce.inventoryservice.domain.model.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for reservation persistence.
 * Pure persistence model - NO business logic.
 * All business operations are in domain.model.Reservation.
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ReservationItemEntity> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
