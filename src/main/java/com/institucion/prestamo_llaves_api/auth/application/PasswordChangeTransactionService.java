package com.institucion.prestamo_llaves_api.auth.application;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUser;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

/**
 * Ejecuta exclusivamente la modificación transaccional
 * de la contraseña.
 */
@Service
public class PasswordChangeTransactionService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public PasswordChangeTransactionService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * Cambia la contraseña y devuelve una fotografía del usuario
     * después de la modificación.
     */
    @Transactional
    public AuthenticatedUser changePassword(
            Long userId,
            String currentPassword,
            String newPassword
    ) {
        validateIdentifier(userId);

        if (currentPassword == null
                || currentPassword.isBlank()) {

            throw new InvalidRequestException(
                "CURRENT_PASSWORD_REQUIRED",
                "La contraseña actual es obligatoria"
            );
        }

        User user = userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Usuario",
                    userId
                )
            );

        if (!user.isEnabled()) {
            throw new BusinessRuleException(
                "USER_DISABLED",
                "El usuario se encuentra deshabilitado"
            );
        }

        boolean currentPasswordMatches =
            passwordEncoder.matches(
                currentPassword,
                user.getPasswordHash()
            );

        if (!currentPasswordMatches) {
            throw new InvalidRequestException(
                "CURRENT_PASSWORD_INVALID",
                "La contraseña actual es incorrecta"
            );
        }

        passwordPolicy.validate(newPassword);

        /*
         * No se permite reutilizar la contraseña actual.
         */
        boolean sameAsCurrentPassword =
            passwordEncoder.matches(
                newPassword,
                user.getPasswordHash()
            );

        if (sameAsCurrentPassword) {
            throw new InvalidRequestException(
                "NEW_PASSWORD_MUST_DIFFER",
                "La nueva contraseña debe ser diferente "
                    + "de la contraseña actual"
            );
        }

        String newPasswordHash =
            passwordEncoder.encode(newPassword);

        /*
         * false indica que ya no será obligatorio volver
         * a cambiarla en el siguiente inicio de sesión.
         */
        user.changePasswordHash(
            newPasswordHash,
            false
        );

        /*
         * Fuerza la ejecución del UPDATE dentro
         * de esta transacción.
         */
        userRepository.flush();

        return AuthenticatedUser.from(user);
    }

    private static void validateIdentifier(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidRequestException(
                "INVALID_USER_IDENTIFIER",
                "El identificador del usuario no es válido"
            );
        }
    }
}