package com.institucion.prestamo_llaves_api.notification.infrastructure.scheduling;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;
import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.notification.application.IncidentNotificationProcessor;
import com.institucion.prestamo_llaves_api.notification.infrastructure.config.NotificationProperties;

/**
 * Busca periódicamente incidencias pendientes de notificación.
 */
@Component
@ConditionalOnProperty(
    prefix = "app.notification",
    name = "processing-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class IncidentNotificationScheduler {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            IncidentNotificationScheduler.class
        );

    private static final List<NotificationStatus>
        RETRYABLE_STATUSES = List.of(
            NotificationStatus.PENDIENTE,
            NotificationStatus.FALLIDA
        );

    private final IncidentRepository incidentRepository;
    private final IncidentNotificationProcessor processor;
    private final NotificationProperties properties;

    public IncidentNotificationScheduler(
            IncidentRepository incidentRepository,
            IncidentNotificationProcessor processor,
            NotificationProperties properties
    ) {
        this.incidentRepository = incidentRepository;
        this.processor = processor;
        this.properties = properties;
    }

    @Scheduled(
        initialDelayString =
            "${app.notification.initial-delay-ms:10000}",
        fixedDelayString =
            "${app.notification.processing-delay-ms:30000}"
    )
    public void processPendingNotifications() {
        List<Incident> incidents =
            incidentRepository
                .findTop50ByNotificationStatusInAndNotificationAttemptsLessThanOrderByReportedAtAsc(
                    RETRYABLE_STATUSES,
                    properties.maxAttempts()
                );

        if (incidents.isEmpty()) {
            return;
        }

        LOGGER.info(
            "Procesando {} notificaciones de incidencias",
            incidents.size()
        );

        for (Incident incident : incidents) {
            processor.process(
                incident.getId()
            );
        }
    }
}