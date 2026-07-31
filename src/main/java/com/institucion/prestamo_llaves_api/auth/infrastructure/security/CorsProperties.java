package com.institucion.prestamo_llaves_api.auth.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(
    prefix = "app.security.cors"
)
public record CorsProperties(

    @NotEmpty
    List<@NotBlank String> allowedOrigins
) {
}