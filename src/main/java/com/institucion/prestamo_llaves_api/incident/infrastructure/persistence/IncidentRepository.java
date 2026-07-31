package com.institucion.prestamo_llaves_api.incident.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;

import java.util.Collection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.institucion.prestamo_llaves_api.incident.domain.model.IncidentType;

/**
 * Acceso a pérdidas e incidencias reportadas.
 */
public interface IncidentRepository
                extends JpaRepository<Incident, Long> {

        /**
         * Comprueba si un préstamo tiene una incidencia abierta.
         */
        boolean existsByLoan_IdAndResolvedAtIsNull(Long loanId);

        /**
         * Busca una incidencia únicamente cuando continúa abierta.
         */
        Optional<Incident> findByIdAndResolvedAtIsNull(
                        Long incidentId);

        /**
         * Recupera las incidencias abiertas con paginación,
         * comenzando por las más antiguas.
         */
        Page<Incident> findAllByResolvedAtIsNullOrderByReportedAtAsc(
                        Pageable pageable);

        /**
         * Recupera como máximo 50 notificaciones por procesar.
         *
         * El límite evita cargar todas las incidencias pendientes
         * en memoria durante un mismo ciclo.
         */
        List<Incident> findTop50ByNotificationStatusOrderByReportedAtAsc(
                        NotificationStatus notificationStatus);

        /**
         * Recupera hasta 50 incidencias pendientes de envío
         * o susceptibles de reintento.
         *
         * Las incidencias que alcanzaron el máximo de intentos
         * dejan de ser seleccionadas.
         */
        List<Incident> findTop50ByNotificationStatusInAndNotificationAttemptsLessThanOrderByReportedAtAsc(
                        Collection<NotificationStatus> statuses,
                        int maxAttempts);

        /**
         * Obtiene la llave asociada a una incidencia.
         */
        @Query("""
                        SELECT incident.loan.roomKey.id
                        FROM Incident incident
                        WHERE incident.id = :incidentId
                        """)
        Optional<Long> findRoomKeyIdByIncidentId(
                        @Param("incidentId") Long incidentId);

        /**
         * Consulta incidencias con filtros opcionales.
         */
        @EntityGraph(attributePaths = {
                        "loan",
                        "loan.roomKey",
                        "loan.roomKey.room",
                        "reportedByUser",
                        "resolvedByUser"
        })
        @Query(value = """
                        SELECT incident
                        FROM Incident incident
                        WHERE (
                            :incidentType IS NULL
                            OR incident.incidentType = :incidentType
                        )
                        AND (
                            :notificationStatus IS NULL
                            OR incident.notificationStatus = :notificationStatus
                        )
                        AND (
                            :resolved IS NULL
                            OR (
                                :resolved = true
                                AND incident.resolvedAt IS NOT NULL
                            )
                            OR (
                                :resolved = false
                                AND incident.resolvedAt IS NULL
                            )
                        )
                        AND (
                            :search IS NULL
                            OR LOCATE(
                                :search,
                                LOWER(incident.description)
                            ) > 0
                            OR LOCATE(
                                :search,
                                LOWER(incident.reportedByUser.fullName)
                            ) > 0
                            OR LOCATE(
                                :search,
                                LOWER(incident.reportedByUser.email)
                            ) > 0
                            OR LOCATE(
                                :search,
                                LOWER(incident.loan.roomKey.room.name)
                            ) > 0
                        )
                        """, countQuery = """
                        SELECT COUNT(incident)
                        FROM Incident incident
                        WHERE (
                            :incidentType IS NULL
                            OR incident.incidentType = :incidentType
                        )
                        AND (
                            :notificationStatus IS NULL
                            OR incident.notificationStatus = :notificationStatus
                        )
                        AND (
                            :resolved IS NULL
                            OR (
                                :resolved = true
                                AND incident.resolvedAt IS NOT NULL
                            )
                            OR (
                                :resolved = false
                                AND incident.resolvedAt IS NULL
                            )
                        )
                        AND (
                            :search IS NULL
                            OR LOCATE(
                                :search,
                                LOWER(incident.description)
                            ) > 0
                            OR LOCATE(
                                :search,
                                LOWER(incident.reportedByUser.fullName)
                            ) > 0
                            OR LOCATE(
                                :search,
                                LOWER(incident.reportedByUser.email)
                            ) > 0
                            OR LOCATE(
                                :search,
                                LOWER(incident.loan.roomKey.room.name)
                            ) > 0
                        )
                        """)
        Page<Incident> searchIncidents(
                        @Param("search") String search,
                        @Param("incidentType") IncidentType incidentType,
                        @Param("notificationStatus") NotificationStatus notificationStatus,
                        @Param("resolved") Boolean resolved,
                        Pageable pageable);
}