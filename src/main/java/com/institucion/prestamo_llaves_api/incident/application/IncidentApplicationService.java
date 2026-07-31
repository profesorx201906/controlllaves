package com.institucion.prestamo_llaves_api.incident.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.prepost.PreAuthorize;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;

/**
 * Casos de uso relacionados con pérdidas e incidencias.
 */
@Service
public class IncidentApplicationService {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final LoanRepository loanRepository;
    private final RoomKeyRepository roomKeyRepository;
    private final IncidentRepository incidentRepository;
    private final Clock clock;

    public IncidentApplicationService(
            LoanRepository loanRepository,
            RoomKeyRepository roomKeyRepository,
            IncidentRepository incidentRepository,
            Clock clock) {
        this.loanRepository = loanRepository;
        this.roomKeyRepository = roomKeyRepository;
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    /**
     * Registra una pérdida o incidencia sobre un préstamo activo.
     *
     * El usuario solamente puede reportar incidencias asociadas
     * a sus propios préstamos.
     */
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO')")
    @Transactional
    public IncidentCreatedResult reportIncident(
            Long loanId,
            Long userId,
            IncidentType incidentType,
            String description) {
        validateIdentifier(loanId, "loanId");
        validateIdentifier(userId, "userId");

        IncidentType validatedType = validateIncidentType(incidentType);

        String normalizedDescription = validateDescription(description);

        /*
         * Primera consulta: obtenemos únicamente la llave
         * relacionada con el préstamo.
         *
         * Todavía no asumimos que el préstamo siga activo.
         */
        Long roomKeyId = loanRepository
                .findRoomKeyIdByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Préstamo",
                        loanId));

        /*
         * Utilizamos el mismo bloqueo que emplea la devolución.
         *
         * De esta forma, reportar y devolver una misma llave
         * no pueden completar sus modificaciones simultáneamente.
         */
        RoomKey roomKey = roomKeyRepository
                .findByIdForUpdate(roomKeyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Llave",
                        roomKeyId));

        /*
         * La validación del préstamo activo se realiza después
         * de obtener el bloqueo.
         *
         * Si una devolución terminó mientras esperábamos,
         * esta consulta ya no encontrará un préstamo activo.
         */
        Loan activeLoan = loanRepository
                .findActiveById(loanId)
                .orElseThrow(() -> new BusinessRuleException(
                        "LOAN_NOT_ACTIVE",
                        "El préstamo ya no se encuentra activo"));

        /*
         * Un préstamo activo requiere que la llave figure
         * como PRESTADA.
         */
        if (!roomKey.isLoaned()) {
            throw new BusinessRuleException(
                    "KEY_STATE_INCONSISTENT",
                    "El préstamo está activo, pero la llave figura "
                            + "como disponible");
        }

        /*
         * Validación anticipada para producir una respuesta
         * funcional comprensible.
         */
        if (incidentRepository
                .existsByLoan_IdAndResolvedAtIsNull(
                        activeLoan.getId())) {

            throw new BusinessRuleException(
                    "OPEN_INCIDENT_ALREADY_EXISTS",
                    "El préstamo ya tiene una incidencia abierta");
        }

        /*
         * El userId será obtenido posteriormente desde el JWT.
         */
        if (!activeLoan.belongsToUser(userId)) {
            throw new BusinessRuleException(
                    "LOAN_NOT_OWNED_BY_USER",
                    "El préstamo pertenece a otro usuario");
        }

        Instant reportedAt = clock.instant();

        Incident incident = new Incident(
                activeLoan,
                activeLoan.getUser(),
                validatedType,
                normalizedDescription,
                reportedAt);

        /*
         * La incidencia se registra con:
         *
         * notification_status = PENDIENTE
         * notification_attempts = 0
         */
        Incident savedIncident = incidentRepository.saveAndFlush(incident);

        return new IncidentCreatedResult(
                savedIncident.getId(),
                activeLoan.getId(),
                roomKey.getId(),
                activeLoan.getUser().getId(),
                savedIncident.getIncidentType(),
                savedIncident.getDescription(),
                savedIncident.getReportedAt(),
                savedIncident.getNotificationStatus());
    }

    private static IncidentType validateIncidentType(
            IncidentType incidentType) {
        if (incidentType == null) {
            throw new BusinessRuleException(
                    "INCIDENT_TYPE_REQUIRED",
                    "El tipo de incidencia es obligatorio");
        }

        return incidentType;
    }

    private static String validateDescription(
            String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessRuleException(
                    "INCIDENT_DESCRIPTION_REQUIRED",
                    "La descripción de la incidencia es obligatoria");
        }

        String normalizedDescription = description.trim();

        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {

            throw new BusinessRuleException(
                    "INCIDENT_DESCRIPTION_TOO_LONG",
                    "La descripción no puede superar "
                            + MAX_DESCRIPTION_LENGTH
                            + " caracteres");
        }

        return normalizedDescription;
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