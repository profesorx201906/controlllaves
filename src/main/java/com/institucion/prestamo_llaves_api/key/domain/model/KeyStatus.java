package com.institucion.prestamo_llaves_api.key.domain.model;


/**
 * Estados permitidos para una llave.
 *
 * Deben coincidir exactamente con la restricción CHECK
 * de la tabla room_keys.
 */
public enum KeyStatus {

    DISPONIBLE,
    PRESTADA
}