package com.institucion.prestamo_llaves_api.auth.api.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales recibidas por el endpoint de login.
 */
public record LoginRequest(

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(
        max = 180,
        message = "El correo no puede superar 180 caracteres"
    )
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
        max = 200,
        message = "La contraseña supera la longitud permitida"
    )
    String password
) {
}