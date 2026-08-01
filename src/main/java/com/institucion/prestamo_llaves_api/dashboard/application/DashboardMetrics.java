package com.institucion.prestamo_llaves_api.dashboard.application;

import com.institucion.prestamo_llaves_api.user.domain.model.UserRole;

/**
 * Resultado interno con las métricas del dashboard.
 *
 * Las métricas administrativas y personales se mantienen
 * separadas mediante métodos de fábrica. Los campos que no
 * aplican al rol permanecen en null y no deben calcularse.
 */
public record DashboardMetrics(

        UserRole role,

        long availableKeys,

        Long loanedKeys,

        Long activeLoans,

        Long openIncidents,

        Long activeUsers,

        Long activeRooms,

        Long myActiveLoans,

        Long myOpenIncidents) {

    /**
     * Valida que ninguna métrica sea negativa y que la
     * estructura corresponda al rol indicado.
     */
    public DashboardMetrics {
        if (role == null) {
            throw new IllegalArgumentException(
                    "El rol del dashboard es obligatorio");
        }

        validateNonNegative(
                availableKeys,
                "availableKeys");

        validateNullableNonNegative(
                loanedKeys,
                "loanedKeys");

        validateNullableNonNegative(
                activeLoans,
                "activeLoans");

        validateNullableNonNegative(
                openIncidents,
                "openIncidents");

        validateNullableNonNegative(
                activeUsers,
                "activeUsers");

        validateNullableNonNegative(
                activeRooms,
                "activeRooms");

        validateNullableNonNegative(
                myActiveLoans,
                "myActiveLoans");

        validateNullableNonNegative(
                myOpenIncidents,
                "myOpenIncidents");

        validateMetricsForRole(
                role,
                loanedKeys,
                activeLoans,
                openIncidents,
                activeUsers,
                activeRooms,
                myActiveLoans,
                myOpenIncidents);
    }

    /**
     * Construye las métricas visibles para un administrador.
     */
    public static DashboardMetrics administrator(
            long availableKeys,
            long loanedKeys,
            long activeLoans,
            long openIncidents,
            long activeUsers,
            long activeRooms) {
        return new DashboardMetrics(
                UserRole.ADMINISTRADOR,
                availableKeys,
                loanedKeys,
                activeLoans,
                openIncidents,
                activeUsers,
                activeRooms,
                null,
                null);
    }

    /**
     * Construye las métricas personales visibles para
     * un usuario estándar.
     */
    public static DashboardMetrics user(
            long availableKeys,
            long myActiveLoans,
            long myOpenIncidents) {
        return new DashboardMetrics(
                UserRole.USUARIO,
                availableKeys,
                null,
                null,
                null,
                null,
                null,
                myActiveLoans,
                myOpenIncidents);
    }

    private static void validateMetricsForRole(
            UserRole role,
            Long loanedKeys,
            Long activeLoans,
            Long openIncidents,
            Long activeUsers,
            Long activeRooms,
            Long myActiveLoans,
            Long myOpenIncidents) {
        if (role == UserRole.ADMINISTRADOR) {
            requireMetric(
                    loanedKeys,
                    "loanedKeys");

            requireMetric(
                    activeLoans,
                    "activeLoans");

            requireMetric(
                    openIncidents,
                    "openIncidents");

            requireMetric(
                    activeUsers,
                    "activeUsers");

            requireMetric(
                    activeRooms,
                    "activeRooms");

            requireAbsentMetric(
                    myActiveLoans,
                    "myActiveLoans");

            requireAbsentMetric(
                    myOpenIncidents,
                    "myOpenIncidents");

            return;
        }

        requireMetric(
                myActiveLoans,
                "myActiveLoans");

        requireMetric(
                myOpenIncidents,
                "myOpenIncidents");

        requireAbsentMetric(
                loanedKeys,
                "loanedKeys");

        requireAbsentMetric(
                activeLoans,
                "activeLoans");

        requireAbsentMetric(
                openIncidents,
                "openIncidents");

        requireAbsentMetric(
                activeUsers,
                "activeUsers");

        requireAbsentMetric(
                activeRooms,
                "activeRooms");
    }

    private static void validateNonNegative(
            long value,
            String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " no puede ser negativo");
        }
    }

    private static void validateNullableNonNegative(
            Long value,
            String fieldName) {
        if (value != null) {
            validateNonNegative(
                    value,
                    fieldName);
        }
    }

    private static void requireMetric(
            Long value,
            String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName
                            + " es obligatorio para este rol");
        }
    }

    private static void requireAbsentMetric(
            Long value,
            String fieldName) {
        if (value != null) {
            throw new IllegalArgumentException(
                    fieldName
                            + " no aplica para este rol");
        }
    }
}