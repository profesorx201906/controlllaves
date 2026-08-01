package com.institucion.prestamo_llaves_api.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

import jakarta.persistence.LockModeType;

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

    /**
     * Obtiene un usuario con bloqueo de escritura.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :userId
            """)
    Optional<User> findByIdForUpdate(
            @Param("userId") Long userId);

    /**
     * Busca usuarios usando filtros opcionales.
     *
     * LOCATE evita tratar %, _ u otros caracteres como
     * comodines de una expresión LIKE.
     */
    @Query(value = """
            SELECT u
            FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:enabled IS NULL OR u.enabled = :enabled)
              AND (
                    :search IS NULL
                    OR LOCATE(:search, LOWER(u.fullName)) > 0
                    OR LOCATE(:search, LOWER(u.email)) > 0
                  )
            """, countQuery = """
            SELECT COUNT(u)
            FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:enabled IS NULL OR u.enabled = :enabled)
              AND (
                    :search IS NULL
                    OR LOCATE(:search, LOWER(u.fullName)) > 0
                    OR LOCATE(:search, LOWER(u.email)) > 0
                  )
            """)
    Page<User> searchUsers(
            @Param("search") String search,
            @Param("role") UserRole role,
            @Param("enabled") Boolean enabled,
            Pageable pageable);

    /**
     * Bloquea todos los administradores activos.
     *
     * Las operaciones de activación y desactivación utilizarán
     * siempre este método antes de modificar una cuenta. Esto
     * serializa las operaciones administrativas y evita que dos
     * solicitudes desactiven simultáneamente a los últimos
     * administradores.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM User u
            WHERE u.role = :role
              AND u.enabled = true
            ORDER BY u.id ASC
            """)
    List<User> findAllEnabledByRoleForUpdate(
            @Param("role") UserRole role);

    /**
     * Recupera únicamente los datos necesarios para validar
     * si un JWT sigue autorizado.
     */
    @Query("""
            SELECT
                user.id AS id,
                user.enabled AS enabled,
                user.tokenVersion AS tokenVersion
            FROM User user
            WHERE user.id = :userId
            """)
    Optional<UserSecurityStateView> findSecurityStateById(
            @Param("userId") Long userId);

    /**
     * Cuenta los usuarios habilitados en el sistema.
     *
     * Se utiliza para la métrica administrativa
     * del dashboard.
     */
    long countByEnabledTrue();

}