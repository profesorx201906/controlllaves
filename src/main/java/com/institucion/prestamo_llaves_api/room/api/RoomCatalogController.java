package com.institucion.prestamo_llaves_api.room.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.room.api.dto.RoomKeyResponse;
import com.institucion.prestamo_llaves_api.room.application.RoomApplicationService;
import com.institucion.prestamo_llaves_api.room.application.RoomKeySummaryResult;
import com.institucion.prestamo_llaves_api.shared.web.PagedResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Catálogo de ambientes disponible para usuarios autenticados.
 */
@Validated
@RestController
@RequestMapping("/api/v1/rooms")
@PreAuthorize(
    "hasAnyRole('ADMINISTRADOR', 'USUARIO')"
)
public class RoomCatalogController {

    private final RoomApplicationService roomService;

    public RoomCatalogController(
            RoomApplicationService roomService
    ) {
        this.roomService = roomService;
    }

    /**
     * Consulta ambientes activos y el estado de sus llaves.
     */
    @GetMapping
    public ResponseEntity<
            PagedResponse<RoomKeyResponse>
        > searchRooms(

        @RequestParam(defaultValue = "0")
        @Min(
            value = 0,
            message = "La página no puede ser negativa"
        )
        int page,

        @RequestParam(defaultValue = "20")
        @Min(
            value = 1,
            message = "El tamaño debe ser como mínimo 1"
        )
        @Max(
            value = 100,
            message = "El tamaño máximo permitido es 100"
        )
        int size,

        @RequestParam(required = false)
        KeyStatus status,

        @RequestParam(required = false)
        @Size(
            max = 100,
            message = "La búsqueda no puede superar "
                + "100 caracteres"
        )
        String search
    ) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.asc("room.name"),
                Sort.Order.asc("id")
            )
        );

        Page<RoomKeySummaryResult> result =
            roomService.searchActiveRoomKeys(
                search,
                status,
                pageable
            );

        PagedResponse<RoomKeyResponse> response =
            PagedResponse.from(
                result,
                RoomKeyResponse::from
            );

        return ResponseEntity.ok(response);
    }
}