package com.institucion.prestamo_llaves_api.room.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.room.domain.model.Room;

/**
 * Representación interna de un ambiente y su llave.
 */
public record RoomKeySummaryResult(
    Long roomId,
    String roomName,
    String description,
    boolean roomActive,
    Long keyId,
    KeyStatus keyStatus,
    Instant keyUpdatedAt
) {

    public static RoomKeySummaryResult from(
            RoomKey roomKey
    ) {
        Room room = roomKey.getRoom();

        return new RoomKeySummaryResult(
            room.getId(),
            room.getName(),
            room.getDescription(),
            room.isActive(),
            roomKey.getId(),
            roomKey.getStatus(),
            roomKey.getUpdatedAt()
        );
    }
}