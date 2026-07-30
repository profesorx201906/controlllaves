package com.institucion.prestamo_llaves_api.user.api.dto;


import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud administrativa para crear una cuenta.
 */
public record CreateUserRequest(

    @NotBlank(
        message = "El nombre completo es obligatorio"
    )
    @Size(
        max = 120,
        message = "El nombre completo no puede superar "
            + "120 caracteres"
    )
    String fullName,

    @NotBlank(
        message = "El correo es obligatorio"
    )
    @Email(
        message = "El correo no tiene un formato válido"
    )
    @Size(
        max = 180,
        message = "El correo no puede superar 180 caracteres"
    )
    String email,

    @NotBlank(
        message = "La contraseña temporal es obligatoria"
    )
    @Size(
        min = 12,
        max = 200,
        message = "La contraseña temporal debe tener "
            + "entre 12 y 200 caracteres"
    )
    String temporaryPassword,

    @NotNull(
        message = "El rol es obligatorio"
    )
    UserRole role
) {
}