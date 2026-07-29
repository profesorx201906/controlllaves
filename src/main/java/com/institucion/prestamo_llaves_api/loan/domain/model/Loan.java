package com.institucion.prestamo_llaves_api.loan.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.user.domain.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Préstamo realizado por un usuario sobre una llave.
 *
 * Un préstamo está activo cuando:
 *
 * returnedAt = null
 * activeSlot = 1
 *
 * Un préstamo está finalizado cuando:
 *
 * returnedAt != null
 * activeSlot = null
 */
@Entity
@Table(name = "loans")
public class Loan {

    private static final byte ACTIVE_SLOT_VALUE = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_key_id", nullable = false)
    private RoomKey roomKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private Instant borrowedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    /**
     * Se utiliza junto con el índice único:
     *
     * UNIQUE (room_key_id, active_slot)
     *
     * Solo puede existir un registro con active_slot = 1
     * para una misma llave.
     */
    @Column(name = "active_slot")
    private Byte activeSlot;

    /**
     * Constructor requerido por JPA.
     */
    protected Loan() {
    }

    /**
     * Crea un préstamo activo.
     *
     * La hora debe ser proporcionada por la capa de aplicación,
     * preferiblemente mediante un Clock inyectado.
     */
    public Loan(
            RoomKey roomKey,
            User user,
            Instant borrowedAt) {
        this.roomKey = Objects.requireNonNull(
                roomKey,
                "roomKey es obligatorio");

        this.user = Objects.requireNonNull(
                user,
                "user es obligatorio");

        this.borrowedAt = Objects.requireNonNull(
                borrowedAt,
                "borrowedAt es obligatorio");

        if (!roomKey.isAvailable()) {
            throw new IllegalStateException(
                    "La llave no se encuentra disponible");
        }

        this.returnedAt = null;
        this.activeSlot = ACTIVE_SLOT_VALUE;
    }

    /**
     * Finaliza el préstamo.
     *
     * Al devolver:
     * returnedAt recibe la fecha.
     * activeSlot cambia a null.
     */
    public void registerReturn(Instant returnDate) {
        Objects.requireNonNull(
                returnDate,
                "returnDate es obligatorio");

        if (!isActive()) {
            throw new IllegalStateException(
                    "El préstamo ya fue finalizado");
        }

        if (returnDate.isBefore(borrowedAt)) {
            throw new IllegalArgumentException(
                    "La devolución no puede ser anterior al préstamo");
        }

        this.returnedAt = returnDate;
        this.activeSlot = null;
    }

    public boolean isActive() {
        return returnedAt == null
                && activeSlot != null
                && activeSlot == ACTIVE_SLOT_VALUE;
    }

    /**
     * Indica si el préstamo pertenece al usuario especificado.
     */
    public boolean belongsToUser(Long userId) {
        return userId != null
                && Objects.equals(user.getId(), userId);
    }

    public Long getId() {
        return id;
    }

    public RoomKey getRoomKey() {
        return roomKey;
    }

    public User getUser() {
        return user;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public Byte getActiveSlot() {
        return activeSlot;
    }
}