package com.institucion.prestamo_llaves_api.user.domain.model;


import java.util.Locale;
import java.util.Objects;

import com.institucion.prestamo_llaves_api.shared.domain.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario registrado en la aplicación.
 *
 * Las cuentas solamente serán creadas por un administrador.
 */
@Entity
@Table(name = "app_users")
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    private Long id;

    @Column(
        name = "full_name",
        nullable = false,
        length = 120
    )
    private String fullName;

    @Column(
        name = "email",
        nullable = false,
        length = 180
    )
    private String email;

    /**
     * Contendrá una contraseña cifrada, nunca texto plano.
     */
    @Column(
        name = "password_hash",
        nullable = false,
        length = 255
    )
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "role",
        nullable = false,
        length = 20
    )
    private UserRole role;

    @Column(
        name = "enabled",
        nullable = false
    )
    private boolean enabled;

    @Column(
        name = "must_change_password",
        nullable = false
    )
    private boolean mustChangePassword;

    /**
     * Constructor requerido por JPA.
     *
     * Se declara protected para evitar crear usuarios vacíos
     * desde otras capas de la aplicación.
     */
    protected User() {
    }

    /**
     * Constructor usado para crear una cuenta nueva.
     *
     * passwordHash debe contener una contraseña previamente
     * cifrada con PasswordEncoder.
     */
    public User(
            String fullName,
            String email,
            String passwordHash,
            UserRole role
    ) {
        this.fullName = requireText(fullName, "fullName");
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role es obligatorio");

        this.enabled = true;
        this.mustChangePassword = true;
    }

    /**
     * Actualiza los datos básicos del usuario.
     */
    public void updateProfile(
            String fullName,
            String email
    ) {
        this.fullName = requireText(fullName, "fullName");
        this.email = normalizeEmail(email);
    }

    /**
     * Cambia el hash de la contraseña.
     *
     * La contraseña debe cifrarse antes de llamar este método.
     */
    public void changePasswordHash(
            String newPasswordHash,
            boolean requireChangeOnNextLogin
    ) {
        this.passwordHash =
            requireText(newPasswordHash, "newPasswordHash");

        this.mustChangePassword = requireChangeOnNextLogin;
    }

    /**
     * Confirma que el usuario ya cambió su contraseña temporal.
     */
    public void markPasswordAsChanged() {
        this.mustChangePassword = false;
    }

    public void changeRole(UserRole newRole) {
        this.role = Objects.requireNonNull(
            newRole,
            "newRole es obligatorio"
        );
    }

    public void activate() {
        this.enabled = true;
    }

    public void deactivate() {
        this.enabled = false;
    }

    /**
     * Normaliza el correo para evitar diferencias por mayúsculas
     * y espacios.
     */
    private static String normalizeEmail(String email) {
        return requireText(email, "email")
            .toLowerCase(Locale.ROOT);
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " es obligatorio"
            );
        }

        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}