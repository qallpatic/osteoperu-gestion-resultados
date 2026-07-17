-- ============================================================
--  Ajustes al rate-limit de recuperacion de password y login
--  + Vigencia de los resultados para el paciente
--  ------------------------------------------------------------
--  Cambios (orden de aplicacion):
--   1) INTENTO_RECUPERACION_PASSWORD: la columna TELEFONO_NORM pasa a
--      DOCUMENTO_NORM (VARCHAR 15) porque la recuperacion ahora se pide
--      por DNI/CE, no por telefono. Se renombra el indice tambien.
--   2) INTENTO_LOGIN: tabla nueva para el rate-limit de /api/auth/login
--      (5 intentos por IP y 5 por usuario en ventana 10 min, configurables).
--   3) RESULTADO_CARGA: nueva columna FECHA_VIGENCIA_HASTA que marca el
--      fin de la vigencia para el paciente (FECHA_HORA_CARGA + 180 dias).
--      Pasado ese plazo el visor publico (/r/{token}) y el portal
--      (/api/paciente/...) bloquean la descarga/visualizacion.
--      El ADMIN sigue viendo todo (registro/auditoria).
--
--  SQL idempotente (cada operacion verifica INFORMATION_SCHEMA) para que
--  sea seguro re-ejecutar tras un rollback parcial o un cambio de rama
--  con BD ya migrada.
-- ============================================================

-- ---------- INTENTO_RECUPERACION_PASSWORD ----------

-- 1) DROP INDEX si existe (MySQL 8 NO soporta DROP INDEX IF EXISTS; usamos INFORMATION_SCHEMA)
SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'INTENTO_RECUPERACION_PASSWORD'
      AND INDEX_NAME   = 'idx_recup_tel_fecha'
);
SET @sql := IF(@idx_exists > 0,
    'ALTER TABLE INTENTO_RECUPERACION_PASSWORD DROP INDEX idx_recup_tel_fecha',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Renombrar TELEFONO_NORM -> DOCUMENTO_NORM (solo si aun existe TELEFONO_NORM)
SET @col_needs_rename := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'INTENTO_RECUPERACION_PASSWORD'
      AND COLUMN_NAME  = 'TELEFONO_NORM'
);
SET @sql := IF(@col_needs_rename > 0,
    'ALTER TABLE INTENTO_RECUPERACION_PASSWORD CHANGE COLUMN TELEFONO_NORM DOCUMENTO_NORM VARCHAR(15) NOT NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Crear idx_recup_doc_fecha si no existe
SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'INTENTO_RECUPERACION_PASSWORD'
      AND INDEX_NAME   = 'idx_recup_doc_fecha'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE INTENTO_RECUPERACION_PASSWORD ADD KEY idx_recup_doc_fecha (DOCUMENTO_NORM, FECHA_REGISTRO)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- INTENTO_LOGIN (CREATE TABLE soporta IF NOT EXISTS) ----------

CREATE TABLE IF NOT EXISTS INTENTO_LOGIN (
    ID              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK tecnica',
    IP_ADDRESS      VARCHAR(45)  NOT NULL                COMMENT 'IPv4 o IPv6 del cliente (X-Forwarded-For si hay proxy)',
    USUARIO         VARCHAR(100) NOT NULL                COMMENT 'Login intentado (puede no existir en BD)',
    BLOQUEADO       TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '1 = la evaluacion determino que superaba el limite',
    FECHA_REGISTRO  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Microseg para distinguir colisiones',
    PRIMARY KEY (ID),
    KEY idx_login_ip_fecha  (IP_ADDRESS, FECHA_REGISTRO),
    KEY idx_login_usu_fecha (USUARIO, FECHA_REGISTRO),
    KEY idx_login_fecha     (FECHA_REGISTRO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- RESULTADO_CARGA: FECHA_VIGENCIA_HASTA ----------

-- 4) Agregar columna si no existe
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'RESULTADO_CARGA'
      AND COLUMN_NAME  = 'FECHA_VIGENCIA_HASTA'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE RESULTADO_CARGA ADD COLUMN FECHA_VIGENCIA_HASTA DATETIME NULL COMMENT ''Fecha hasta la cual el resultado esta vigente para el paciente (FECHA_HORA_CARGA + 180 dias). Pasado ese plazo se bloquea el acceso al paciente; el ADMIN sigue viendo todo.''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) Backfill para filas existentes con FECHA_VIGENCIA_HASTA NULL
SET @needs_backfill := (SELECT COUNT(*) FROM RESULTADO_CARGA WHERE FECHA_VIGENCIA_HASTA IS NULL);
SET @sql := IF(@needs_backfill > 0,
    'UPDATE RESULTADO_CARGA SET FECHA_VIGENCIA_HASTA = DATE_ADD(FECHA_HORA_CARGA, INTERVAL 180 DAY) WHERE FECHA_VIGENCIA_HASTA IS NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) Asegurar NOT NULL (solo si actualmente es NULL-able)
SET @is_nullable := (
    SELECT UPPER(IS_NULLABLE) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'RESULTADO_CARGA'
      AND COLUMN_NAME  = 'FECHA_VIGENCIA_HASTA'
);
SET @sql := IF(@is_nullable = 'YES',
    'ALTER TABLE RESULTADO_CARGA MODIFY COLUMN FECHA_VIGENCIA_HASTA DATETIME NOT NULL COMMENT ''Fecha hasta la cual el resultado esta vigente para el paciente (FECHA_HORA_CARGA + 180 dias). Pasado ese plazo se bloquea el acceso al paciente; el ADMIN sigue viendo todo.''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) Crear indice si no existe
SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'RESULTADO_CARGA'
      AND INDEX_NAME   = 'IDX_RESULTADO_CARGA_VIGENCIA'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX IDX_RESULTADO_CARGA_VIGENCIA ON RESULTADO_CARGA (FECHA_VIGENCIA_HASTA)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
