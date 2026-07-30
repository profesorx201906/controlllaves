package com.institucion.prestamo_llaves_api.loan.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud para obtener una llave disponible.
 *
 * El userId no se recibe desde el cliente.
 */
public record CreateLoanRequest(

    @NotNull(
        message = "El identificador de la llave es obligatorio"
    )
    @Positive(
        message = "El identificador de la llave debe ser positivo"
    )
    Long roomKeyId
) {
}