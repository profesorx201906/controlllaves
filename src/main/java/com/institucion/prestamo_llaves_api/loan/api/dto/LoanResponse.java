package com.institucion.prestamo_llaves_api.loan.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.loan.application.LoanCreatedResult;
import com.institucion.prestamo_llaves_api.loan.application.LoanReturnedResult;

/**
 * Representación pública de un préstamo.
 */
public record LoanResponse(
    Long loanId,
    Long roomKeyId,
    Long userId,
    Instant borrowedAt,
    Instant returnedAt,
    boolean active,
    KeyStatus keyStatus
) {

    public static LoanResponse from(
            LoanCreatedResult result
    ) {
        return new LoanResponse(
            result.loanId(),
            result.roomKeyId(),
            result.userId(),
            result.borrowedAt(),
            null,
            true,
            result.keyStatus()
        );
    }

    public static LoanResponse from(
            LoanReturnedResult result
    ) {
        return new LoanResponse(
            result.loanId(),
            result.roomKeyId(),
            result.userId(),
            result.borrowedAt(),
            result.returnedAt(),
            false,
            result.keyStatus()
        );
    }
}