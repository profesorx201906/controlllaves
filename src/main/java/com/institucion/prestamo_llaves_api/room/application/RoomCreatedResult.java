package com.institucion.prestamo_llaves_api.room.application;

import java.time.Instant;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;

/**
 * Resultado de crear un ambiente y su llave.
 */
public record RoomCreatedResult(
    Long roomId,
    String roomName,
    String description,
    boolean roomActive,
    Instant roomCreatedAt,
    Long keyId,
    KeyStatus keyStatus,
    Instant keyCreatedAt
) {
}