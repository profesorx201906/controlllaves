package com.institucion.prestamo_llaves_api.loan.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.room.domain.model.Room;

/**
 * Información de un préstamo visible para su propietario.
 */
public record LoanQueryResult(
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

    public static LoanQueryResult from(
            Loan loan,
            boolean openIncident
    ) {
        Room room = loan
            .getRoomKey()
            .getRoom();

        return new LoanQueryResult(
            loan.getId(),
            loan.getRoomKey().getId(),
            room.getId(),
            room.getName(),
            room.getDescription(),
            loan.getBorrowedAt(),
            loan.getReturnedAt(),
            loan.isActive(),
            loan.getRoomKey().getStatus(),
            openIncident
        );
    }
}