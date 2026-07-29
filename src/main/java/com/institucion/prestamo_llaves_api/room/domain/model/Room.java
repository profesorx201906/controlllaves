package com.institucion.prestamo_llaves_api.room.domain.model;


import com.institucion.prestamo_llaves_api.shared.domain.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ambiente de formación que posee una llave asociada.
 */
@Entity
@Table(name = "rooms")
public class Room extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    private Long id;

    @Column(
        name = "name",
        nullable = false,
        length = 120
    )
    private String name;

    @Column(
        name = "description",
        length = 255
    )
    private String description;

    /**
     * Permite retirar el ambiente del servicio sin eliminar
     * sus préstamos históricos.
     */
    @Column(
        name = "active",
        nullable = false
    )
    private boolean active;

    /**
     * Constructor requerido por JPA.
     */
    protected Room() {
    }

    /**
     * Crea un ambiente activo.
     */
    public Room(
            String name,
            String description
    ) {
        this.name = requireText(name, "name");
        this.description = normalizeOptionalText(description);
        this.active = true;
    }

    /**
     * Actualiza la información descriptiva del ambiente.
     */
    public void updateDetails(
            String name,
            String description
    ) {
        this.name = requireText(name, "name");
        this.description = normalizeOptionalText(description);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " es obligatorio"
            );
        }

        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}