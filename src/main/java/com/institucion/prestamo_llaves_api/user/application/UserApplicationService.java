package com.institucion.prestamo_llaves_api.user.application;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.auth.application.PasswordPolicy;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;
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
    private static final int MAX_SEARCH_LENGTH = 100;

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
                    savedUser.getCreatedAt(),
                    savedUser.getUpdatedAt());

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

    /**
     * Consulta usuarios usando paginación y filtros opcionales.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<UserSummaryResult> searchUsers(
            String search,
            UserRole role,
            Boolean enabled,
            Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new InvalidRequestException(
                    "PAGINATION_REQUIRED",
                    "La consulta debe indicar una paginación válida");
        }

        String normalizedSearch = normalizeSearch(search);

        return userRepository
                .searchUsers(
                        normalizedSearch,
                        role,
                        enabled,
                        pageable)
                .map(UserSummaryResult::from);
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * actorUserId corresponde al administrador autenticado.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UserStatusChangedResult changeUserStatus(
            Long targetUserId,
            Long actorUserId,
            boolean enabled) {
        validateIdentifier(
                targetUserId,
                "targetUserId");

        validateIdentifier(
                actorUserId,
                "actorUserId");

        /*
         * Todas las modificaciones de estado bloquean primero
         * los administradores activos en el mismo orden.
         *
         * Esto evita carreras entre dos desactivaciones.
         */
        List<User> activeAdministrators = userRepository.findAllEnabledByRoleForUpdate(
                UserRole.ADMINISTRADOR);

        /*
         * Además de validar el rol del JWT, comprobamos que
         * la cuenta administrativa continúe activa en MariaDB.
         *
         * Esto reduce el riesgo de que un token antiguo perteneciente
         * a una cuenta desactivada cambie otros usuarios.
         */
        boolean actorIsActiveAdministrator = activeAdministrators
                .stream()
                .anyMatch(user -> user.getId().equals(actorUserId));

        if (!actorIsActiveAdministrator) {
            throw new AccessDeniedException(
                    "La cuenta administrativa ya no está activa");
        }

        User targetUser = userRepository
                .findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario",
                        targetUserId));

        if (enabled) {
            activateUser(targetUser);
        } else {
            deactivateUser(
                    targetUser,
                    activeAdministrators);
        }

        /*
         * Ejecuta el UPDATE dentro del método y permite que
         * updatedAt sea actualizado por la auditoría JPA.
         */
        userRepository.flush();

        return UserStatusChangedResult.from(
                targetUser);
    }

    private static void activateUser(User user) {
        if (user.isEnabled()) {
            throw new BusinessRuleException(
                    "USER_ALREADY_ENABLED",
                    "El usuario ya se encuentra habilitado");
        }

        user.activate();
    }

    private static void deactivateUser(
            User user,
            List<User> activeAdministrators) {
        if (!user.isEnabled()) {
            throw new BusinessRuleException(
                    "USER_ALREADY_DISABLED",
                    "El usuario ya se encuentra deshabilitado");
        }

        /*
         * Nunca debe desactivarse el último administrador activo.
         */
        if (user.getRole() == UserRole.ADMINISTRADOR
                && activeAdministrators.size() <= 1) {

            throw new BusinessRuleException(
                    "LAST_ACTIVE_ADMIN",
                    "No se puede desactivar el último "
                            + "administrador activo");
        }

        user.deactivate();
    }

    private static String normalizeSearch(
            String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String normalizedSearch = search
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedSearch.length() > MAX_SEARCH_LENGTH) {

            throw new InvalidRequestException(
                    "SEARCH_TOO_LONG",
                    "El texto de búsqueda no puede superar "
                            + MAX_SEARCH_LENGTH
                            + " caracteres");
        }

        return normalizedSearch;
    }

    private static void validateIdentifier(
            Long identifier,
            String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new InvalidRequestException(
                    "INVALID_IDENTIFIER",
                    fieldName
                            + " debe ser un identificador positivo");
        }
    }
}