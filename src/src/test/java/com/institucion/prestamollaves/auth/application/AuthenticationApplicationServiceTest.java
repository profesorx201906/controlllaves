package com.institucion.prestamo_llaves_api.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.institucion.prestamollaves.auth.infrastructure.security.AuthenticatedUser;
import com.institucion.prestamollaves.shared.exception.InvalidCredentialsException;
import com.institucion.prestamollaves.user.domain.model.UserRole;

@ExtendWith(MockitoExtension.class)
class AuthenticationApplicationServiceTest {

    private static final Instant EXPIRES_AT =
        Instant.parse("2026-07-29T17:00:00Z");

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Test
    void shouldAuthenticateAndReturnToken() {
        AuthenticatedUser user =
            new AuthenticatedUser(
                1L,
                "Administrador",
                "administrador@example.com",
                "{bcrypt}hash",
                UserRole.ADMINISTRADOR,
                true,
                true
            );

        Authentication authentication =
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
            );

        when(
            authenticationManager.authenticate(
                any(Authentication.class)
            )
        ).thenReturn(authentication);

        when(jwtTokenService.issueToken(user))
            .thenReturn(
                new IssuedToken(
                    "token-generado",
                    EXPIRES_AT
                )
            );

        AuthenticationApplicationService service =
            new AuthenticationApplicationService(
                authenticationManager,
                jwtTokenService
            );

        LoginResult result = service.login(
            " ADMINISTRADOR@EXAMPLE.COM ",
            "Clave_Segura_2026!"
        );

        assertEquals(
            "token-generado",
            result.accessToken()
        );

        assertEquals(
            "Bearer",
            result.tokenType()
        );

        assertEquals(
            "administrador@example.com",
            result.email()
        );

        assertEquals(
            true,
            result.mustChangePassword()
        );

        verify(authenticationManager)
            .authenticate(any(Authentication.class));
    }

    @Test
    void shouldReturnGenericErrorForInvalidCredentials() {
        when(
            authenticationManager.authenticate(
                any(Authentication.class)
            )
        ).thenThrow(
            new BadCredentialsException(
                "Bad credentials"
            )
        );

        AuthenticationApplicationService service =
            new AuthenticationApplicationService(
                authenticationManager,
                jwtTokenService
            );

        InvalidCredentialsException exception =
            assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(
                    "usuario@example.com",
                    "clave-incorrecta"
                )
            );

        assertEquals(
            "INVALID_CREDENTIALS",
            exception.getCode()
        );
    }
}