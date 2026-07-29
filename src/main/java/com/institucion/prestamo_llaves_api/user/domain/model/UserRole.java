package com.institucion.prestamo_llaves_api.user.domain.model;

/**
 * Roles autorizados dentro de la aplicación.
 *
 * Los nombres deben coincidir exactamente con la restricción
 * CHECK definida en MariaDB.
 */
public enum UserRole {

    ADMINISTRADOR,
    USUARIO
}
