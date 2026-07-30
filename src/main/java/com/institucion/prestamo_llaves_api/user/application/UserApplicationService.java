package com.institucion.prestamo_llaves_api.user.application;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.auth.application.PasswordPolicy;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

/**
 * Casos de uso administrativos relacionados con usuarios.
 */
@Service
public class UserApplicationService {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_EMAIL_LENGTH = 180;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public UserApplicationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * Crea una cuenta con contraseña temporal.
     *
     * Solo un administrador autenticado puede ejecutar
     * este caso de uso.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UserCreatedResult createUser(
            String fullName,
            String email,
            String temporaryPassword,
            UserRole role) {
        String normalizedName = validateFullName(fullName);

        String normalizedEmail = validateEmail(email);

        UserRole validatedRole = validateRole(role);

        passwordPolicy.validate(
                temporaryPassword);

        /*
         * Validación anticipada para responder con un error
         * funcional comprensible.
         */
        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail)) {
            throw new BusinessRuleException(
                    "EMAIL_ALREADY_REGISTERED",
                    "Ya existe un usuario registrado con ese correo");
        }

        String passwordHash = passwordEncoder.encode(
                temporaryPassword);

        User user = new User(
                normalizedName,
                normalizedEmail,
                passwordHash,
                validatedRole);

        try {
            /*
             * saveAndFlush fuerza el INSERT dentro del método.
             * Así se puede capturar una carrera sobre el correo
             * antes de salir de la transacción.
             */
            User savedUser = userRepository.saveAndFlush(user);

            return new UserCreatedResult(
                    savedUser.getId(),
                    savedUser.getFullName(),
                    savedUser.getEmail(),
                    savedUser.getRole(),
                    savedUser.isEnabled(),
                    savedUser.isMustChangePassword(),
                    savedUser.getCreatedAt());

        } catch (DataIntegrityViolationException exception) {
            /*
             * Protección definitiva proporcionada por:
             *
             * UNIQUE (email)
             */
            throw new BusinessRuleException(
                    "EMAIL_ALREADY_REGISTERED",
                    "Ya existe un usuario registrado con ese correo",
                    exception);
        }
    }

    private static String validateFullName(
            String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidRequestException(
                    "FULL_NAME_REQUIRED",
                    "El nombre completo es obligatorio");
        }

        String normalizedName = fullName.trim();

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new InvalidRequestException(
                    "FULL_NAME_TOO_LONG",
                    "El nombre completo no puede superar "
                            + MAX_NAME_LENGTH
                            + " caracteres");
        }

        return normalizedName;
    }

    private static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidRequestException(
                    "EMAIL_REQUIRED",
                    "El correo es obligatorio");
        }

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > MAX_EMAIL_LENGTH) {
            throw new InvalidRequestException(
                    "EMAIL_TOO_LONG",
                    "El correo no puede superar "
                            + MAX_EMAIL_LENGTH
                            + " caracteres");
        }

        return normalizedEmail;
    }

    private static UserRole validateRole(
            UserRole role) {
        if (role == null) {
            throw new InvalidRequestException(
                    "ROLE_REQUIRED",
                    "El rol es obligatorio");
        }

        return role;
    }
}