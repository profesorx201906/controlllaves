package com.institucion.prestamo_llaves_api.user.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para activar o desactivar una cuenta.
 */
public record UpdateUserStatusRequest(

    @NotNull(
        message = "El estado enabled es obligatorio"
    )
    Boolean enabled
) {
}