package com.institucion.prestamo_llaves_api.auth.application;


import java.time.Instant;

/**
 * Token generado y su fecha de expiración.
 */
public record IssuedToken(
    String value,
    Instant expiresAt
) {
}