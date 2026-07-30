package com.institucion.prestamo_llaves_api.user.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.user.application.UserCreatedResult;
import com.institucion.prestamo_llaves_api.user.application.UserStatusChangedResult;
import com.institucion.prestamo_llaves_api.user.application.UserSummaryResult;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Representación pública de un usuario.
 */
public record UserResponse(
    Long id,
    String fullName,
    String email,
    UserRole role,
    boolean enabled,
    boolean mustChangePassword,
    Instant createdAt,
    Instant updatedAt
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
            result.createdAt(),
            result.updatedAt()
        );
    }

    public static UserResponse from(
            UserSummaryResult result
    ) {
        return new UserResponse(
            result.id(),
            result.fullName(),
            result.email(),
            result.role(),
            result.enabled(),
            result.mustChangePassword(),
            result.createdAt(),
            result.updatedAt()
        );
    }

    public static UserResponse from(
            UserStatusChangedResult result
    ) {
        return new UserResponse(
            result.id(),
            result.fullName(),
            result.email(),
            result.role(),
            result.enabled(),
            result.mustChangePassword(),
            result.createdAt(),
            result.updatedAt()
        );
    }
}