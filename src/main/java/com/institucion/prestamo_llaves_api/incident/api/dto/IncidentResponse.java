package com.institucion.prestamo_llaves_api.incident.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.application.IncidentCreatedResult;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;

/**
 * Representación pública de una incidencia registrada.
 */
public record IncidentResponse(
    Long incidentId,
    Long loanId,
    Long roomKeyId,
    Long reportedByUserId,
    IncidentType incidentType,
    String description,
    Instant reportedAt,
    NotificationStatus notificationStatus
) {

    public static IncidentResponse from(
            IncidentCreatedResult result
    ) {
        return new IncidentResponse(
            result.incidentId(),
            result.loanId(),
            result.roomKeyId(),
            result.reportedByUserId(),
            result.incidentType(),
            result.description(),
            result.reportedAt(),
            result.notificationStatus()
        );
    }
}