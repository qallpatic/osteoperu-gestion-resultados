-- ============================================================
--  Rate limit para recuperacion de password
--  ------------------------------------------------------------
--  Registra cada intento (exitoso o bloqueado) para que el backend
--  pueda limitar abuso desde una misma IP o un mismo telefono en una
--  ventana deslizante (default 10 min, configurable).
--
--  Reglas evaluadas en TasaLimitRecuperacionService:
--    * 3 intentos por IP en la ventana.
--    * 3 intentos por telefono normalizado en la ventana.
--    * 3 telefonos distintos solicitados desde una misma IP en la ventana.
--
--  Housekeeping via servicio @Scheduled (config recuperacion.retention-horas).
-- ============================================================

CREATE TABLE INTENTO_RECUPERACION_PASSWORD (
    ID              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK tecnica',
    IP_ADDRESS      VARCHAR(45)  NOT NULL                COMMENT 'IPv4 o IPv6 del cliente (X-Forwarded-For si hay proxy)',
    TELEFONO_NORM   VARCHAR(20)  NOT NULL                COMMENT 'Telefono normalizado con countryCode (ej. 51987654321)',
    BLOQUEADO       TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '1 = la evaluacion determino que superaba el limite',
    FECHA_REGISTRO  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Microseg para evitar colisiones en simultaneos',
    PRIMARY KEY (ID),
    KEY idx_recup_ip_fecha   (IP_ADDRESS, FECHA_REGISTRO),
    KEY idx_recup_tel_fecha  (TELEFONO_NORM, FECHA_REGISTRO),
    KEY idx_recup_fecha      (FECHA_REGISTRO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
