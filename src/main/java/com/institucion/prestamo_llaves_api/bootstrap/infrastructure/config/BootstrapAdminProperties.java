package com.institucion.prestamo_llaves_api.bootstrap.infrastructure.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Variables utilizadas exclusivamente para crear
 * el primer administrador.
 *
 * La validación se realiza únicamente cuando enabled=true.
 * De esta manera, los campos pueden permanecer vacíos
 * cuando el proceso está deshabilitado.
 */
@ConfigurationProperties(
    prefix = "app.bootstrap.admin"
)
public record BootstrapAdminProperties(
    boolean enabled,
    String name,
    String email,
    String password
) {
}