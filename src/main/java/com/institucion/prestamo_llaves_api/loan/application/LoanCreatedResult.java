package com.institucion.prestamo_llaves_api.loan.application;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;

/**
 * Resultado inmutable del caso de uso de solicitud de llave.
 */
public record LoanCreatedResult(
    Long loanId,
    Long roomKeyId,
    Long userId,
    Instant borrowedAt,
    KeyStatus keyStatus
) {
}