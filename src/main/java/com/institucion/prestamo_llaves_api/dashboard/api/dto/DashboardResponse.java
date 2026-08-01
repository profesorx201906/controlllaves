package com.institucion.prestamo_llaves_api.dashboard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.institucion.prestamo_llaves_api.dashboard.application.DashboardMetrics;
import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Respuesta pública del dashboard.
 *
 * Los campos con valor null se excluyen del JSON para que
 * cada rol reciba únicamente las métricas que le corresponden.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(

        UserRole role,

        long availableKeys,

        Long loanedKeys,

        Long activeLoans,

        Long openIncidents,

        Long activeUsers,

        Long activeRooms,

        Long myActiveLoans,

        Long myOpenIncidents

) {

    /**
     * Convierte el resultado interno del servicio
     * en la respuesta HTTP.
     */
    public static DashboardResponse from(
            DashboardMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException(
                    "Las métricas del dashboard son obligatorias");
        }

        return new DashboardResponse(
                metrics.role(),
                metrics.availableKeys(),
                metrics.loanedKeys(),
                metrics.activeLoans(),
                metrics.openIncidents(),
                metrics.activeUsers(),
                metrics.activeRooms(),
                metrics.myActiveLoans(),
                metrics.myOpenIncidents());
    }
}