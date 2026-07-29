package com.institucion.prestamo_llaves_api.incident.application;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;

/**
 * Resultado inmutable del registro de una incidencia.
 */
public record IncidentCreatedResult(
    Long incidentId,
    Long loanId,
    Long roomKeyId,
    Long reportedByUserId,
    IncidentType incidentType,
    String description,
    Instant reportedAt,
    NotificationStatus notificationStatus
) {
}