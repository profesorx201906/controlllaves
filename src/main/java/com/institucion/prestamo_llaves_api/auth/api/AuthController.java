package com.institucion.prestamo_llaves_api.auth.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.institucion.prestamo_llaves_api.auth.api.dto.LoginRequest;
import com.institucion.prestamo_llaves_api.auth.api.dto.LoginResponse;
import com.institucion.prestamo_llaves_api.auth.application.AuthenticationApplicationService;
import com.institucion.prestamo_llaves_api.auth.application.LoginResult;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.institucion.prestamo_llaves_api.auth.api.dto.ChangePasswordRequest;
import com.institucion.prestamo_llaves_api.auth.application.PasswordChangeApplicationService;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidCredentialsException;

import jakarta.validation.Valid;

/**
 * Endpoints públicos de autenticación.
 */
@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationApplicationService authenticationService;

    private final PasswordChangeApplicationService passwordChangeService;

    public AuthController(
            AuthenticationApplicationService authenticationService,
            PasswordChangeApplicationService passwordChangeService) {
        this.authenticationService = authenticationService;

        this.passwordChangeService = passwordChangeService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResult result = authenticationService.login(
                request.email(),
                request.password());

        return ResponseEntity.ok(
                LoginResponse.from(result));
    }

    /**
     * Cambia la contraseña del usuario autenticado
     * y entrega un nuevo JWT.
     */
    @PostMapping("/change-password")
    public ResponseEntity<LoginResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(jwt);

        LoginResult result = passwordChangeService.changePassword(
                userId,
                request.currentPassword(),
                request.newPassword());

        return ResponseEntity.ok(
                LoginResponse.from(result));
    }

    /**
     * El subject fue generado por nuestra aplicación
     * como identificador numérico del usuario.
     */
    private static Long extractUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {

            throw new InvalidCredentialsException();
        }

        try {
            return Long.valueOf(jwt.getSubject());

        } catch (NumberFormatException exception) {
            throw new InvalidCredentialsException();
        }
    }
}