package com.institucion.prestamo_llaves_api.incident.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentResolutionAction;
import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;

/**
 * Resultado de resolver una incidencia y liberar la llave.
 */
public record IncidentResolvedResult(
    Long incidentId,
    Long loanId,
    Long roomKeyId,
    Long resolvedByUserId,
    Instant resolvedAt,
    IncidentResolutionAction resolutionAction,
    String resolutionNote,
    Instant loanReturnedAt,
    KeyStatus keyStatus
) {
}