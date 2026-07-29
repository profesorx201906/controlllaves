package com.institucion.prestamo_llaves_api.auth.api.dto;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.auth.application.LoginResult;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Respuesta pública del inicio de sesión.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    UserData user
) {

    public static LoginResponse from(
            LoginResult result
    ) {
        return new LoginResponse(
            result.accessToken(),
            result.tokenType(),
            result.expiresAt(),
            new UserData(
                result.userId(),
                result.fullName(),
                result.email(),
                result.role(),
                result.mustChangePassword()
            )
        );
    }

    public record UserData(
        Long id,
        String fullName,
        String email,
        UserRole role,
        boolean mustChangePassword
    ) {
    }
}