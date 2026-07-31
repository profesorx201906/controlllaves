-- =========================================================
-- Versión de seguridad de los usuarios
-- =========================================================

ALTER TABLE app_users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0
        AFTER must_change_password,

    ADD CONSTRAINT ck_app_users_token_version
        CHECK (token_version >= 0);