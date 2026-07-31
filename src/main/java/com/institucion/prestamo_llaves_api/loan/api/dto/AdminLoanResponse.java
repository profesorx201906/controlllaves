package com.institucion.prestamo_llaves_api.loan.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.loan.application.AdminLoanQueryResult;

/**
 * Representación administrativa de un préstamo.
 */
public record AdminLoanResponse(
    Long loanId,
    Long userId,
    String userFullName,
    String userEmail,
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

    public static AdminLoanResponse from(
            AdminLoanQueryResult result
    ) {
        return new AdminLoanResponse(
            result.loanId(),
            result.userId(),
            result.userFullName(),
            result.userEmail(),
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