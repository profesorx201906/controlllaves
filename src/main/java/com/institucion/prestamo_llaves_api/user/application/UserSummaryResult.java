package com.institucion.prestamo_llaves_api.user.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Representación interna de un usuario consultado.
 */
public record UserSummaryResult(
    Long id,
    String fullName,
    String email,
    UserRole role,
    boolean enabled,
    boolean mustChangePassword,
    Instant createdAt,
    Instant updatedAt
) {

    public static UserSummaryResult from(User user) {
        return new UserSummaryResult(
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