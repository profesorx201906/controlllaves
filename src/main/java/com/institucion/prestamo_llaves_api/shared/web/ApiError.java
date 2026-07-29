package com.institucion.prestamo_llaves_api.shared.web;


import java.time.Instant;

/**
 * Estructura común para errores HTTP.
 */
public record ApiError(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path
) {
}