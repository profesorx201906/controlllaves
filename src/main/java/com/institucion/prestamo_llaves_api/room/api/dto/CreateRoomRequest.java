package com.institucion.prestamo_llaves_api.room.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud administrativa para crear un ambiente.
 */
public record CreateRoomRequest(

    @NotBlank(
        message = "El nombre del ambiente es obligatorio"
    )
    @Size(
        max = 120,
        message = "El nombre del ambiente no puede superar "
            + "120 caracteres"
    )
    String name,

    @Size(
        max = 255,
        message = "La descripción no puede superar "
            + "255 caracteres"
    )
    String description
) {
}