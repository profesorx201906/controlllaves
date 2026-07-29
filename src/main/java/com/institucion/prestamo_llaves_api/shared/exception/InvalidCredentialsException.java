package com.institucion.prestamo_llaves_api.shared.exception;


/**
 * Representa un intento de autenticación fallido.
 */
public class InvalidCredentialsException
        extends ApplicationException {

    public InvalidCredentialsException() {
        super(
            "INVALID_CREDENTIALS",
            "El correo o la contraseña son incorrectos"
        );
    }
}