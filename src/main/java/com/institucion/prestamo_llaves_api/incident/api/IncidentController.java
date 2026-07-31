package com.institucion.prestamo_llaves_api.incident.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUserIdResolver;
import com.institucion.prestamo_llaves_api.incident.api.dto.CreateIncidentRequest;
import com.institucion.prestamo_llaves_api.incident.api.dto.IncidentResponse;
import com.institucion.prestamo_llaves_api.incident.application.IncidentApplicationService;
import com.institucion.prestamo_llaves_api.incident.application.IncidentCreatedResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * Endpoints para reportar pérdidas e incidencias.
 */
@Validated
@RestController
@RequestMapping("/api/v1/loans/{loanId}/incidents")
@PreAuthorize(
    "hasAnyRole('ADMINISTRADOR', 'USUARIO')"
)
public class IncidentController {

    private final IncidentApplicationService
        incidentService;

    private final AuthenticatedUserIdResolver
        userIdResolver;

    public IncidentController(
            IncidentApplicationService incidentService,
            AuthenticatedUserIdResolver userIdResolver
    ) {
        this.incidentService = incidentService;
        this.userIdResolver = userIdResolver;
    }

    /**
     * Registra una pérdida o incidencia sobre un préstamo.
     */
    @PostMapping
    public ResponseEntity<IncidentResponse>
            reportIncident(

        @PathVariable
        @Positive(
            message = "El identificador del préstamo "
                + "debe ser positivo"
        )
        Long loanId,

        @AuthenticationPrincipal
        Jwt jwt,

        @Valid
        @RequestBody
        CreateIncidentRequest request
    ) {
        Long userId =
            userIdResolver.resolve(jwt);

        IncidentCreatedResult result =
            incidentService.reportIncident(
                loanId,
                userId,
                request.incidentType(),
                request.description()
            );

        URI location = URI.create(
            "/api/v1/loans/"
                + loanId
                + "/incidents/"
                + result.incidentId()
        );

        return ResponseEntity
            .created(location)
            .body(IncidentResponse.from(result));
    }
}