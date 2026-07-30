package com.institucion.prestamo_llaves_api.user.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Resultado de activar o desactivar una cuenta.
 */
public record UserStatusChangedResult(
    Long id,
    String fullName,
    String email,
    UserRole role,
    boolean enabled,
    boolean mustChangePassword,
    Instant createdAt,
    Instant updatedAt
) {

    public static UserStatusChangedResult from(User user) {
        return new UserStatusChangedResult(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.isEnabled(),
            user.isMustChangePassword(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}