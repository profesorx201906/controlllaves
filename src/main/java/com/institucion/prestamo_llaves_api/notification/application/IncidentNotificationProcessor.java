package com.institucion.prestamo_llaves_api.notification.application;


import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.institucion.prestamo_llaves_api.notification.domain.IncidentEmailSender;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;

/**
 * Procesa una incidencia individual.
 *
 * No mantiene una transacción abierta durante la conexión SMTP.
 */
@Service
public class IncidentNotificationProcessor {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            IncidentNotificationProcessor.class
        );

    private static final int MAX_ERROR_LENGTH = 500;

    private final IncidentNotificationDataLoader dataLoader;
    private final IncidentEmailSender emailSender;
    private final IncidentNotificationStateService stateService;
    private final Clock clock;

    public IncidentNotificationProcessor(
            IncidentNotificationDataLoader dataLoader,
            IncidentEmailSender emailSender,
            IncidentNotificationStateService stateService,
            Clock clock
    ) {
        this.dataLoader = dataLoader;
        this.emailSender = emailSender;
        this.stateService = stateService;
        this.clock = clock;
    }

    public void process(Long incidentId) {
        IncidentEmailMessage message;

        try {
            message = dataLoader.load(incidentId);

        } catch (ResourceNotFoundException exception) {
            /*
             * La incidencia ya no existe. No hay estado
             * que actualizar.
             */
            LOGGER.warn(
                "No se encontró la incidencia {} durante "
                    + "el procesamiento de correo",
                incidentId
            );

            return;
        }

        try {
            emailSender.send(message);

            stateService.markAsSent(
                incidentId,
                clock.instant()
            );

            LOGGER.info(
                "Notificación de incidencia {} enviada",
                incidentId
            );

        } catch (Exception exception) {
            String safeError =
                buildSafeError(exception);

            try {
                stateService.markAsFailed(
                    incidentId,
                    safeError
                );

            } catch (Exception persistenceException) {
                LOGGER.error(
                    "No fue posible guardar el fallo SMTP "
                        + "de la incidencia {}",
                    incidentId,
                    persistenceException
                );
            }

            LOGGER.warn(
                "Falló el envío SMTP de la incidencia {}: {}",
                incidentId,
                safeError
            );
        }
    }

    /**
     * Evita almacenar trazas completas o saltos de línea
     * provenientes del proveedor SMTP.
     */
    private static String buildSafeError(
            Exception exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = "Error sin mensaje";
        }

        String safeError = (
            exception.getClass().getSimpleName()
                + ": "
                + message
        )
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim();

        if (safeError.length() > MAX_ERROR_LENGTH) {
            return safeError.substring(
                0,
                MAX_ERROR_LENGTH
            );
        }

        return safeError;
    }
}