package com.institucion.prestamo_llaves_api.dashboard.application;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.key.domain.model.KeyStatus;
import com.institucion.prestamo_llaves_api.key.infrastructure.persistence.RoomKeyRepository;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.room.infrastructure.persistence.RoomRepository;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;

/**
 * Consulta las métricas utilizadas por el dashboard.
 *
 * Las consultas se ejecutan mediante COUNT directamente
 * en la base de datos para evitar cargar entidades completas
 * en memoria.
 */
@Service
public class DashboardApplicationService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomKeyRepository roomKeyRepository;
    private final LoanRepository loanRepository;
    private final IncidentRepository incidentRepository;

    public DashboardApplicationService(
            UserRepository userRepository,
            RoomRepository roomRepository,
            RoomKeyRepository roomKeyRepository,
            LoanRepository loanRepository,
            IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomKeyRepository = roomKeyRepository;
        this.loanRepository = loanRepository;
        this.incidentRepository = incidentRepository;
    }

    /**
     * Obtiene las métricas correspondientes al usuario
     * autenticado.
     *
     * El rol recibido proviene del JWT previamente validado
     * por Spring Security.
     */
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO')")
    @Transactional(readOnly = true)
    public DashboardMetrics getMetrics(
            Long userId,
            UserRole role) {
        validateIdentifier(userId);
        validateRole(role);

        long availableKeys = roomKeyRepository
                .countByStatusAndRoom_ActiveTrue(
                        KeyStatus.DISPONIBLE);

        if (role == UserRole.ADMINISTRADOR) {
            return getAdministratorMetrics(
                    availableKeys);
        }

        return getUserMetrics(
                userId,
                availableKeys);
    }

    /**
     * Construye las métricas globales disponibles
     * para un administrador.
     */
    private DashboardMetrics getAdministratorMetrics(
            long availableKeys) {
        long loanedKeys = roomKeyRepository
                .countByStatusAndRoom_ActiveTrue(
                        KeyStatus.PRESTADA);

        long activeLoans = loanRepository
                .countByReturnedAtIsNullAndActiveSlotIsNotNull();

        long openIncidents = incidentRepository
                .countByResolvedAtIsNull();

        long activeUsers = userRepository
                .countByEnabledTrue();

        long activeRooms = roomRepository
                .countByActiveTrue();

        return DashboardMetrics.administrator(
                availableKeys,
                loanedKeys,
                activeLoans,
                openIncidents,
                activeUsers,
                activeRooms);
    }

    /**
     * Construye las métricas personales disponibles
     * para un usuario estándar.
     */
    private DashboardMetrics getUserMetrics(
            Long userId,
            long availableKeys) {
        long myActiveLoans = loanRepository
                .countByUser_IdAndReturnedAtIsNullAndActiveSlotIsNotNull(
                        userId);

        long myOpenIncidents = incidentRepository
                .countByReportedByUser_IdAndResolvedAtIsNull(
                        userId);

        return DashboardMetrics.user(
                availableKeys,
                myActiveLoans,
                myOpenIncidents);
    }

    /**
     * Impide ejecutar consultas con identificadores
     * ausentes o inválidos.
     */
    private static void validateIdentifier(
            Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidRequestException(
                    "INVALID_USER_IDENTIFIER",
                    "El identificador del usuario debe ser positivo");
        }
    }

    /**
     * Solo se admiten los roles funcionales definidos
     * por la aplicación.
     */
    private static void validateRole(
            UserRole role) {
        if (role == null) {
            throw new InvalidRequestException(
                    "INVALID_USER_ROLE",
                    "El rol del usuario es obligatorio");
        }

        if (role != UserRole.ADMINISTRADOR &&
                role != UserRole.USUARIO) {
            throw new InvalidRequestException(
                    "INVALID_USER_ROLE",
                    "El rol del usuario no es válido");
        }
    }
}