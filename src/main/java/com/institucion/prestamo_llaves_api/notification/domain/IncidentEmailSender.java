package com.institucion.prestamo_llaves_api.notification.domain;


import com.institucion.prestamo_llaves_api.notification.application.IncidentEmailMessage;

/**
 * Puerto de salida para notificar una incidencia.
 *
 * La implementación concreta utilizará SMTP.
 */
public interface IncidentEmailSender {

    void send(IncidentEmailMessage message);
}