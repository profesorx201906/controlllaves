package com.institucion.prestamo_llaves_api.key.domain.model;


import java.util.Objects;

import com.institucion.prestamo_llaves_api.room.domain.model.Room;
import com.institucion.prestamo_llaves_api.shared.domain.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Llave física asociada a un ambiente de formación.
 *
 * Cada ambiente posee una sola llave.
 */
@Entity
@Table(name = "room_keys")
public class RoomKey extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    private Long id;

    /**
     * La restricción unique coincide con uk_room_keys_room.
     *
     * La relación se mantiene unidireccional:
     * RoomKey conoce al ambiente, pero Room no necesita
     * conocer directamente a RoomKey.
     */
    @OneToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "room_id",
        nullable = false,
        unique = true
    )
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private KeyStatus status;

    /**
     * Control de concurrencia optimista.
     *
     * Hibernate incrementará este valor cuando la llave
     * sea modificada.
     */
    @Version
    @Column(
        name = "version",
        nullable = false
    )
    private long version;

    /**
     * Constructor requerido por JPA.
     */
    protected RoomKey() {
    }

    /**
     * Crea una llave disponible para un ambiente.
     */
    public RoomKey(Room room) {
        this.room = Objects.requireNonNull(
            room,
            "room es obligatorio"
        );

        this.status = KeyStatus.DISPONIBLE;
    }

    /**
     * Cambia el estado a PRESTADA.
     *
     * Evita prestar una llave que ya está ocupada.
     */
    public void markAsLoaned() {
        if (status != KeyStatus.DISPONIBLE) {
            throw new IllegalStateException(
                "La llave no se encuentra disponible"
            );
        }

        this.status = KeyStatus.PRESTADA;
    }

    /**
     * Cambia el estado a DISPONIBLE después de una devolución.
     */
    public void markAsAvailable() {
        if (status != KeyStatus.PRESTADA) {
            throw new IllegalStateException(
                "La llave no se encuentra prestada"
            );
        }

        this.status = KeyStatus.DISPONIBLE;
    }

    public boolean isAvailable() {
        return status == KeyStatus.DISPONIBLE;
    }

    public boolean isLoaned() {
        return status == KeyStatus.PRESTADA;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}