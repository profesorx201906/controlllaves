package com.institucion.prestamo_llaves_api.notification.infrastructure.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuración funcional de las notificaciones.
 */
@Validated
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(

    @NotBlank
    @Email
    String administratorEmail,

    @NotBlank
    @Email
    String fromEmail,

    @NotBlank
    String subjectPrefix,

    @Min(1)
    int maxAttempts
) {
}