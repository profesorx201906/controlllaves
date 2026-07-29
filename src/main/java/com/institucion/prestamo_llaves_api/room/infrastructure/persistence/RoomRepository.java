package com.institucion.prestamo_llaves_api.room.infrastructure.persistence;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.institucion.prestamo_llaves_api.room.domain.model.Room;

/**
 * Acceso a los ambientes de formación.
 */
public interface RoomRepository
        extends JpaRepository<Room, Long> {

    /**
     * Busca un ambiente por su nombre.
     */
    Optional<Room> findByNameIgnoreCase(String name);

    /**
     * Comprueba si ya existe un ambiente con el mismo nombre.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Recupera únicamente los ambientes activos,
     * ordenados alfabéticamente.
     */
    List<Room> findAllByActiveTrueOrderByNameAsc();
}