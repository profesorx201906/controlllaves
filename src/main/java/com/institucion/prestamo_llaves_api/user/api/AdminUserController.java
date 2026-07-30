package com.institucion.prestamo_llaves_api.user.api;


import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.institucion.prestamo_llaves_api.user.api.dto.CreateUserRequest;
import com.institucion.prestamo_llaves_api.user.api.dto.UserResponse;
import com.institucion.prestamo_llaves_api.user.application.UserApplicationService;
import com.institucion.prestamo_llaves_api.user.application.UserCreatedResult;

import jakarta.validation.Valid;

/**
 * Endpoints de administración de usuarios.
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUserController {

    private final UserApplicationService userService;

    public AdminUserController(
            UserApplicationService userService
    ) {
        this.userService = userService;
    }

    /**
     * Crea una cuenta habilitada con contraseña temporal.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid
            @RequestBody CreateUserRequest request
    ) {
        UserCreatedResult result =
            userService.createUser(
                request.fullName(),
                request.email(),
                request.temporaryPassword(),
                request.role()
            );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{userId}")
            .buildAndExpand(result.id())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(UserResponse.from(result));
    }
}