package com.institucion.prestamo_llaves_api.incident.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.application.IncidentResolvedResult;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentResolutionAction;
import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;

public record IncidentResolvedResponse(
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

    public static IncidentResolvedResponse from(
            IncidentResolvedResult result
    ) {
        return new IncidentResolvedResponse(
            result.incidentId(),
            result.loanId(),
            result.roomKeyId(),
            result.resolvedByUserId(),
            result.resolvedAt(),
            result.resolutionAction(),
            result.resolutionNote(),
            result.loanReturnedAt(),
            result.keyStatus()
        );
    }
}