package com.institucion.prestamo_llaves_api.notification.infrastructure.mail;


import java.time.format.DateTimeFormatter;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.institucion.prestamo_llaves_api.notification.application.IncidentEmailMessage;
import com.institucion.prestamo_llaves_api.notification.domain.IncidentEmailSender;
import com.institucion.prestamo_llaves_api.notification.infrastructure.config.NotificationProperties;

/**
 * Envía notificaciones de incidencias mediante SMTP.
 */
@Component
public class SmtpIncidentEmailSender
        implements IncidentEmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpIncidentEmailSender(
            JavaMailSender mailSender,
            NotificationProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(IncidentEmailMessage message) {
        SimpleMailMessage mailMessage =
            new SimpleMailMessage();

        mailMessage.setFrom(properties.fromEmail());
        mailMessage.setTo(
            properties.administratorEmail()
        );

        mailMessage.setSubject(
            buildSubject(message)
        );

        mailMessage.setText(
            buildBody(message)
        );

        mailSender.send(mailMessage);
    }

    private String buildSubject(
            IncidentEmailMessage message
    ) {
        return "%s Incidencia #%d - %s"
            .formatted(
                properties.subjectPrefix(),
                message.incidentId(),
                message.incidentType()
            );
    }

    private String buildBody(
            IncidentEmailMessage message
    ) {
        return """
            Se ha registrado una novedad en el sistema de préstamo de llaves.

            Identificador de incidencia: %d
            Tipo: %s
            Ambiente: %s
            Identificador de llave: %d
            Identificador de préstamo: %d

            Usuario que reporta:
            Nombre: %s
            Correo: %s
            Identificador: %d

            Fecha del reporte (UTC): %s

            Descripción:
            %s

            La llave permanece en estado PRESTADA hasta que la incidencia sea resuelta.
            """
            .formatted(
                message.incidentId(),
                message.incidentType(),
                message.roomName(),
                message.roomKeyId(),
                message.loanId(),
                message.reportedByName(),
                message.reportedByEmail(),
                message.reportedByUserId(),
                DateTimeFormatter.ISO_INSTANT.format(
                    message.reportedAt()
                ),
                message.description()
            );
    }
}