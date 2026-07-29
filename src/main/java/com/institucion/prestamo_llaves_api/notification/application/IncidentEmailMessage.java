package com.institucion.prestamo_llaves_api.notification.application;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;

/**
 * Datos necesarios para construir el correo.
 *
 * No contiene entidades JPA ni relaciones lazy.
 */
public record IncidentEmailMessage(
    Long incidentId,
    Long loanId,
    Long roomKeyId,
    String roomName,
    Long reportedByUserId,
    String reportedByName,
    String reportedByEmail,
    IncidentType incidentType,
    String description,
    Instant reportedAt
) {
}