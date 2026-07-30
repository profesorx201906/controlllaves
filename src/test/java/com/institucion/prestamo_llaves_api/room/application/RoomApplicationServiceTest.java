package com.institucion.prestamo_llaves_api.room.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.room.domain.model.Room;
import com.institucion.prestamo_llaves_api.room.infrastructure.persistence.RoomRepository;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;

@ExtendWith(MockitoExtension.class)
class RoomApplicationServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomKeyRepository roomKeyRepository;

    private RoomApplicationService service;

    @BeforeEach
    void setUp() {
        service = new RoomApplicationService(
            roomRepository,
            roomKeyRepository
        );
    }

    @Test
    void shouldCreateRoomAndAvailableKey() {
        when(
            roomRepository.existsByNameIgnoreCase(
                "Ambiente 101"
            )
        ).thenReturn(false);

        when(
            roomRepository.saveAndFlush(
                any(Room.class)
            )
        ).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);

            ReflectionTestUtils.setField(
                room,
                "id",
                1L
            );

            return room;
        });

        when(
            roomKeyRepository.saveAndFlush(
                any(RoomKey.class)
            )
        ).thenAnswer(invocation -> {
            RoomKey roomKey =
                invocation.getArgument(0);

            ReflectionTestUtils.setField(
                roomKey,
                "id",
                10L
            );

            return roomKey;
        });

        RoomCreatedResult result =
            service.createRoom(
                "  Ambiente 101  ",
                "  Ambiente de formación  "
            );

        assertEquals(1L, result.roomId());
        assertEquals(10L, result.keyId());

        assertEquals(
            "Ambiente 101",
            result.roomName()
        );

        assertEquals(
            "Ambiente de formación",
            result.description()
        );

        assertTrue(result.roomActive());

        assertEquals(
            KeyStatus.DISPONIBLE,
            result.keyStatus()
        );

        verify(roomRepository)
            .saveAndFlush(any(Room.class));

        verify(roomKeyRepository)
            .saveAndFlush(any(RoomKey.class));
    }

    @Test
    void shouldRejectDuplicatedRoomName() {
        when(
            roomRepository.existsByNameIgnoreCase(
                "Ambiente 101"
            )
        ).thenReturn(true);

        BusinessRuleException exception =
            assertThrows(
                BusinessRuleException.class,
                () -> service.createRoom(
                    "Ambiente 101",
                    "Descripción"
                )
            );

        assertEquals(
            "ROOM_NAME_ALREADY_REGISTERED",
            exception.getCode()
        );

        verify(roomRepository, never())
            .saveAndFlush(any(Room.class));

        verify(roomKeyRepository, never())
            .saveAndFlush(any(RoomKey.class));
    }

    @Test
    void shouldSearchActiveRoomKeys() {
        Room room = new Room(
            "Ambiente 101",
            "Ambiente de formación"
        );

        ReflectionTestUtils.setField(
            room,
            "id",
            1L
        );

        RoomKey roomKey =
            new RoomKey(room);

        ReflectionTestUtils.setField(
            roomKey,
            "id",
            10L
        );

        PageRequest pageable =
            PageRequest.of(0, 20);

        when(
            roomKeyRepository.searchActiveRoomKeys(
                "ambiente",
                KeyStatus.DISPONIBLE,
                pageable
            )
        ).thenReturn(
            new PageImpl<>(
                List.of(roomKey),
                pageable,
                1
            )
        );

        Page<RoomKeySummaryResult> result =
            service.searchActiveRoomKeys(
                "  AMBIENTE  ",
                KeyStatus.DISPONIBLE,
                pageable
            );

        assertEquals(
            1,
            result.getTotalElements()
        );

        assertEquals(
            "Ambiente 101",
            result.getContent()
                .getFirst()
                .roomName()
        );

        assertEquals(
            KeyStatus.DISPONIBLE,
            result.getContent()
                .getFirst()
                .keyStatus()
        );
    }

    @Test
    void shouldRejectDescriptionLongerThanLimit() {
        String description =
            "a".repeat(256);

        InvalidRequestException exception =
            assertThrows(
                InvalidRequestException.class,
                () -> service.createRoom(
                    "Ambiente 102",
                    description
                )
            );

        assertEquals(
            "ROOM_DESCRIPTION_TOO_LONG",
            exception.getCode()
        );

        verify(roomRepository, never())
            .existsByNameIgnoreCase(any());
    }
}