package com.institucion.prestamo_llaves_api.incident.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.application.IncidentAdminSummaryResult;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentResolutionAction;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;

public record IncidentAdminResponse(
    Long incidentId,
    Long loanId,
    Long roomKeyId,
    String roomName,
    Long reportedByUserId,
    String reportedByName,
    String reportedByEmail,
    IncidentType incidentType,
    String description,
    Instant reportedAt,
    boolean resolved,
    Instant resolvedAt,
    Long resolvedByUserId,
    IncidentResolutionAction resolutionAction,
    String resolutionNote,
    NotificationStatus notificationStatus,
    int notificationAttempts
) {

    public static IncidentAdminResponse from(
            IncidentAdminSummaryResult result
    ) {
        return new IncidentAdminResponse(
            result.incidentId(),
            result.loanId(),
            result.roomKeyId(),
            result.roomName(),
            result.reportedByUserId(),
            result.reportedByName(),
            result.reportedByEmail(),
            result.incidentType(),
            result.description(),
            result.reportedAt(),
            result.resolved(),
            result.resolvedAt(),
            result.resolvedByUserId(),
            result.resolutionAction(),
            result.resolutionNote(),
            result.notificationStatus(),
            result.notificationAttempts()
        );
    }
}