package com.institucion.prestamo_llaves_api.user.infrastructure.persistence;

/**
 * Proyección mínima utilizada durante la validación JWT.
 *
 * Evita cargar la entidad completa del usuario.
 */
public interface UserSecurityStateView {

    Long getId();

    Boolean getEnabled();

    Long getTokenVersion();
}