package com.institucion.prestamo_llaves_api.incident.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUserIdResolver;
import com.institucion.prestamo_llaves_api.incident.api.dto.IncidentAdminResponse;
import com.institucion.prestamo_llaves_api.incident.api.dto.IncidentResolvedResponse;
import com.institucion.prestamo_llaves_api.incident.api.dto.ResolveIncidentRequest;
import com.institucion.prestamo_llaves_api.incident.application.IncidentAdminApplicationService;
import com.institucion.prestamo_llaves_api.incident.application.IncidentAdminSummaryResult;
import com.institucion.prestamo_llaves_api.incident.application.IncidentResolvedResult;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;
import com.institucion.prestamo_llaves_api.shared.web.PagedResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/admin/incidents")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminIncidentController {

    private final IncidentAdminApplicationService service;
    private final AuthenticatedUserIdResolver userIdResolver;

    public AdminIncidentController(
            IncidentAdminApplicationService service,
            AuthenticatedUserIdResolver userIdResolver
    ) {
        this.service = service;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping
    public ResponseEntity<
            PagedResponse<IncidentAdminResponse>
        > searchIncidents(

        @RequestParam(defaultValue = "0")
        @Min(0)
        int page,

        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100)
        int size,

        @RequestParam(required = false)
        IncidentType incidentType,

        @RequestParam(required = false)
        NotificationStatus notificationStatus,

        @RequestParam(required = false)
        Boolean resolved,

        @RequestParam(required = false)
        @Size(max = 100)
        String search
    ) {
        Page<IncidentAdminSummaryResult> result =
            service.searchIncidents(
                search,
                incidentType,
                notificationStatus,
                resolved,
                PageRequest.of(
                    page,
                    size,
                    Sort.by(
                        Sort.Order.desc("reportedAt"),
                        Sort.Order.desc("id")
                    )
                )
            );

        return ResponseEntity.ok(
            PagedResponse.from(
                result,
                IncidentAdminResponse::from
            )
        );
    }

    @PostMapping("/{incidentId}/resolve")
    public ResponseEntity<IncidentResolvedResponse>
            resolveIncident(

        @PathVariable
        @Positive
        Long incidentId,

        @AuthenticationPrincipal
        Jwt jwt,

        @Valid
        @RequestBody
        ResolveIncidentRequest request
    ) {
        Long administratorId =
            userIdResolver.resolve(jwt);

        IncidentResolvedResult result =
            service.resolveIncident(
                incidentId,
                administratorId,
                request.resolutionAction(),
                request.resolutionNote()
            );

        return ResponseEntity.ok(
            IncidentResolvedResponse.from(result)
        );
    }
}