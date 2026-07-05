-- ============================================================
--  Esquema inicial - Envio de resultados de pacientes
--  BD: osteoperu_resultados  (utf8mb4 / utf8mb4_unicode_ci)
-- ============================================================

-- Lote de resultados de un paciente (una entrega = una carpeta {telefono}_{dni})
CREATE TABLE patient_result_batch (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    phone            VARCHAR(20)  NOT NULL COMMENT 'Numero destino normalizado, ej: 51987654321',
    dni              VARCHAR(20)  NOT NULL,
    folder_name      VARCHAR(80)  NOT NULL COMMENT 'Carpeta fisica: {telefono}_{dni}',
    public_token     VARCHAR(64)  NOT NULL COMMENT 'Token secreto e irrepetible del visor /r/{token}',
    sms_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | QUEUED | SENT | FAILED | DISABLED',
    view_count       INT          NOT NULL DEFAULT 0 COMMENT 'Veces que el paciente abrio la pagina de resultados',
    first_viewed_at  DATETIME     NULL COMMENT 'Marca la primera vez que el paciente vio sus resultados',
    last_viewed_at   DATETIME     NULL,
    created_by       VARCHAR(100) NULL COMMENT 'Usuario del panel que subio los resultados',
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_public_token (public_token),
    KEY idx_batch_phone_dni (phone, dni),
    KEY idx_batch_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Archivos (pdf / imagen / video) asociados a un lote
CREATE TABLE result_file (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id           BIGINT       NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    stored_filename    VARCHAR(255) NOT NULL COMMENT 'Nombre saneado con el que se guardo en disco',
    content_type       VARCHAR(120) NULL,
    media_type         VARCHAR(20)  NOT NULL COMMENT 'PDF | IMAGE | VIDEO | OTHER',
    size_bytes         BIGINT       NOT NULL DEFAULT 0,
    created_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_file_batch (batch_id),
    CONSTRAINT fk_file_batch FOREIGN KEY (batch_id)
        REFERENCES patient_result_batch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Acortador de links: el codigo corto que viaja en el SMS
CREATE TABLE short_link (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    short_code   VARCHAR(16)  NOT NULL,
    original_url TEXT         NOT NULL COMMENT 'Destino real, normalmente el visor /r/{token}',
    type         VARCHAR(20)  NULL COMMENT 'RESULTADO, etc.',
    batch_id     BIGINT       NULL,
    click_count  INT          NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL,
    expires_at   DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_short_code (short_code),
    KEY idx_short_batch (batch_id),
    CONSTRAINT fk_short_batch FOREIGN KEY (batch_id)
        REFERENCES patient_result_batch (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Traza detallada de cada click al link corto y cada vista de la pagina
CREATE TABLE view_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id    BIGINT       NOT NULL,
    short_code  VARCHAR(16)  NULL,
    event_type  VARCHAR(20)  NOT NULL COMMENT 'CLICK (abrio el link corto) | VIEW (cargo la pagina) | DOWNLOAD',
    ip_address  VARCHAR(64)  NULL,
    user_agent  VARCHAR(512) NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_view_batch (batch_id),
    KEY idx_view_type (event_type),
    CONSTRAINT fk_view_batch FOREIGN KEY (batch_id)
        REFERENCES patient_result_batch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
