package com.institucion.prestamo_llaves_api.room.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.institucion.prestamo_llaves_api.room.api.dto.CreateRoomRequest;
import com.institucion.prestamo_llaves_api.room.api.dto.RoomKeyResponse;
import com.institucion.prestamo_llaves_api.room.application.RoomApplicationService;
import com.institucion.prestamo_llaves_api.room.application.RoomCreatedResult;

import jakarta.validation.Valid;

/**
 * Endpoints administrativos para ambientes.
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/rooms")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminRoomController {

    private final RoomApplicationService roomService;

    public AdminRoomController(
            RoomApplicationService roomService
    ) {
        this.roomService = roomService;
    }

    /**
     * Crea un ambiente y su llave asociada.
     */
    @PostMapping
    public ResponseEntity<RoomKeyResponse>
            createRoom(
                @Valid
                @RequestBody
                CreateRoomRequest request
            ) {

        RoomCreatedResult result =
            roomService.createRoom(
                request.name(),
                request.description()
            );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{roomId}")
            .buildAndExpand(result.roomId())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(RoomKeyResponse.from(result));
    }
}