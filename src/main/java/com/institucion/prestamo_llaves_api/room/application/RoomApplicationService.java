package com.institucion.prestamo_llaves_api.room.application;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.room.domain.model.Room;
import com.institucion.prestamo_llaves_api.room.infrastructure.persistence.RoomRepository;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;

/**
 * Casos de uso relacionados con ambientes y llaves.
 */
@Service
public class RoomApplicationService {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final int MAX_SEARCH_LENGTH = 100;

    private final RoomRepository roomRepository;
    private final RoomKeyRepository roomKeyRepository;

    public RoomApplicationService(
            RoomRepository roomRepository,
            RoomKeyRepository roomKeyRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomKeyRepository = roomKeyRepository;
    }

    /**
     * Crea un ambiente activo y su llave disponible.
     *
     * Ambas inserciones forman parte de la misma transacción.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public RoomCreatedResult createRoom(
            String name,
            String description
    ) {
        String normalizedName =
            validateName(name);

        String normalizedDescription =
            validateDescription(description);

        if (roomRepository.existsByNameIgnoreCase(
                normalizedName
        )) {
            throw new BusinessRuleException(
                "ROOM_NAME_ALREADY_REGISTERED",
                "Ya existe un ambiente registrado con ese nombre"
            );
        }

        Room room = new Room(
            normalizedName,
            normalizedDescription
        );

        try {
            /*
             * Primero se inserta el ambiente para obtener
             * su identificador.
             */
            Room savedRoom =
                roomRepository.saveAndFlush(room);

            /*
             * La llave comienza automáticamente en estado
             * DISPONIBLE.
             */
            RoomKey roomKey =
                new RoomKey(savedRoom);

            RoomKey savedRoomKey =
                roomKeyRepository.saveAndFlush(
                    roomKey
                );

            return new RoomCreatedResult(
                savedRoom.getId(),
                savedRoom.getName(),
                savedRoom.getDescription(),
                savedRoom.isActive(),
                savedRoom.getCreatedAt(),
                savedRoomKey.getId(),
                savedRoomKey.getStatus(),
                savedRoomKey.getCreatedAt()
            );

        } catch (DataIntegrityViolationException exception) {
            /*
             * La restricción UNIQUE sobre rooms.name y
             * room_keys.room_id protege contra solicitudes
             * concurrentes.
             */
            throw new BusinessRuleException(
                "ROOM_CREATION_CONFLICT",
                "No fue posible crear el ambiente porque "
                    + "el nombre o la llave ya están registrados",
                exception
            );
        }
    }

    /**
     * Consulta el catálogo visible para usuarios autenticados.
     */
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'USUARIO')"
    )
    @Transactional(readOnly = true)
    public Page<RoomKeySummaryResult>
            searchActiveRoomKeys(
                String search,
                KeyStatus status,
                Pageable pageable
            ) {

        validatePageable(pageable);

        String normalizedSearch =
            normalizeSearch(search);

        return roomKeyRepository
            .searchActiveRoomKeys(
                normalizedSearch,
                status,
                pageable
            )
            .map(RoomKeySummaryResult::from);
    }

    private static String validateName(
            String name
    ) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException(
                "ROOM_NAME_REQUIRED",
                "El nombre del ambiente es obligatorio"
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new InvalidRequestException(
                "ROOM_NAME_TOO_LONG",
                "El nombre del ambiente no puede superar "
                    + MAX_NAME_LENGTH
                    + " caracteres"
            );
        }

        return normalizedName;
    }

    private static String validateDescription(
            String description
    ) {
        if (description == null
                || description.isBlank()) {
            return null;
        }

        String normalizedDescription =
            description.trim();

        if (normalizedDescription.length()
                > MAX_DESCRIPTION_LENGTH) {

            throw new InvalidRequestException(
                "ROOM_DESCRIPTION_TOO_LONG",
                "La descripción no puede superar "
                    + MAX_DESCRIPTION_LENGTH
                    + " caracteres"
            );
        }

        return normalizedDescription;
    }

    private static String normalizeSearch(
            String search
    ) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String normalizedSearch = search
            .trim()
            .toLowerCase(Locale.ROOT);

        if (normalizedSearch.length()
                > MAX_SEARCH_LENGTH) {

            throw new InvalidRequestException(
                "SEARCH_TOO_LONG",
                "El texto de búsqueda no puede superar "
                    + MAX_SEARCH_LENGTH
                    + " caracteres"
            );
        }

        return normalizedSearch;
    }

    private static void validatePageable(
            Pageable pageable
    ) {
        if (pageable == null
                || pageable.isUnpaged()) {

            throw new InvalidRequestException(
                "PAGINATION_REQUIRED",
                "La consulta debe indicar una paginación válida"
            );
        }

        if (pageable.getPageSize() > 100) {
            throw new InvalidRequestException(
                "PAGE_SIZE_TOO_LARGE",
                "El tamaño máximo permitido es 100"
            );
        }
    }
}