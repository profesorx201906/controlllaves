package com.institucion.prestamo_llaves_api.incident.application;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;
import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.room.domain.model.Room;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

@ExtendWith(MockitoExtension.class)
class IncidentApplicationServiceTest {

    private static final Instant BORROWED_AT =
        Instant.parse("2026-07-28T13:00:00Z");

    private static final Instant REPORTED_AT =
        Instant.parse("2026-07-28T14:00:00Z");

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RoomKeyRepository roomKeyRepository;

    @Mock
    private IncidentRepository incidentRepository;

    private IncidentApplicationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
            REPORTED_AT,
            ZoneOffset.UTC
        );

        service = new IncidentApplicationService(
            loanRepository,
            roomKeyRepository,
            incidentRepository,
            fixedClock
        );
    }

    @Test
    void shouldCreatePendingIncidentForActiveLoan() {
        TestData data = createActiveLoan();

        when(loanRepository.findRoomKeyIdByLoanId(20L))
            .thenReturn(Optional.of(10L));

        when(roomKeyRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(data.roomKey()));

        when(loanRepository.findActiveById(20L))
            .thenReturn(Optional.of(data.loan()));

        when(incidentRepository.saveAndFlush(any(Incident.class)))
            .thenAnswer(invocation -> {
                Incident incident = invocation.getArgument(0);

                ReflectionTestUtils.setField(
                    incident,
                    "id",
                    30L
                );

                return incident;
            });

        IncidentCreatedResult result =
            service.reportIncident(
                20L,
                5L,
                IncidentType.PERDIDA,
                "  La llave se extravió durante el traslado.  "
            );

        assertEquals(30L, result.incidentId());
        assertEquals(20L, result.loanId());
        assertEquals(10L, result.roomKeyId());
        assertEquals(5L, result.reportedByUserId());

        assertEquals(
            IncidentType.PERDIDA,
            result.incidentType()
        );

        assertEquals(
            "La llave se extravió durante el traslado.",
            result.description()
        );

        assertEquals(
            REPORTED_AT,
            result.reportedAt()
        );

        assertEquals(
            NotificationStatus.PENDIENTE,
            result.notificationStatus()
        );

        verify(incidentRepository)
            .saveAndFlush(any(Incident.class));
    }

    @Test
    void shouldRejectIncidentWhenLoanIsNotActive() {
        RoomKey roomKey = createRoomKey();

        ReflectionTestUtils.setField(
            roomKey,
            "id",
            10L
        );

        /*
         * Una llave disponible representa que el préstamo
         * histórico ya terminó.
         */
        when(loanRepository.findRoomKeyIdByLoanId(20L))
            .thenReturn(Optional.of(10L));

        when(roomKeyRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(roomKey));

        when(loanRepository.findActiveById(20L))
            .thenReturn(Optional.empty());

        BusinessRuleException exception =
            assertThrows(
                BusinessRuleException.class,
                () -> service.reportIncident(
                    20L,
                    5L,
                    IncidentType.INCIDENCIA,
                    "La llave presenta una novedad"
                )
            );

        assertEquals(
            "LOAN_NOT_ACTIVE",
            exception.getCode()
        );

        verify(incidentRepository, never())
            .saveAndFlush(any(Incident.class));
    }

    @Test
    void shouldRejectIncidentFromAnotherUser() {
        TestData data = createActiveLoan();

        when(loanRepository.findRoomKeyIdByLoanId(20L))
            .thenReturn(Optional.of(10L));

        when(roomKeyRepository.findByIdForUpdate(10L))
            .thenReturn(Optional.of(data.roomKey()));

        when(loanRepository.findActiveById(20L))
            .thenReturn(Optional.of(data.loan()));

        BusinessRuleException exception =
            assertThrows(
                BusinessRuleException.class,
                () -> service.reportIncident(
                    20L,
                    99L,
                    IncidentType.INCIDENCIA,
                    "Reporte realizado por otro usuario"
                )
            );

        assertEquals(
            "LOAN_NOT_OWNED_BY_USER",
            exception.getCode()
        );

        verify(incidentRepository, never())
            .saveAndFlush(any(Incident.class));
    }

    @Test
    void shouldRejectDescriptionLongerThanFiveHundredCharacters() {
        String description = "a".repeat(501);

        BusinessRuleException exception =
            assertThrows(
                BusinessRuleException.class,
                () -> service.reportIncident(
                    20L,
                    5L,
                    IncidentType.INCIDENCIA,
                    description
                )
            );

        assertEquals(
            "INCIDENT_DESCRIPTION_TOO_LONG",
            exception.getCode()
        );

        /*
         * La validación ocurre antes de consultar la BD.
         */
        verify(loanRepository, never())
            .findRoomKeyIdByLoanId(20L);
    }

    private static TestData createActiveLoan() {
        User user = new User(
            "Usuario de prueba",
            "usuario@example.com",
            "hash_de_prueba",
            UserRole.USUARIO
        );

        ReflectionTestUtils.setField(
            user,
            "id",
            5L
        );

        RoomKey roomKey = createRoomKey();

        ReflectionTestUtils.setField(
            roomKey,
            "id",
            10L
        );

        Loan loan = new Loan(
            roomKey,
            user,
            BORROWED_AT
        );

        ReflectionTestUtils.setField(
            loan,
            "id",
            20L
        );

        roomKey.markAsLoaned();

        return new TestData(
            user,
            roomKey,
            loan
        );
    }

    private static RoomKey createRoomKey() {
        Room room = new Room(
            "Ambiente 101",
            "Ambiente de formación"
        );

        return new RoomKey(room);
    }

    /**
     * Agrupa los objetos necesarios para las pruebas.
     */
    private record TestData(
        User user,
        RoomKey roomKey,
        Loan loan
    ) {
    }
}