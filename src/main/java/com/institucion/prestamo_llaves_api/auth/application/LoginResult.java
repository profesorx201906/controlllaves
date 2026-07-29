package com.institucion.prestamo_llaves_api.auth.application;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Resultado interno del inicio de sesión.
 */
public record LoginResult(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    Long userId,
    String fullName,
    String email,
    UserRole role,
    boolean mustChangePassword
) {
}