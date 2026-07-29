package com.institucion.prestamo_llaves_api.loan.application;


import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;

/**
 * Resultado inmutable del caso de uso de devolución.
 */
public record LoanReturnedResult(
    Long loanId,
    Long roomKeyId,
    Long userId,
    Instant borrowedAt,
    Instant returnedAt,
    KeyStatus keyStatus
) {
}