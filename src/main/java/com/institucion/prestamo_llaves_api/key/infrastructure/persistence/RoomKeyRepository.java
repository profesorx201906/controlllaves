package com.institucion.prestamo_llaves_api.key.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

/**
 * Acceso a las llaves de los ambientes.
 */
public interface RoomKeyRepository
        extends JpaRepository<RoomKey, Long> {

    /**
     * Busca la llave asignada a un ambiente.
     *
     * El guion bajo indica expresamente la navegación:
     * room.id
     */
    Optional<RoomKey> findByRoom_Id(Long roomId);

    /**
     * Comprueba si un ambiente ya tiene una llave.
     */
    boolean existsByRoom_Id(Long roomId);

    /**
     * Lista las llaves por estado, pero únicamente cuando
     * el ambiente asociado continúa activo.
     */
    List<RoomKey> findAllByStatusAndRoom_ActiveTrueOrderByRoom_NameAsc(
            KeyStatus status);

    /**
     * Recupera una llave aplicando un bloqueo de escritura
     * sobre su registro en la base de datos.
     *
     * Este método se utilizará al prestar y devolver llaves.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT roomKey
            FROM RoomKey roomKey
            WHERE roomKey.id = :keyId
            """)
    Optional<RoomKey> findByIdForUpdate(
            @Param("keyId") Long keyId);

    /**
     * Consulta el catálogo de ambientes activos y sus llaves.
     *
     * Los filtros de búsqueda y estado son opcionales.
     * EntityGraph carga el ambiente junto con la llave y evita
     * consultas adicionales al construir la respuesta.
     */
    @EntityGraph(attributePaths = "room")
    @Query(value = """
            SELECT roomKey
            FROM RoomKey roomKey
            WHERE roomKey.room.active = true
              AND (
                    :status IS NULL
                    OR roomKey.status = :status
                  )
              AND (
                    :search IS NULL
                    OR LOCATE(
                        :search,
                        LOWER(roomKey.room.name)
                    ) > 0
                    OR LOCATE(
                        :search,
                        LOWER(
                            COALESCE(
                                roomKey.room.description,
                                ''
                            )
                        )
                    ) > 0
                  )
            """, countQuery = """
            SELECT COUNT(roomKey)
            FROM RoomKey roomKey
            WHERE roomKey.room.active = true
              AND (
                    :status IS NULL
                    OR roomKey.status = :status
                  )
              AND (
                    :search IS NULL
                    OR LOCATE(
                        :search,
                        LOWER(roomKey.room.name)
                    ) > 0
                    OR LOCATE(
                        :search,
                        LOWER(
                            COALESCE(
                                roomKey.room.description,
                                ''
                            )
                        )
                    ) > 0
                  )
            """)
    Page<RoomKey> searchActiveRoomKeys(
            @Param("search") String search,
            @Param("status") KeyStatus status,
            Pageable pageable);

}