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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.institucion.prestamo_llaves_api.shared.exception.InvalidCredentialsException;
import com.institucion.prestamo_llaves_api.shared.web.PagedResponse;
import com.institucion.prestamo_llaves_api.user.api.dto.UpdateUserStatusRequest;
import com.institucion.prestamo_llaves_api.user.application.UserStatusChangedResult;
import com.institucion.prestamo_llaves_api.user.application.UserSummaryResult;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
            UserApplicationService userService) {
        this.userService = userService;
    }

    /**
     * Crea una cuenta habilitada con contraseña temporal.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserCreatedResult result = userService.createUser(
                request.fullName(),
                request.email(),
                request.temporaryPassword(),
                request.role());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(UserResponse.from(result));
    }

    /**
     * Consulta paginada de usuarios.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> searchUsers(

            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,

            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño debe ser como mínimo 1") @Max(value = 100, message = "El tamaño máximo permitido es 100") int size,

            @RequestParam(required = false) UserRole role,

            @RequestParam(required = false) Boolean enabled,

            @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no puede superar "
                    + "100 caracteres") String search) {
        /*
         * El cliente no controla directamente el campo de orden.
         * Evitamos nombres de propiedades inválidos o inesperados.
         */
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.asc("id")));

        Page<UserSummaryResult> result = userService.searchUsers(
                search,
                role,
                enabled,
                pageable);

        PagedResponse<UserResponse> response = PagedResponse.from(
                result,
                UserResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * Activa o desactiva una cuenta.
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> changeStatus(

            @PathVariable @Positive(message = "El identificador debe ser positivo") Long userId,

            @AuthenticationPrincipal Jwt jwt,

            @Valid @RequestBody UpdateUserStatusRequest request) {
        Long actorUserId = extractUserId(jwt);

        UserStatusChangedResult result = userService.changeUserStatus(
                userId,
                actorUserId,
                request.enabled());

        return ResponseEntity.ok(
                UserResponse.from(result));
    }

    /**
     * Obtiene el administrador autenticado desde el subject
     * firmado del JWT.
     */
    private static Long extractUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {

            throw new InvalidCredentialsException();
        }

        try {
            return Long.valueOf(
                    jwt.getSubject());

        } catch (NumberFormatException exception) {
            throw new InvalidCredentialsException();
        }
    }
}