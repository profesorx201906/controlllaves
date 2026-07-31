package com.institucion.prestamo_llaves_api.loan.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.loan.application.LoanQueryResult;

/**
 * Representación pública de un préstamo del usuario.
 */
public record UserLoanResponse(
    Long loanId,
    Long roomKeyId,
    Long roomId,
    String roomName,
    String roomDescription,
    Instant borrowedAt,
    Instant returnedAt,
    boolean active,
    KeyStatus keyStatus,
    boolean openIncident
) {

    public static UserLoanResponse from(
            LoanQueryResult result
    ) {
        return new UserLoanResponse(
            result.loanId(),
            result.roomKeyId(),
            result.roomId(),
            result.roomName(),
            result.roomDescription(),
            result.borrowedAt(),
            result.returnedAt(),
            result.active(),
            result.keyStatus(),
            result.openIncident()
        );
    }
}