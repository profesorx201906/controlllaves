package com.institucion.prestamo_llaves_api.notification.application;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;

/**
 * Obtiene una fotografía inmutable de los datos requeridos
 * para enviar la notificación.
 */
@Service
public class IncidentNotificationDataLoader {

    private final IncidentRepository incidentRepository;

    public IncidentNotificationDataLoader(
            IncidentRepository incidentRepository
    ) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * La transacción permanece abierta mientras se recorren
     * las relaciones lazy de la incidencia.
     */
    @Transactional(readOnly = true)
    public IncidentEmailMessage load(Long incidentId) {
        Incident incident = incidentRepository
            .findById(incidentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Incidencia",
                    incidentId
                )
            );

        Loan loan = incident.getLoan();
        User reporter = incident.getReportedByUser();

        return new IncidentEmailMessage(
            incident.getId(),
            loan.getId(),
            loan.getRoomKey().getId(),
            loan.getRoomKey().getRoom().getName(),
            reporter.getId(),
            reporter.getFullName(),
            reporter.getEmail(),
            incident.getIncidentType(),
            incident.getDescription(),
            incident.getReportedAt()
        );
    }
}