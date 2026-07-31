package com.institucion.prestamo_llaves_api.incident.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentResolutionAction;
import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;
import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.key.domain.model.RoomKey;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;
import com.institucion.prestamo_llaves_api.user.domain.model.User;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

@Service
public class IncidentAdminApplicationService {

    private static final int MAX_SEARCH_LENGTH = 100;
    private static final int MAX_RESOLUTION_NOTE_LENGTH = 500;

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final RoomKeyRepository roomKeyRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    public IncidentAdminApplicationService(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            RoomKeyRepository roomKeyRepository,
            LoanRepository loanRepository,
            Clock clock
    ) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.roomKeyRepository = roomKeyRepository;
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<IncidentAdminSummaryResult> searchIncidents(
            String search,
            IncidentType incidentType,
            NotificationStatus notificationStatus,
            Boolean resolved,
            Pageable pageable
    ) {
        validatePageable(pageable);

        return incidentRepository
            .searchIncidents(
                normalizeSearch(search),
                incidentType,
                notificationStatus,
                resolved,
                pageable
            )
            .map(IncidentAdminSummaryResult::from);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public IncidentResolvedResult resolveIncident(
            Long incidentId,
            Long administratorId,
            IncidentResolutionAction action,
            String resolutionNote
    ) {
        validateIdentifier(incidentId, "incidentId");
        validateIdentifier(administratorId, "administratorId");

        if (action == null) {
            throw new InvalidRequestException(
                "RESOLUTION_ACTION_REQUIRED",
                "La acción de resolución es obligatoria"
            );
        }

        String normalizedNote =
            validateResolutionNote(resolutionNote);

        User administrator = userRepository
            .findByIdForUpdate(administratorId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Usuario",
                    administratorId
                )
            );

        if (!administrator.isEnabled()
                || administrator.getRole()
                    != UserRole.ADMINISTRADOR) {

            throw new AccessDeniedException(
                "La cuenta administrativa no está habilitada"
            );
        }

        Long roomKeyId = incidentRepository
            .findRoomKeyIdByIncidentId(incidentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Incidencia",
                    incidentId
                )
            );

        /*
         * Coordina la resolución con solicitudes,
         * devoluciones y nuevos reportes.
         */
        RoomKey roomKey = roomKeyRepository
            .findByIdForUpdate(roomKeyId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Llave",
                    roomKeyId
                )
            );

        Incident incident = incidentRepository
            .findByIdAndResolvedAtIsNull(incidentId)
            .orElseThrow(() ->
                new BusinessRuleException(
                    "INCIDENT_ALREADY_RESOLVED",
                    "La incidencia ya fue resuelta"
                )
            );

        Loan loan = incident.getLoan();

        if (!loan.isActive()) {
            throw new BusinessRuleException(
                "LOAN_STATE_INCONSISTENT",
                "La incidencia está abierta, pero el préstamo "
                    + "ya fue finalizado"
            );
        }

        if (!roomKey.isLoaned()) {
            throw new BusinessRuleException(
                "KEY_STATE_INCONSISTENT",
                "La incidencia está abierta, pero la llave "
                    + "figura como disponible"
            );
        }

        if (!roomKeyId.equals(
                loan.getRoomKey().getId()
        )) {
            throw new BusinessRuleException(
                "INCIDENT_KEY_INCONSISTENT",
                "La incidencia no corresponde a la llave bloqueada"
            );
        }

        Instant resolvedAt = clock.instant();

        incident.resolve(
            administrator,
            resolvedAt,
            action,
            normalizedNote
        );

        /*
         * La llave recuperada o reemplazada vuelve a estar
         * operativa y el préstamo queda cerrado.
         */
        loan.registerReturn(resolvedAt);
        roomKey.markAsAvailable();

        /*
         * Flush sincroniza todas las entidades administradas
         * dentro de la transacción.
         */
        incidentRepository.flush();

        return new IncidentResolvedResult(
            incident.getId(),
            loan.getId(),
            roomKey.getId(),
            administrator.getId(),
            incident.getResolvedAt(),
            incident.getResolutionAction(),
            incident.getResolutionNote(),
            loan.getReturnedAt(),
            roomKey.getStatus()
        );
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String normalized = search
            .trim()
            .toLowerCase(Locale.ROOT);

        if (normalized.length() > MAX_SEARCH_LENGTH) {
            throw new InvalidRequestException(
                "SEARCH_TOO_LONG",
                "La búsqueda no puede superar 100 caracteres"
            );
        }

        return normalized;
    }

    private static String validateResolutionNote(
            String note
    ) {
        if (note == null || note.isBlank()) {
            throw new InvalidRequestException(
                "RESOLUTION_NOTE_REQUIRED",
                "La observación de resolución es obligatoria"
            );
        }

        String normalized = note.trim();

        if (normalized.length()
                > MAX_RESOLUTION_NOTE_LENGTH) {

            throw new InvalidRequestException(
                "RESOLUTION_NOTE_TOO_LONG",
                "La observación de resolución no puede superar "
                    + "500 caracteres"
            );
        }

        return normalized;
    }

    private static void validatePageable(
            Pageable pageable
    ) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new InvalidRequestException(
                "PAGINATION_REQUIRED",
                "La consulta debe indicar paginación"
            );
        }

        if (pageable.getPageSize() > 100) {
            throw new InvalidRequestException(
                "PAGE_SIZE_TOO_LARGE",
                "El tamaño máximo permitido es 100"
            );
        }
    }

    private static void validateIdentifier(
            Long identifier,
            String fieldName
    ) {
        if (identifier == null || identifier <= 0) {
            throw new InvalidRequestException(
                "INVALID_IDENTIFIER",
                fieldName
                    + " debe ser un identificador positivo"
            );
        }
    }
}