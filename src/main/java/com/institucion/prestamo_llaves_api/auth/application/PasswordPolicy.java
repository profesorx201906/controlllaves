package com.institucion.prestamo_llaves_api.auth.application;



import org.springframework.stereotype.Component;

import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;

/**
 * Política centralizada para todas las contraseñas creadas
 * o modificadas dentro de la aplicación.
 */
@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 200;

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidRequestException(
                "PASSWORD_REQUIRED",
                "La contraseña es obligatoria"
            );
        }

        if (password.length() < MIN_LENGTH) {
            throw new InvalidRequestException(
                "WEAK_PASSWORD",
                "La contraseña debe tener como mínimo "
                    + MIN_LENGTH
                    + " caracteres"
            );
        }

        if (password.length() > MAX_LENGTH) {
            throw new InvalidRequestException(
                "PASSWORD_TOO_LONG",
                "La contraseña supera la longitud permitida"
            );
        }

        boolean hasUppercase =
            password.chars().anyMatch(
                Character::isUpperCase
            );

        boolean hasLowercase =
            password.chars().anyMatch(
                Character::isLowerCase
            );

        boolean hasDigit =
            password.chars().anyMatch(
                Character::isDigit
            );

        boolean hasSpecialCharacter =
            password.chars().anyMatch(
                character ->
                    !Character.isLetterOrDigit(character)
            );

        if (!hasUppercase
                || !hasLowercase
                || !hasDigit
                || !hasSpecialCharacter) {

            throw new InvalidRequestException(
                "WEAK_PASSWORD",
                "La contraseña debe contener mayúscula, "
                    + "minúscula, número y carácter especial"
            );
        }
    }
}