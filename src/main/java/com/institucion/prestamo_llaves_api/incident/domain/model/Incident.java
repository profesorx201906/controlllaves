package com.institucion.prestamo_llaves_api.incident.domain.model;

import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

import java.time.Instant;
import java.util.Objects;

import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.user.domain.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Incidencia o pérdida reportada sobre un préstamo activo.
 *
 * Una incidencia se almacena separadamente de la llave.
 * La llave permanece con estado PRESTADA mientras el caso
 * no sea resuelto.
 */
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private User reportedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 20)
    private IncidentType incidentType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private NotificationStatus notificationStatus;

    @Column(name = "notification_attempts", nullable = false)
    private int notificationAttempts;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "last_notification_error", length = 500)
    private String lastNotificationError;

    private static final byte OPEN_SLOT_VALUE = 1;

    /**
     * Constructor requerido por JPA.
     */
    protected Incident() {
    }

    /**
     * Crea una incidencia pendiente de notificación.
     */
    public Incident(
            Loan loan,
            User reportedByUser,
            IncidentType incidentType,
            String description,
            Instant reportedAt) {
        this.loan = Objects.requireNonNull(
                loan,
                "loan es obligatorio");

        this.reportedByUser = Objects.requireNonNull(
                reportedByUser,
                "reportedByUser es obligatorio");

        this.incidentType = Objects.requireNonNull(
                incidentType,
                "incidentType es obligatorio");

        this.description = requireText(
                description,
                "description");

        this.reportedAt = Objects.requireNonNull(
                reportedAt,
                "reportedAt es obligatorio");

        this.openSlot = OPEN_SLOT_VALUE;

        if (!loan.isActive()) {
            throw new IllegalStateException(
                    "Solo se pueden reportar incidencias "
                            + "sobre préstamos activos");
        }

        /*
         * Protege contra una configuración incorrecta del reloj.
         */
        if (reportedAt.isBefore(loan.getBorrowedAt())) {
            throw new IllegalArgumentException(
                    "La fecha del reporte no puede ser anterior "
                            + "a la fecha del préstamo");
        }

        if (this.description.length() > 500) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar 500 caracteres");
        }

        this.notificationStatus = NotificationStatus.PENDIENTE;

        this.notificationAttempts = 0;
    }

    /**
     * Registra que el correo fue enviado correctamente.
     */
    public void markNotificationAsSent(
            Instant notificationDate) {
        this.notifiedAt = Objects.requireNonNull(
                notificationDate,
                "notificationDate es obligatorio");

        this.notificationAttempts++;
        this.notificationStatus = NotificationStatus.ENVIADA;
        this.lastNotificationError = null;
    }

    /**
     * Registra un intento fallido de notificación.
     */
    public void markNotificationAsFailed(
            String errorMessage) {
        String normalizedError = requireText(
                errorMessage,
                "errorMessage");

        if (normalizedError.length() > 500) {
            normalizedError = normalizedError.substring(0, 500);
        }

        this.notificationAttempts++;
        this.notificationStatus = NotificationStatus.FALLIDA;
        this.lastNotificationError = normalizedError;
    }

    /**
     * Coloca nuevamente la incidencia en cola de notificación.
     */
    public void markNotificationAsPending() {
        if (notificationStatus == NotificationStatus.ENVIADA) {
            throw new IllegalStateException(
                    "Una notificación enviada no debe reenviarse "
                            + "sin una acción administrativa explícita");
        }

        this.notificationStatus = NotificationStatus.PENDIENTE;
    }

    /**
     * Resuelve administrativamente la incidencia.
     *
     * La liberación de la llave y el cierre del préstamo
     * serán coordinados por el servicio de aplicación.
     */
    public void resolve(
            User administrator,
            Instant resolutionDate,
            IncidentResolutionAction resolutionAction,
            String resolutionNote) {
        Objects.requireNonNull(
                administrator,
                "administrator es obligatorio");

        Objects.requireNonNull(
                resolutionDate,
                "resolutionDate es obligatorio");

        Objects.requireNonNull(
                resolutionAction,
                "resolutionAction es obligatorio");

        if (administrator.getRole() != UserRole.ADMINISTRADOR) {

            throw new IllegalArgumentException(
                    "La incidencia solo puede ser resuelta "
                            + "por un administrador");
        }

        if (!administrator.isEnabled()) {
            throw new IllegalArgumentException(
                    "El administrador se encuentra deshabilitado");
        }

        if (!isOpen()) {
            throw new IllegalStateException(
                    "La incidencia ya está resuelta");
        }

        if (!loan.isActive()) {
            throw new IllegalStateException(
                    "El préstamo relacionado ya no está activo");
        }

        if (resolutionDate.isBefore(reportedAt)) {
            throw new IllegalArgumentException(
                    "La resolución no puede ser anterior al reporte");
        }

        String normalizedNote = requireText(
                resolutionNote,
                "resolutionNote");

        if (normalizedNote.length() > 500) {
            throw new IllegalArgumentException(
                    "La observación de resolución no puede superar "
                            + "500 caracteres");
        }

        this.resolvedByUser = administrator;
        this.resolvedAt = resolutionDate;
        this.resolutionAction = resolutionAction;
        this.resolutionNote = normalizedNote;
        this.openSlot = null;
    }

    public boolean isResolved() {
        return resolvedAt != null
                && resolvedByUser != null
                && resolutionAction != null
                && openSlot == null;
    }

    public boolean isOpen() {
        return resolvedAt == null
                && resolvedByUser == null
                && openSlot != null
                && openSlot == OPEN_SLOT_VALUE;
    }

    private static String requireText(
            String value,
            String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " es obligatorio");
        }

        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public Loan getLoan() {
        return loan;
    }

    public User getReportedByUser() {
        return reportedByUser;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public User getResolvedByUser() {
        return resolvedByUser;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public int getNotificationAttempts() {
        return notificationAttempts;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public String getLastNotificationError() {
        return lastNotificationError;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 30)
    private IncidentResolutionAction resolutionAction;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    /**
     * 1 indica que la incidencia sigue abierta.
     * NULL indica que ya fue resuelta.
     */
    @Column(name = "open_slot")
    private Byte openSlot;

    public IncidentResolutionAction getResolutionAction() {
        return resolutionAction;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Byte getOpenSlot() {
        return openSlot;
    }
}