package com.institucion.prestamo_llaves_api.notification.application;


import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;
import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;

/**
 * Actualiza el resultado de cada intento SMTP.
 */
@Service
public class IncidentNotificationStateService {

    private final IncidentRepository incidentRepository;

    public IncidentNotificationStateService(
            IncidentRepository incidentRepository
    ) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Utiliza una transacción independiente para almacenar
     * el resultado del envío exitoso.
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markAsSent(
            Long incidentId,
            Instant sentAt
    ) {
        Incident incident = findIncident(incidentId);

        /*
         * Evita contar nuevamente un envío ya confirmado.
         */
        if (incident.getNotificationStatus()
                == NotificationStatus.ENVIADA) {
            return;
        }

        incident.markNotificationAsSent(sentAt);
    }

    /**
     * Almacena un intento fallido sin modificar ni eliminar
     * el reporte original.
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markAsFailed(
            Long incidentId,
            String errorMessage
    ) {
        Incident incident = findIncident(incidentId);

        /*
         * Si otro proceso ya confirmó el envío, no retrocedemos
         * el estado a FALLIDA.
         */
        if (incident.getNotificationStatus()
                == NotificationStatus.ENVIADA) {
            return;
        }

        incident.markNotificationAsFailed(
            errorMessage
        );
    }

    private Incident findIncident(Long incidentId) {
        return incidentRepository
            .findById(incidentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Incidencia",
                    incidentId
                )
            );
    }
}