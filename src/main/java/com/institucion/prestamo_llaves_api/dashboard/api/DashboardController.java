package com.institucion.prestamo_llaves_api.dashboard.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUserIdResolver;
import com.institucion.prestamo_llaves_api.dashboard.api.dto.DashboardResponse;
import com.institucion.prestamo_llaves_api.dashboard.application.DashboardApplicationService;
import com.institucion.prestamo_llaves_api.dashboard.application.DashboardMetrics;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Endpoints relacionados con las métricas del dashboard.
 *
 * Las métricas devueltas dependen del rol funcional
 * almacenado en el JWT autenticado.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO')")
public class DashboardController {

    private static final String ROLE_CLAIM = "role";

    private final DashboardApplicationService dashboardService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public DashboardController(
            DashboardApplicationService dashboardService,
            AuthenticatedUserIdResolver userIdResolver) {
        this.dashboardService = dashboardService;
        this.userIdResolver = userIdResolver;
    }

    /**
     * Consulta las métricas correspondientes al usuario
     * autenticado.
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = userIdResolver.resolve(jwt);

        UserRole role = resolveRole(jwt);

        DashboardMetrics metrics = dashboardService.getMetrics(
                userId,
                role);

        return ResponseEntity.ok(
                DashboardResponse.from(metrics));
    }

    /**
     * Convierte el claim role del JWT al enum utilizado
     * por el dominio.
     */
    private static UserRole resolveRole(
            Jwt jwt) {
        if (jwt == null) {
            throw new InvalidRequestException(
                    "INVALID_USER_ROLE",
                    "No fue posible determinar el rol del usuario");
        }

        String roleClaim = jwt.getClaimAsString(ROLE_CLAIM);

        if (roleClaim == null ||
                roleClaim.isBlank()) {
            throw new InvalidRequestException(
                    "INVALID_USER_ROLE",
                    "El token no contiene el rol del usuario");
        }

        try {
            return UserRole.valueOf(
                    roleClaim.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "INVALID_USER_ROLE",
                    "El rol contenido en el token no es válido");
        }
    }
}