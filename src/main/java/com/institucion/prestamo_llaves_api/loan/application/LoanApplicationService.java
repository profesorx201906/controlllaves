package com.institucion.prestamo_llaves_api.loan.application;

import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

/**
 * Casos de uso relacionados con préstamos y devoluciones.
 */
@Service
public class LoanApplicationService {

    private final UserRepository userRepository;
    private final RoomKeyRepository roomKeyRepository;
    private final LoanRepository loanRepository;
    private final IncidentRepository incidentRepository;
    private final Clock clock;

    public LoanApplicationService(
            UserRepository userRepository,
            RoomKeyRepository roomKeyRepository,
            LoanRepository loanRepository,
            IncidentRepository incidentRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.roomKeyRepository = roomKeyRepository;
        this.loanRepository = loanRepository;
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    /**
     * Solicita una llave disponible.
     *
     * Toda la operación ocurre dentro de una misma transacción.
     * El bloqueo pesimista evita que dos solicitudes modifiquen
     * simultáneamente la misma llave.
     */
    @Transactional
    public LoanCreatedResult requestLoan(
            Long keyId,
            Long userId) {
        validateIdentifier(keyId, "keyId");
        validateIdentifier(userId, "userId");

        /*
         * Primero validamos el usuario para reducir el tiempo
         * durante el cual mantendremos bloqueada la llave.
         */
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario",
                        userId));

        if (!user.isEnabled()) {
            throw new BusinessRuleException(
                    "USER_DISABLED",
                    "El usuario se encuentra deshabilitado");
        }

        /*
         * SELECT ... FOR UPDATE.
         *
         * El bloqueo se mantiene hasta que finalice
         * esta transacción.
         */
        RoomKey roomKey = roomKeyRepository
                .findByIdForUpdate(keyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Llave",
                        keyId));

        if (!roomKey.getRoom().isActive()) {
            throw new BusinessRuleException(
                    "ROOM_INACTIVE",
                    "El ambiente asociado a la llave está inactivo");
        }

        if (!roomKey.isAvailable()) {
            throw new BusinessRuleException(
                    "KEY_NOT_AVAILABLE",
                    "La llave no se encuentra disponible");
        }

        /*
         * Validación defensiva.
         *
         * Si la llave aparece DISPONIBLE, pero existe un préstamo
         * activo, el estado de los datos es inconsistente.
         */
        if (loanRepository
                .findActiveByRoomKeyId(keyId)
                .isPresent()) {

            throw new BusinessRuleException(
                    "KEY_STATE_INCONSISTENT",
                    "La llave tiene un préstamo activo, pero figura "
                            + "como disponible");
        }

        Instant borrowedAt = clock.instant();

        /*
         * El préstamo debe crearse antes de cambiar el estado,
         * porque su constructor exige que la llave esté disponible.
         */
        Loan loan = new Loan(
                roomKey,
                user,
                borrowedAt);

        roomKey.markAsLoaned();

        try {
            /*
             * saveAndFlush obliga a ejecutar el INSERT dentro
             * de este método.
             *
             * De esa forma podemos capturar aquí una posible
             * violación de la restricción única.
             */
            Loan savedLoan = loanRepository.saveAndFlush(loan);

            /*
             * No es necesario ejecutar save(roomKey).
             *
             * La llave fue cargada dentro de la transacción y está
             * administrada por JPA. Hibernate detectará el cambio.
             */
            return new LoanCreatedResult(
                    savedLoan.getId(),
                    roomKey.getId(),
                    user.getId(),
                    savedLoan.getBorrowedAt(),
                    roomKey.getStatus());

        } catch (DataIntegrityViolationException exception) {
            /*
             * Protección final proporcionada por:
             *
             * UNIQUE (room_key_id, active_slot)
             */
            throw new BusinessRuleException(
                    "KEY_ALREADY_LOANED",
                    "La llave ya posee un préstamo activo",
                    exception);
        }
    }

    /**
     * Registra la devolución de una llave.
     *
     * El usuario solamente puede devolver una llave asociada
     * a uno de sus préstamos activos.
     */
    @Transactional
    public LoanReturnedResult returnLoan(
            Long keyId,
            Long userId) {
        validateIdentifier(keyId, "keyId");
        validateIdentifier(userId, "userId");

        /*
         * Bloquea la llave hasta que finalice la transacción.
         *
         * Esto evita que otra operación intente prestar o devolver
         * simultáneamente la misma llave.
         */
        RoomKey roomKey = roomKeyRepository
                .findByIdForUpdate(keyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Llave",
                        keyId));

        if (!roomKey.isLoaned()) {
            throw new BusinessRuleException(
                    "KEY_NOT_LOANED",
                    "La llave no se encuentra prestada");
        }

        /*
         * La llave figura como PRESTADA, por lo que debe existir
         * exactamente un préstamo activo.
         */
        Loan activeLoan = loanRepository
                .findActiveByRoomKeyId(keyId)
                .orElseThrow(() -> new BusinessRuleException(
                        "KEY_STATE_INCONSISTENT",
                        "La llave figura como prestada, pero no tiene "
                                + "un préstamo activo"));

        /*
         * El identificador del usuario llegará posteriormente
         * desde el token JWT autenticado.
         */
        if (!activeLoan.belongsToUser(userId)) {
            throw new BusinessRuleException(
                    "LOAN_NOT_OWNED_BY_USER",
                    "El préstamo activo pertenece a otro usuario");
        }

        /*
         * Una pérdida o incidencia abierta debe ser gestionada
         * administrativamente antes de liberar la llave.
         */
        boolean hasOpenIncident = incidentRepository
                .existsByLoan_IdAndResolvedAtIsNull(
                        activeLoan.getId());

        if (hasOpenIncident) {
            throw new BusinessRuleException(
                    "OPEN_INCIDENT_EXISTS",
                    "La llave tiene una incidencia abierta y no puede "
                            + "ser devuelta mediante el flujo normal");
        }

        Instant returnedAt = clock.instant();

        /*
         * Ambas entidades están administradas por JPA dentro
         * de la transacción.
         */
        activeLoan.registerReturn(returnedAt);
        roomKey.markAsAvailable();

        /*
         * Fuerza la ejecución de los UPDATE antes de finalizar
         * el método y valida las restricciones de MariaDB.
         */
        loanRepository.flush();

        return new LoanReturnedResult(
                activeLoan.getId(),
                roomKey.getId(),
                activeLoan.getUser().getId(),
                activeLoan.getBorrowedAt(),
                activeLoan.getReturnedAt(),
                roomKey.getStatus());
    }

    private static void validateIdentifier(
            Long identifier,
            String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new BusinessRuleException(
                    "INVALID_IDENTIFIER",
                    fieldName
                            + " debe ser un identificador positivo");
        }
    }
}