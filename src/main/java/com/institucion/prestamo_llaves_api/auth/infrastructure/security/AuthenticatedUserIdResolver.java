package com.institucion.prestamo_llaves_api.auth.infrastructure.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.institucion.prestamo_llaves_api.shared.exception.InvalidCredentialsException;

/**
 * Obtiene el identificador del usuario autenticado
 * desde el claim subject del JWT.
 */
@Component
public class AuthenticatedUserIdResolver {

    public Long resolve(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {

            throw new InvalidCredentialsException();
        }

        try {
            Long userId = Long.valueOf(
                jwt.getSubject()
            );

            if (userId <= 0) {
                throw new InvalidCredentialsException();
            }

            return userId;

        } catch (NumberFormatException exception) {
            throw new InvalidCredentialsException();
        }
    }
}