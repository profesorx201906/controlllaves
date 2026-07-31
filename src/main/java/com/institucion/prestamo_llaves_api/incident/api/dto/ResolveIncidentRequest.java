package com.institucion.prestamo_llaves_api.incident.api.dto;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentResolutionAction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveIncidentRequest(

    @NotNull(
        message = "La acción de resolución es obligatoria"
    )
    IncidentResolutionAction resolutionAction,

    @NotBlank(
        message = "La observación de resolución es obligatoria"
    )
    @Size(
        max = 500,
        message = "La observación no puede superar "
            + "500 caracteres"
    )
    String resolutionNote
) {
}