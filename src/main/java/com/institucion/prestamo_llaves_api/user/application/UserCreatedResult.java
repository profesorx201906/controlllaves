package com.institucion.prestamo_llaves_api.user.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Resultado de la creación administrativa de una cuenta.
 */
public record UserCreatedResult(
    Long id,
    String fullName,
    String email,
    UserRole role,
    boolean enabled,
    boolean mustChangePassword,
    Instant createdAt,
    Instant updatedAt
) {
}