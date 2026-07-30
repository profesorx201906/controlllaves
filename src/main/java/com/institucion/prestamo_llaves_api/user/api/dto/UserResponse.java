package com.institucion.prestamo_llaves_api.user.api.dto;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.user.application.UserCreatedResult;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Representación pública de una cuenta.
 */
public record UserResponse(
    Long id,
    String fullName,
    String email,
    UserRole role,
    boolean enabled,
    boolean mustChangePassword,
    Instant createdAt
) {

    public static UserResponse from(
            UserCreatedResult result
    ) {
        return new UserResponse(
            result.id(),
            result.fullName(),
            result.email(),
            result.role(),
            result.enabled(),
            result.mustChangePassword(),
            result.createdAt()
        );
    }
}