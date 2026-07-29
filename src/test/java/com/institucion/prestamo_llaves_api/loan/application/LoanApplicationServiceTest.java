package com.institucion.prestamo_llaves_api.loan.application;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.springframework.test.util.ReflectionTestUtils;

import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.room.domain.model.Room;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-27T15:30:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomKeyRepository roomKeyRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private IncidentRepository incidentRepository;

    private LoanApplicationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                FIXED_INSTANT,
                ZoneOffset.UTC);

        service = new LoanApplicationService(
                userRepository,
                roomKeyRepository,
                loanRepository,
                incidentRepository,
                fixedClock);
    }

    @Test
    void shouldCreateLoanAndMarkKeyAsLoaned() {
        User user = createEnabledUser();
        RoomKey roomKey = createAvailableKey();

        when(userRepository.findById(5L))
                .thenReturn(Optional.of(user));

        when(roomKeyRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(roomKey));

        when(loanRepository.findActiveByRoomKeyId(10L))
                .thenReturn(Optional.empty());

        when(loanRepository.saveAndFlush(any(Loan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanCreatedResult result = service.requestLoan(10L, 5L);

        assertEquals(
                FIXED_INSTANT,
                result.borrowedAt());

        assertEquals(
                KeyStatus.PRESTADA,
                result.keyStatus());

        assertTrue(roomKey.isLoaned());

        verify(loanRepository)
                .saveAndFlush(any(Loan.class));
    }

    @Test
    void shouldRejectLoanWhenKeyIsNotAvailable() {
        User user = createEnabledUser();
        RoomKey roomKey = createAvailableKey();

        roomKey.markAsLoaned();

        when(userRepository.findById(5L))
                .thenReturn(Optional.of(user));

        when(roomKeyRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(roomKey));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.requestLoan(10L, 5L));

        assertEquals(
                "KEY_NOT_AVAILABLE",
                exception.getCode());

        verify(loanRepository, never())
                .saveAndFlush(any(Loan.class));
    }

    @Test
    void shouldRejectLoanWhenUserIsDisabled() {
        User user = createEnabledUser();
        user.deactivate();

        when(userRepository.findById(5L))
                .thenReturn(Optional.of(user));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.requestLoan(10L, 5L));

        assertEquals(
                "USER_DISABLED",
                exception.getCode());

        verify(roomKeyRepository, never())
                .findByIdForUpdate(10L);

        verify(loanRepository, never())
                .saveAndFlush(any(Loan.class));
    }

    @Test
    void shouldRejectReturnWhenLoanBelongsToAnotherUser() {
        User owner = createEnabledUser();

        ReflectionTestUtils.setField(
                owner,
                "id",
                5L);

        RoomKey roomKey = createAvailableKey();

        ReflectionTestUtils.setField(
                roomKey,
                "id",
                10L);

        Loan loan = new Loan(
                roomKey,
                owner,
                FIXED_INSTANT.minusSeconds(3600));

        ReflectionTestUtils.setField(
                loan,
                "id",
                20L);

        roomKey.markAsLoaned();

        when(roomKeyRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(roomKey));

        when(loanRepository.findActiveByRoomKeyId(10L))
                .thenReturn(Optional.of(loan));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.returnLoan(10L, 99L));

        assertEquals(
                "LOAN_NOT_OWNED_BY_USER",
                exception.getCode());

        assertTrue(roomKey.isLoaned());
        assertTrue(loan.isActive());

        verify(loanRepository, never()).flush();
    }

    @Test
    void shouldRejectReturnWhenLoanHasOpenIncident() {
        User user = createEnabledUser();

        ReflectionTestUtils.setField(
                user,
                "id",
                5L);

        RoomKey roomKey = createAvailableKey();

        ReflectionTestUtils.setField(
                roomKey,
                "id",
                10L);

        Loan loan = new Loan(
                roomKey,
                user,
                FIXED_INSTANT.minusSeconds(3600));

        ReflectionTestUtils.setField(
                loan,
                "id",
                20L);

        roomKey.markAsLoaned();

        when(roomKeyRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(roomKey));

        when(loanRepository.findActiveByRoomKeyId(10L))
                .thenReturn(Optional.of(loan));

        when(
                incidentRepository
                        .existsByLoan_IdAndResolvedAtIsNull(20L))
                .thenReturn(true);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.returnLoan(10L, 5L));

        assertEquals(
                "OPEN_INCIDENT_EXISTS",
                exception.getCode());

        assertTrue(roomKey.isLoaned());
        assertTrue(loan.isActive());

        verify(loanRepository, never()).flush();
    }

    @Test
    void shouldRejectReturnWhenKeyIsAlreadyAvailable() {
        RoomKey roomKey = createAvailableKey();

        ReflectionTestUtils.setField(
                roomKey,
                "id",
                10L);

        when(roomKeyRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(roomKey));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.returnLoan(10L, 5L));

        assertEquals(
                "KEY_NOT_LOANED",
                exception.getCode());

        verify(loanRepository, never())
                .findActiveByRoomKeyId(10L);

        verify(loanRepository, never()).flush();
    }

    @Test
    void shouldReturnLoanAndMarkKeyAsAvailable() {
        User user = createEnabledUser();

        ReflectionTestUtils.setField(
                user,
                "id",
                5L);

        RoomKey roomKey = createAvailableKey();

        ReflectionTestUtils.setField(
                roomKey,
                "id",
                10L);

        /*
         * La llave debe estar prestada antes de ejecutar
         * el caso de uso de devolución.
         */
        Loan loan = new Loan(
                roomKey,
                user,
                FIXED_INSTANT.minusSeconds(3600));

        ReflectionTestUtils.setField(
                loan,
                "id",
                20L);

        roomKey.markAsLoaned();

        when(roomKeyRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(roomKey));

        when(loanRepository.findActiveByRoomKeyId(10L))
                .thenReturn(Optional.of(loan));

        when(
                incidentRepository
                        .existsByLoan_IdAndResolvedAtIsNull(20L))
                .thenReturn(false);

        LoanReturnedResult result = service.returnLoan(10L, 5L);

        assertEquals(
                20L,
                result.loanId());

        assertEquals(
                FIXED_INSTANT,
                result.returnedAt());

        assertEquals(
                KeyStatus.DISPONIBLE,
                result.keyStatus());

        assertTrue(roomKey.isAvailable());
        assertFalse(loan.isActive());

        verify(loanRepository).flush();
    }

    private static User createEnabledUser() {
        return new User(
                "Usuario de prueba",
                "usuario@example.com",
                "hash_de_prueba",
                UserRole.USUARIO);
    }

    private static RoomKey createAvailableKey() {
        Room room = new Room(
                "Ambiente 101",
                "Ambiente de formación");

        return new RoomKey(room);
    }

}