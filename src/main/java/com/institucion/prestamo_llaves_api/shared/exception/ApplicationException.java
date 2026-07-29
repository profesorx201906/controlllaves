
package com.institucion.prestamo_llaves_api.shared.exception;

/**
 * Excepción base para errores controlados de la aplicación.
 *
 * El código permitirá mapear posteriormente cada error
 * a una respuesta HTTP estructurada.
 */
public abstract class ApplicationException
        extends RuntimeException {

    private final String code;

    protected ApplicationException(
            String code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    protected ApplicationException(
            String code,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}