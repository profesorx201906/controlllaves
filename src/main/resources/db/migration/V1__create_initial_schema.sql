-- =========================================================
-- Usuarios de la aplicación
-- =========================================================

CREATE TABLE app_users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    -- Roles funcionales definidos para la aplicación.
    role VARCHAR(20) NOT NULL,

    -- Permite desactivar una cuenta sin eliminar su historial.
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- El administrador inicial deberá cambiar su contraseña.
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_app_users_email
        UNIQUE (email),

    CONSTRAINT ck_app_users_role
        CHECK (role IN ('ADMINISTRADOR', 'USUARIO'))
) ENGINE = InnoDB;

-- =========================================================
-- Ambientes de formación
-- =========================================================

CREATE TABLE rooms (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    name VARCHAR(120) NOT NULL,
    description VARCHAR(255) NULL,

    -- Permite retirar temporalmente un ambiente del servicio.
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_rooms_name
        UNIQUE (name)
) ENGINE = InnoDB;

-- =========================================================
-- Llaves de los ambientes
-- =========================================================

CREATE TABLE room_keys (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    room_id BIGINT UNSIGNED NOT NULL,

    -- Estados funcionales aprobados para una llave.
    status VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',

    -- Se mapeará posteriormente con @Version de JPA.
    version BIGINT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- Inicialmente cada ambiente tendrá una sola llave.
    CONSTRAINT uk_room_keys_room
        UNIQUE (room_id),

    CONSTRAINT ck_room_keys_status
        CHECK (status IN ('DISPONIBLE', 'PRESTADA')),

    CONSTRAINT fk_room_keys_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX idx_room_keys_status
    ON room_keys (status);

-- =========================================================
-- Préstamos y devoluciones
-- =========================================================

CREATE TABLE loans (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    room_key_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,

    borrowed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    returned_at DATETIME(6) NULL,

    /*
     * 1    = préstamo activo
     * NULL = préstamo finalizado
     *
     * La restricción única impide que una misma llave tenga
     * dos préstamos activos simultáneamente.
     */
    active_slot TINYINT UNSIGNED NULL DEFAULT 1,

    PRIMARY KEY (id),

    CONSTRAINT uk_loans_active_key
        UNIQUE (room_key_id, active_slot),

    CONSTRAINT ck_loans_active_slot
        CHECK (
            (returned_at IS NULL AND active_slot = 1)
            OR
            (returned_at IS NOT NULL AND active_slot IS NULL)
        ),

    CONSTRAINT fk_loans_room_key
        FOREIGN KEY (room_key_id)
        REFERENCES room_keys (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_loans_user
        FOREIGN KEY (user_id)
        REFERENCES app_users (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX idx_loans_user_borrowed_at
    ON loans (user_id, borrowed_at);

CREATE INDEX idx_loans_key_borrowed_at
    ON loans (room_key_id, borrowed_at);

CREATE INDEX idx_loans_returned_at
    ON loans (returned_at);

-- =========================================================
-- Pérdidas e incidencias
-- =========================================================

CREATE TABLE incidents (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    loan_id BIGINT UNSIGNED NOT NULL,
    reported_by_user_id BIGINT UNSIGNED NOT NULL,

    incident_type VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,

    reported_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- Una incidencia abierta tendrá estos campos en NULL.
    resolved_at DATETIME(6) NULL,
    resolved_by_user_id BIGINT UNSIGNED NULL,

    /*
     * Permite controlar el envío y los reintentos del correo.
     * El préstamo no dependerá de que SMTP responda inmediatamente.
     */
    notification_status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    notification_attempts INT UNSIGNED NOT NULL DEFAULT 0,
    notified_at DATETIME(6) NULL,
    last_notification_error VARCHAR(500) NULL,

    PRIMARY KEY (id),

    CONSTRAINT ck_incidents_type
        CHECK (incident_type IN ('PERDIDA', 'INCIDENCIA')),

    CONSTRAINT ck_incidents_notification_status
        CHECK (
            notification_status IN (
                'PENDIENTE',
                'ENVIADA',
                'FALLIDA'
            )
        ),

    CONSTRAINT ck_incidents_resolution
        CHECK (
            (resolved_at IS NULL AND resolved_by_user_id IS NULL)
            OR
            (
                resolved_at IS NOT NULL
                AND resolved_by_user_id IS NOT NULL
            )
        ),

    CONSTRAINT fk_incidents_loan
        FOREIGN KEY (loan_id)
        REFERENCES loans (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_incidents_reported_by
        FOREIGN KEY (reported_by_user_id)
        REFERENCES app_users (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_incidents_resolved_by
        FOREIGN KEY (resolved_by_user_id)
        REFERENCES app_users (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX idx_incidents_loan_reported_at
    ON incidents (loan_id, reported_at);

CREATE INDEX idx_incidents_notification_status
    ON incidents (notification_status);

CREATE INDEX idx_incidents_resolved_at
    ON incidents (resolved_at);


