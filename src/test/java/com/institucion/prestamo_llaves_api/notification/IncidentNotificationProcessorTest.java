package com.institucion.prestamo_llaves_api.notification;


import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.notification.application.IncidentEmailMessage;
import com.institucion.prestamo_llaves_api.notification.application.IncidentNotificationDataLoader;
import com.institucion.prestamo_llaves_api.notification.application.IncidentNotificationProcessor;
import com.institucion.prestamo_llaves_api.notification.application.IncidentNotificationStateService;
import com.institucion.prestamo_llaves_api.notification.domain.IncidentEmailSender;

@ExtendWith(MockitoExtension.class)
class IncidentNotificationProcessorTest {

    private static final Instant FIXED_INSTANT =
        Instant.parse("2026-07-28T16:00:00Z");

    @Mock
    private IncidentNotificationDataLoader dataLoader;

    @Mock
    private IncidentEmailSender emailSender;

    @Mock
    private IncidentNotificationStateService stateService;

    private IncidentNotificationProcessor processor;

    private IncidentEmailMessage message;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC
        );

        processor = new IncidentNotificationProcessor(
            dataLoader,
            emailSender,
            stateService,
            clock
        );

        message = new IncidentEmailMessage(
            30L,
            20L,
            10L,
            "Ambiente 101",
            5L,
            "Usuario de prueba",
            "usuario@example.com",
            IncidentType.PERDIDA,
            "La llave fue extraviada",
            FIXED_INSTANT.minusSeconds(600)
        );
    }

    @Test
    void shouldMarkNotificationAsSent() {
        when(dataLoader.load(30L))
            .thenReturn(message);

        processor.process(30L);

        verify(emailSender).send(message);

        verify(stateService).markAsSent(
            30L,
            FIXED_INSTANT
        );
    }

    @Test
    void shouldMarkNotificationAsFailedWhenSmtpFails() {
        when(dataLoader.load(30L))
            .thenReturn(message);

        org.mockito.Mockito
            .doThrow(
                new RuntimeException(
                    "Servidor SMTP no disponible"
                )
            )
            .when(emailSender)
            .send(message);

        processor.process(30L);

        verify(stateService).markAsFailed(
            org.mockito.ArgumentMatchers.eq(30L),
            contains(
                "Servidor SMTP no disponible"
            )
        );
    }
}