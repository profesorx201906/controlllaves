package com.institucion.prestamo_llaves_api.auth.application;


import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUser;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidCredentialsException;

/**
 * Caso de uso de inicio de sesión.
 */
@Service
public class AuthenticationApplicationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthenticationApplicationService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult login(
            String email,
            String rawPassword
    ) {
        String normalizedEmail =
            normalizeEmail(email);

        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        rawPassword
                    )
                );

            AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                    authentication.getPrincipal();

            IssuedToken issuedToken =
                jwtTokenService.issueToken(
                    authenticatedUser
                );

            return new LoginResult(
                issuedToken.value(),
                "Bearer",
                issuedToken.expiresAt(),
                authenticatedUser.id(),
                authenticatedUser.fullName(),
                authenticatedUser.email(),
                authenticatedUser.role(),
                authenticatedUser
                    .mustChangePassword()
            );

        } catch (AuthenticationException exception) {
            /*
             * No diferenciamos usuario inexistente,
             * deshabilitado o contraseña incorrecta.
             */
            throw new InvalidCredentialsException();
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException();
        }

        return email
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}