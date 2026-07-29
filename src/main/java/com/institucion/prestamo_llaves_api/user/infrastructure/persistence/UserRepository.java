package com.institucion.prestamo_llaves_api.user.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.institucion.prestamo_llaves_api.user.domain.model.User;

import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Acceso a los usuarios registrados en la aplicación.
 */
public interface UserRepository
        extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por correo sin distinguir mayúsculas
     * y minúsculas.
     *
     * Se utilizará principalmente durante la autenticación.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Permite validar que no se registre dos veces el mismo
     * correo electrónico.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Comprueba si ya existe al menos un usuario con el rol
     * indicado.
     *
     * Se utilizará para evitar crear múltiples administradores
     * mediante el proceso de inicialización.
     */
    boolean existsByRole(UserRole role);
}