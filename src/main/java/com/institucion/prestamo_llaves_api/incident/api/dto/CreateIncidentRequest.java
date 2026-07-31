package com.institucion.prestamo_llaves_api.incident.api.dto;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para reportar una pérdida o incidencia.
 *
 * El usuario y el préstamo no se reciben dentro del cuerpo:
 *
 * - userId se obtiene desde el JWT.
 * - loanId se obtiene desde la ruta.
 */
public record CreateIncidentRequest(

    @NotNull(
        message = "El tipo de incidencia es obligatorio"
    )
    IncidentType incidentType,

    @NotBlank(
        message = "La descripción es obligatoria"
    )
    @Size(
        max = 500,
        message = "La descripción no puede superar "
            + "500 caracteres"
    )
    String description
) {
}