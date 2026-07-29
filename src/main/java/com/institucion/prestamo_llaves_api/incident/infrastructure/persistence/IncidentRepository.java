package com.institucion.prestamo_llaves_api.incident.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.institucion.prestamo_llaves_api.incident.domain.model.Incident;
import com.institucion.prestamo_llaves_api.incident.domain.model.NotificationStatus;

import java.util.Collection;

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
}