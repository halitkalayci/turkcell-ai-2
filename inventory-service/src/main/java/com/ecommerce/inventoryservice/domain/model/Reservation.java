package com.ecommerce.inventoryservice.domain.model;

import com.ecommerce.inventoryservice.domain.exception.InvalidReservationStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a stock reservation for an order.
 * Pure business logic with NO framework dependencies.
 * 
 * State transitions:
 * PENDING → CONFIRMED (order payment successful)
 * PENDING → CANCELLED (order cancelled)
 * PENDING → EXPIRED (TTL timeout)
 * Terminal states (CONFIRMED, CANCELLED, EXPIRED) cannot transition.
 */
public class Reservation {

    private final UUID id;
    private final UUID orderId;
    private final List<ReservationItem> items;
    private ReservationStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    /**
     * Creates a new reservation with PENDING status.
     *
     * @param id unique reservation identifier
     * @param orderId the order this reservation belongs to
     * @param items list of reserved items
     * @param createdAt creation timestamp
     * @param expiresAt expiration timestamp
     */
    public Reservation(UUID id, UUID orderId, List<ReservationItem> items, 
                       Instant createdAt, Instant expiresAt) {
        validateNotNull(id, "Reservation ID cannot be null");
        validateNotNull(orderId, "Order ID cannot be null");
        validateNotNull(items, "Items cannot be null");
        validateNotNull(createdAt, "Created at cannot be null");
        validateNotNull(expiresAt, "Expires at cannot be null");
        
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Reservation must have at least one item");
        }
        
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiration time cannot be before creation time");
        }
        
        this.id = id;
        this.orderId = orderId;
        this.items = new ArrayList<>(items); // Defensive copy
        this.status = ReservationStatus.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Factory method to create reservation with status (for reconstruction from persistence).
     *
     * @param id unique reservation identifier
     * @param orderId the order this reservation belongs to
     * @param items list of reserved items
     * @param status current status
     * @param createdAt creation timestamp
     * @param expiresAt expiration timestamp
     */
    public static Reservation reconstruct(UUID id, UUID orderId, List<ReservationItem> items,
                                          ReservationStatus status, Instant createdAt, Instant expiresAt) {
        Reservation reservation = new Reservation(id, orderId, items, createdAt, expiresAt);
        reservation.status = status;
        return reservation;
    }

    /**
     * Checks if this reservation has expired based on current time.
     *
     * @return true if current time is after expiresAt
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Confirms this reservation (order payment successful).
     * Can only confirm PENDING reservations that haven't expired.
     *
     * @throws InvalidReservationStateException if cannot be confirmed
     */
    public void confirm() {
        if (status.isTerminal()) {
            throw new InvalidReservationStateException(
                String.format("Cannot confirm reservation %s in %s state", id, status)
            );
        }
        
        if (isExpired()) {
            throw new InvalidReservationStateException(
                String.format("Cannot confirm expired reservation %s", id)
            );
        }
        
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Cancels this reservation (order cancelled, stock released).
     * Can only cancel PENDING reservations.
     *
     * @throws InvalidReservationStateException if cannot be cancelled
     */
    public void cancel() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException(
                String.format("Cannot cancel confirmed reservation %s", id)
            );
        }
        
        if (status == ReservationStatus.CANCELLED) {
            // Idempotent - already cancelled
            return;
        }
        
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Marks this reservation as expired.
     * Can only expire PENDING reservations.
     *
     * @throws InvalidReservationStateException if cannot be expired
     */
    public void expire() {
        if (status.isTerminal()) {
            // Idempotent - already in terminal state
            return;
        }
        
        this.status = ReservationStatus.EXPIRED;
    }

    /**
     * Checks if this reservation can be confirmed.
     *
     * @return true if status is PENDING and not expired
     */
    public boolean canBeConfirmed() {
        return status == ReservationStatus.PENDING && !isExpired();
    }

    /**
     * Checks if this reservation can be cancelled.
     *
     * @return true if status is PENDING (expired reservations can also be cancelled)
     */
    public boolean canBeCancelled() {
        return status == ReservationStatus.PENDING;
    }

    private void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    // Getters only (controlled state changes through business methods)

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<ReservationItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", status=" + status +
                ", items=" + items.size() +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
