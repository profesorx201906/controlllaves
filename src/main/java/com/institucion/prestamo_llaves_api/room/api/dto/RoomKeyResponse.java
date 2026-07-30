package com.institucion.prestamo_llaves_api.room.api.dto;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.room.application.RoomCreatedResult;
import com.institucion.prestamo_llaves_api.room.application.RoomKeySummaryResult;

/**
 * Representación pública de un ambiente y su llave.
 */
public record RoomKeyResponse(
    Long roomId,
    String roomName,
    String description,
    boolean roomActive,
    Long keyId,
    KeyStatus keyStatus,
    Instant keyUpdatedAt
) {

    public static RoomKeyResponse from(
            RoomCreatedResult result
    ) {
        return new RoomKeyResponse(
            result.roomId(),
            result.roomName(),
            result.description(),
            result.roomActive(),
            result.keyId(),
            result.keyStatus(),
            result.keyCreatedAt()
        );
    }

    public static RoomKeyResponse from(
            RoomKeySummaryResult result
    ) {
        return new RoomKeyResponse(
            result.roomId(),
            result.roomName(),
            result.description(),
            result.roomActive(),
            result.keyId(),
            result.keyStatus(),
            result.keyUpdatedAt()
        );
    }
}