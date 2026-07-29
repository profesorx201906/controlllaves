package com.institucion.prestamo_llaves_api.shared.exception;


/**
 * Representa una operación válida técnicamente, pero
 * rechazada por una regla funcional.
 */
public class BusinessRuleException
        extends ApplicationException {

    public BusinessRuleException(
            String code,
            String message
    ) {
        super(code, message);
    }

    public BusinessRuleException(
            String code,
            String message,
            Throwable cause
    ) {
        super(code, message, cause);
    }
}