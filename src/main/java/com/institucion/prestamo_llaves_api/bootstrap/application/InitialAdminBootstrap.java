package com.institucion.prestamo_llaves_api.bootstrap.application;


import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.bootstrap.infrastructure.config.BootstrapAdminProperties;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

/**
 * Crea el primer administrador cuando la aplicación
 * se inicia por primera vez.
 *
 * El proceso es idempotente:
 * si ya existe un administrador, no crea uno nuevo.
 */
@Component
public class InitialAdminBootstrap
        implements ApplicationRunner {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            InitialAdminBootstrap.class
        );

    private static final int MIN_PASSWORD_LENGTH = 12;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[A-Za-z0-9._%+-]+"
                + "@[A-Za-z0-9.-]+"
                + "\\.[A-Za-z]{2,}$"
        );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public InitialAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BootstrapAdminProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /**
     * Se ejecuta durante el arranque de Spring Boot.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!properties.enabled()) {
            LOGGER.info(
                "Inicialización del administrador deshabilitada"
            );

            return;
        }

        ValidatedAdminData adminData =
            validateProperties();

        /*
         * Primero comprobamos si el correo configurado
         * ya pertenece a una cuenta.
         */
        Optional<User> existingUser =
            userRepository.findByEmailIgnoreCase(
                adminData.email()
            );

        if (existingUser.isPresent()) {
            handleExistingUser(
                existingUser.get(),
                adminData.email()
            );

            return;
        }

        /*
         * Si ya existe otro administrador, no creamos uno
         * adicional aunque haya cambiado la variable de correo.
         *
         * Esto evita duplicar administradores cuando se modifica
         * accidentalmente la configuración de despliegue.
         */
        if (userRepository.existsByRole(
                UserRole.ADMINISTRADOR
        )) {
            LOGGER.warn(
                "Ya existe un administrador. "
                    + "Se omite la inicialización para el correo {}",
                adminData.email()
            );

            return;
        }

        String passwordHash =
            passwordEncoder.encode(
                adminData.password()
            );

        User administrator = new User(
            adminData.name(),
            adminData.email(),
            passwordHash,
            UserRole.ADMINISTRADOR
        );

        /*
         * El constructor de User establece:
         *
         * enabled = true
         * mustChangePassword = true
         */
        User savedAdministrator =
            userRepository.saveAndFlush(
                administrator
            );

        LOGGER.info(
            "Administrador inicial creado con id {} "
                + "y correo {}. Debe cambiar su contraseña.",
            savedAdministrator.getId(),
            savedAdministrator.getEmail()
        );
    }

    private void handleExistingUser(
            User existingUser,
            String configuredEmail
    ) {
        if (existingUser.getRole()
                != UserRole.ADMINISTRADOR) {

            /*
             * No elevamos automáticamente una cuenta normal.
             * Un error de configuración debe detener el arranque.
             */
            throw new IllegalStateException(
                "El correo configurado para el administrador "
                    + configuredEmail
                    + " ya pertenece a un usuario sin rol "
                    + "ADMINISTRADOR"
            );
        }

        LOGGER.info(
            "El administrador inicial con correo {} "
                + "ya existe. No se realizaron cambios.",
            configuredEmail
        );
    }

    private ValidatedAdminData validateProperties() {
        String name = requireText(
            properties.name(),
            "BOOTSTRAP_ADMIN_NAME"
        );

        String email = normalizeEmail(
            properties.email()
        );

        String password = requireText(
            properties.password(),
            "BOOTSTRAP_ADMIN_PASSWORD"
        );

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException(
                "BOOTSTRAP_ADMIN_EMAIL no contiene "
                    + "un correo válido"
            );
        }

        validatePassword(password);

        return new ValidatedAdminData(
            name,
            email,
            password
        );
    }

    private static void validatePassword(
            String password
    ) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "BOOTSTRAP_ADMIN_PASSWORD debe tener "
                    + "como mínimo "
                    + MIN_PASSWORD_LENGTH
                    + " caracteres"
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

            throw new IllegalStateException(
                "BOOTSTRAP_ADMIN_PASSWORD debe contener "
                    + "mayúscula, minúscula, número "
                    + "y carácter especial"
            );
        }
    }

    private static String normalizeEmail(
            String email
    ) {
        return requireText(
            email,
            "BOOTSTRAP_ADMIN_EMAIL"
        )
            .toLowerCase(Locale.ROOT);
    }

    private static String requireText(
            String value,
            String variableName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                variableName + " es obligatoria cuando "
                    + "BOOTSTRAP_ADMIN_ENABLED=true"
            );
        }

        return value.trim();
    }

    /**
     * Mantiene juntos los valores ya normalizados
     * y validados.
     */
    private record ValidatedAdminData(
        String name,
        String email,
        String password
    ) {
    }
}