package com.institucion.prestamo_llaves_api.shared.domain.model;


import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Clase base para entidades que contienen fechas de creación
 * y modificación.
 *
 * @MappedSuperclass indica que sus campos serán heredados por
 * las entidades, pero esta clase no tendrá una tabla propia.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    /**
     * Fecha de creación almacenada en UTC.
     * No puede modificarse después de insertar el registro.
     */
    @CreatedDate
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private Instant createdAt;

    /**
     * Fecha de la última modificación del registro.
     */
    @LastModifiedDate
    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}