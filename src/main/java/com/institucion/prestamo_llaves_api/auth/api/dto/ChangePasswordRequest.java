package com.institucion.prestamo_llaves_api.auth.api.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos requeridos para cambiar la contraseña.
 */
public record ChangePasswordRequest(

    @NotBlank(
        message = "La contraseña actual es obligatoria"
    )
    @Size(
        max = 200,
        message = "La contraseña actual supera "
            + "la longitud permitida"
    )
    String currentPassword,

    @NotBlank(
        message = "La nueva contraseña es obligatoria"
    )
    @Size(
        min = 12,
        max = 200,
        message = "La nueva contraseña debe tener "
            + "entre 12 y 200 caracteres"
    )
    String newPassword
) {
}