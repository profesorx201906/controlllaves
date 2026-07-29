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

import jakarta.validation.Valid;

/**
 * Endpoints públicos de autenticación.
 */
@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationApplicationService
        authenticationService;

    public AuthController(
            AuthenticationApplicationService
                authenticationService
    ) {
        this.authenticationService =
            authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid
            @RequestBody LoginRequest request
    ) {
        LoginResult result =
            authenticationService.login(
                request.email(),
                request.password()
            );

        return ResponseEntity.ok(
            LoginResponse.from(result)
        );
    }
}