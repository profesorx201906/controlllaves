package com.institucion.prestamo_llaves_api.loan.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;

/**
 * Información de un préstamo visible para administradores.
 */
public record AdminLoanQueryResult(
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

    public static AdminLoanQueryResult from(
            Loan loan,
            boolean openIncident
    ) {
        return new AdminLoanQueryResult(
            loan.getId(),
            loan.getUser().getId(),
            loan.getUser().getFullName(),
            loan.getUser().getEmail(),
            loan.getRoomKey().getId(),
            loan.getRoomKey().getRoom().getId(),
            loan.getRoomKey().getRoom().getName(),
            loan.getRoomKey().getRoom().getDescription(),
            loan.getBorrowedAt(),
            loan.getReturnedAt(),
            loan.isActive(),
            loan.getRoomKey().getStatus(),
            openIncident
        );
    }
}