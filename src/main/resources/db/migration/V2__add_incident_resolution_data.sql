-- =========================================================
-- Datos administrativos de resolución
-- =========================================================

ALTER TABLE incidents
    ADD COLUMN resolution_action VARCHAR(30) NULL
        AFTER resolved_by_user_id,

    ADD COLUMN resolution_note VARCHAR(500) NULL
        AFTER resolution_action,

    /*
     * 1    = incidencia abierta
     * NULL = incidencia resuelta
     */
    ADD COLUMN open_slot TINYINT UNSIGNED NULL
        AFTER resolution_note;


-- Compatibilidad con incidencias resueltas antes de V2.
UPDATE incidents
SET
    resolution_action = 'LLAVE_RECUPERADA',
    resolution_note =
        'Incidencia resuelta antes de la migración V2'
WHERE resolved_at IS NOT NULL;


-- Inicializar el indicador de incidencia abierta.
UPDATE incidents
SET open_slot = CASE
    WHEN resolved_at IS NULL THEN 1
    ELSE NULL
END;


ALTER TABLE incidents
    MODIFY open_slot TINYINT UNSIGNED NULL DEFAULT 1,

    /*
     * Solo puede existir una incidencia abierta
     * por préstamo.
     */
    ADD CONSTRAINT uk_incidents_open_loan
        UNIQUE (loan_id, open_slot),

    ADD CONSTRAINT ck_incidents_resolution_data
        CHECK (
            (
                resolved_at IS NULL
                AND resolved_by_user_id IS NULL
                AND resolution_action IS NULL
                AND resolution_note IS NULL
                AND open_slot = 1
            )
            OR
            (
                resolved_at IS NOT NULL
                AND resolved_by_user_id IS NOT NULL
                AND resolution_action IN (
                    'LLAVE_RECUPERADA',
                    'LLAVE_REEMPLAZADA'
                )
                AND resolution_note IS NOT NULL
                AND open_slot IS NULL
            )
        );