-- ============================================================
--  Redisenio del modelo de resultados: cabecera/detalle
--  ------------------------------------------------------------
--  Justificacion: un paciente puede tener multiples estudios a
--  lo largo del tiempo. Cada carga es una transaccion (cabecera)
--  con N archivos (detalle). Se migra el esquema anterior
--  RESULTADO + ARCHIVO_RESULTADO -> RESULTADO_CARGA + RESULTADO_ARCHIVO.
--
--  Cambios clave:
--   * ID_GENERADO (UUID-like, VARCHAR 64) como business key publica
--     en la cabecera, aparte del PK AUTO_INCREMENT (rendimiento).
--   * RESULTADO_ARCHIVO.ID_GENERADO denormalizado para construir
--     el object key de S3 sin JOIN.
--   * RESULTADO_ARCHIVO.ID_ARCHIVO (UK) sufijo unico del object key.
--   * Soft delete reversible: ESTADO + FECHA_ELIMINACION + USUARIO +
--     MOTIVO. Los archivos en S3 permanecen hasta purga manual.
--   * Campos denormalizados en cabecera: CANTIDAD_ARCHIVOS,
--     PESO_TOTAL_MB (evitan GROUP BY en el panel del paciente).
--  Corte limpio confirmado por usuario (no se migran datos).
-- ============================================================

-- DROP idempotente de FKs hacia RESULTADO / ARCHIVO_RESULTADO.
-- Se usa un procedimiento para tolerar re-ejecuciones luego de una
-- migracion parcialmente aplicada (Flyway deja el registro como failed).
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DELIMITER $$
CREATE PROCEDURE drop_fk_if_exists(IN p_table VARCHAR(64), IN p_fk VARCHAR(64))
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND CONSTRAINT_NAME = p_fk
      AND CONSTRAINT_TYPE = 'FOREIGN KEY';
    IF v_count > 0 THEN
        SET @sql := CONCAT('ALTER TABLE ', p_table, ' DROP FOREIGN KEY ', p_fk);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL drop_fk_if_exists('ENLACE_CORTO',      'FK_ENLACE_RESULTADO');
CALL drop_fk_if_exists('EVENTO_VISTA',      'FK_EVENTO_RESULTADO');
CALL drop_fk_if_exists('LOG_CARGA',         'FK_LOG_RESULTADO');
CALL drop_fk_if_exists('ARCHIVO_RESULTADO', 'FK_ARCHIVO_RESULTADO');
CALL drop_fk_if_exists('RESULTADO',         'FK_RESULTADO_PACIENTE');
CALL drop_fk_if_exists('ENLACE_CORTO',      'FK_ENLACE_RESULTADO_CARGA');
CALL drop_fk_if_exists('EVENTO_VISTA',      'FK_EVENTO_RESULTADO_CARGA');
CALL drop_fk_if_exists('LOG_CARGA',         'FK_LOG_RESULTADO_CARGA');

DROP PROCEDURE drop_fk_if_exists;

-- DROP idempotente de tablas (toleran haber sido aplicadas parcialmente).
DROP TABLE IF EXISTS ARCHIVO_RESULTADO;
DROP TABLE IF EXISTS RESULTADO_CARGA;
DROP TABLE IF EXISTS RESULTADO_ARCHIVO;
DROP TABLE IF EXISTS RESULTADO;

-- Corte limpio: las tablas hijas referencian al viejo RESULTADO. Sin el padre
-- las filas huerfanas harian fallar la creacion de los nuevos FKs hacia
-- RESULTADO_CARGA. Se truncan explicitamente (corte limpio confirmado).
TRUNCATE TABLE ENLACE_CORTO;
TRUNCATE TABLE EVENTO_VISTA;
TRUNCATE TABLE LOG_CARGA;

-- ============================================================
--  RESULTADO_CARGA (cabecera / transaccion)
-- ============================================================
CREATE TABLE RESULTADO_CARGA (
    ID_RESULTADO_CARGA    BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'PK tecnica (performance de JOINs)',
    ID_GENERADO           VARCHAR(64)   NOT NULL                COMMENT 'Business key UUID-like: iasd52a445asd52f4asd2f-asdf5452',
    TOKEN_PUBLICO         VARCHAR(64)   NOT NULL                COMMENT 'URL publica del visor /r/{token}',
    ID_PACIENTE           BIGINT        NULL                    COMMENT 'FK opcional a PACIENTE',
    DOCUMENTO_IDENTIDAD   VARCHAR(20)   NOT NULL                COMMENT 'DNI normalizado (sin prefijo pais)',
    TELEFONO              VARCHAR(20)   NULL                    COMMENT 'Telefono destino normalizado con prefijo 51 (opcional)',
    NOMBRE_PACIENTE       VARCHAR(150)  NULL                    COMMENT 'Nombre del paciente (RENIEC o cargado)',
    MENSAJE               VARCHAR(500)  NULL                    COMMENT 'Mensaje del agente al paciente',
    ESTADO                TINYINT       NOT NULL DEFAULT 1       COMMENT '1=activo, 0=inactivo (soft delete)',
    FECHA_ELIMINACION     DATETIME      NULL                    COMMENT 'Cuando se hizo soft delete (si ESTADO=0)',
    USUARIO_ELIMINACION   VARCHAR(100)  NULL                    COMMENT 'Quien hizo el soft delete',
    MOTIVO_ELIMINACION    VARCHAR(500)  NULL                    COMMENT 'Motivo documentado del soft delete',
    ESTADO_SMS            VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE|EN_COLA|ENVIADO|FALLIDO|DESHABILITADO',
    CONTADOR_VISTAS       INT           NOT NULL DEFAULT 0,
    FECHA_PRIMERA_VISTA   DATETIME      NULL,
    FECHA_ULTIMA_VISTA    DATETIME      NULL,
    CANTIDAD_ARCHIVOS     INT           NOT NULL DEFAULT 0       COMMENT 'Denormalizado: COUNT(archivos) para listado rapido',
    PESO_TOTAL_MB         DECIMAL(14,4) NOT NULL DEFAULT 0       COMMENT 'Denormalizado: SUM(peso_mb) para listado rapido',
    FECHA_HORA_CARGA      DATETIME      NOT NULL                 COMMENT 'Business event time: cuando se realizo la carga',
    -- Columnas de auditoria (EntidadAuditable @MappedSuperclass).
    FECHA_CREACION        DATETIME      NOT NULL                 COMMENT 'Audit: cuando se persistio la fila',
    USUARIO_CREACION      VARCHAR(100)  NULL                     COMMENT 'Audit: agente que creo',
    FECHA_MODIFICACION    DATETIME      NULL                     COMMENT 'Audit: ultima modificacion',
    USUARIO_MODIFICACION  VARCHAR(100)  NULL                     COMMENT 'Audit: quien modifico',
    PRIMARY KEY (ID_RESULTADO_CARGA),
    UNIQUE KEY UK_RESULTADO_CARGA_GENERADO (ID_GENERADO),
    UNIQUE KEY UK_RESULTADO_CARGA_TOKEN (TOKEN_PUBLICO),
    KEY IDX_RESULTADO_CARGA_DNI (DOCUMENTO_IDENTIDAD),
    KEY IDX_RESULTADO_CARGA_DNI_ESTADO (DOCUMENTO_IDENTIDAD, ESTADO),
    KEY IDX_RESULTADO_CARGA_DNI_FECHA (DOCUMENTO_IDENTIDAD, FECHA_HORA_CARGA),
    KEY IDX_RESULTADO_CARGA_PACIENTE (ID_PACIENTE),
    KEY IDX_RESULTADO_CARGA_FECHA (FECHA_HORA_CARGA),
    KEY IDX_RESULTADO_CARGA_ESTADO (ESTADO),
    CONSTRAINT FK_RESULTADO_CARGA_PACIENTE FOREIGN KEY (ID_PACIENTE)
        REFERENCES PACIENTE (ID_PACIENTE) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  RESULTADO_ARCHIVO (detalle / archivos de la transaccion)
-- ============================================================
CREATE TABLE RESULTADO_ARCHIVO (
    ID_RESULTADO_ARCHIVO  BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'PK tecnica',
    ID_RESULTADO_CARGA     BIGINT        NOT NULL                COMMENT 'FK a la cabecera',
    ID_GENERADO            VARCHAR(64)   NOT NULL                COMMENT 'Denormalizado del padre (evita JOIN al construir object key S3)',
    ID_ARCHIVO             VARCHAR(64)   NOT NULL                COMMENT 'Sufijo unico del object key S3 (parte final)',
    NOMBRE_ORIGINAL        VARCHAR(255)  NOT NULL                COMMENT 'Nombre tal como llego del upload',
    TIPO_CONTENIDO         VARCHAR(120)  NOT NULL                COMMENT 'MIME type (application/pdf, image/png, ...)',
    TIPO_MEDIO             VARCHAR(20)   NOT NULL                COMMENT 'PDF|IMAGEN|VIDEO|OTRO',
    EXTENSION              VARCHAR(10)   NULL                    COMMENT 'Extension sin punto (pdf, png, mp4, ...)',
    PESO_MB                DECIMAL(12,4) NOT NULL DEFAULT 0      COMMENT 'Tamano del archivo en MB',
    FECHA_CREACION         DATETIME      NOT NULL                 COMMENT 'Cuando se subio este archivo',
    PRIMARY KEY (ID_RESULTADO_ARCHIVO),
    UNIQUE KEY UK_RESULTADO_ARCHIVO_ID (ID_ARCHIVO),
    KEY IDX_RESULTADO_ARCHIVO_CARGA (ID_RESULTADO_CARGA),
    KEY IDX_RESULTADO_ARCHIVO_GENERADO (ID_GENERADO),
    CONSTRAINT FK_RESULTADO_ARCHIVO_CARGA FOREIGN KEY (ID_RESULTADO_CARGA)
        REFERENCES RESULTADO_CARGA (ID_RESULTADO_CARGA) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  Re-crear FKs en tablas relacionadas apuntando a la nueva cabecera
--  (se conserva el nombre de columna ID_RESULTADO en estas tablas
--  para minimizar cambios en el codigo Java existente)
-- ============================================================
ALTER TABLE ENLACE_CORTO
    ADD CONSTRAINT FK_ENLACE_RESULTADO_CARGA
    FOREIGN KEY (ID_RESULTADO) REFERENCES RESULTADO_CARGA (ID_RESULTADO_CARGA) ON DELETE SET NULL;

ALTER TABLE EVENTO_VISTA
    ADD CONSTRAINT FK_EVENTO_RESULTADO_CARGA
    FOREIGN KEY (ID_RESULTADO) REFERENCES RESULTADO_CARGA (ID_RESULTADO_CARGA) ON DELETE CASCADE;

ALTER TABLE LOG_CARGA
    ADD CONSTRAINT FK_LOG_RESULTADO_CARGA
    FOREIGN KEY (ID_RESULTADO) REFERENCES RESULTADO_CARGA (ID_RESULTADO_CARGA) ON DELETE SET NULL;
