package com.institucion.prestamo_llaves_api.incident.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentResolutionAction;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;

/**
 * Incidencia visible para el administrador.
 */
public record IncidentAdminSummaryResult(
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

    public static IncidentAdminSummaryResult from(
            Incident incident
    ) {
        return new IncidentAdminSummaryResult(
            incident.getId(),
            incident.getLoan().getId(),
            incident.getLoan().getRoomKey().getId(),
            incident.getLoan()
                .getRoomKey()
                .getRoom()
                .getName(),
            incident.getReportedByUser().getId(),
            incident.getReportedByUser().getFullName(),
            incident.getReportedByUser().getEmail(),
            incident.getIncidentType(),
            incident.getDescription(),
            incident.getReportedAt(),
            incident.isResolved(),
            incident.getResolvedAt(),
            incident.getResolvedByUser() == null
                ? null
                : incident.getResolvedByUser().getId(),
            incident.getResolutionAction(),
            incident.getResolutionNote(),
            incident.getNotificationStatus(),
            incident.getNotificationAttempts()
        );
    }
}