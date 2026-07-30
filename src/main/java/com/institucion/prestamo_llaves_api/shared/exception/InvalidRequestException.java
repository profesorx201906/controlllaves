package com.institucion.prestamo_llaves_api.shared.exception;


/**
 * Representa datos formalmente válidos, pero no aceptables
 * para completar la operación solicitada.
 */
public class InvalidRequestException
        extends ApplicationException {

    public InvalidRequestException(
            String code,
            String message
    ) {
        super(code, message);
    }
}