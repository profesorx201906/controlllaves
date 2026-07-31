package com.institucion.prestamo_llaves_api.incident.domain.model;

/**
 * Acciones administrativas que permiten cerrar una incidencia
 * y volver a habilitar la llave.
 */
public enum IncidentResolutionAction {

    LLAVE_RECUPERADA,
    LLAVE_REEMPLAZADA
}