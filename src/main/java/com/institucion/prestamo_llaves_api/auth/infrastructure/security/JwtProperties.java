package com.institucion.prestamo_llaves_api.auth.infrastructure.security;


import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Propiedades utilizadas para firmar y validar tokens JWT.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(

    @NotBlank
    String issuer,

    @NotBlank
    String secretBase64,

    @NotNull
    Duration accessTokenTtl
) {
}