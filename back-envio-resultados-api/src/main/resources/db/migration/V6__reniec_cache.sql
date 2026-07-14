-- ============================================================
--  RENIEC_CACHE: cache local de respuestas de la API de RENIEC.
--  Cada DNI se cobra 1 vez al proveedor; las consultas siguientes
--  dentro del TTL (reniec.cache-dias, por defecto 180 dias) salen
--  de esta tabla. Si la API falla y la fila esta expirada, se
--  devuelve igualmente como fallback de resiliencia.
-- ============================================================

CREATE TABLE RENIEC_CACHE (
    DNI                 VARCHAR(8)   NOT NULL COMMENT 'PK; DNI peruano de 8 digitos',
    NOMBRES             VARCHAR(120) NULL,
    APELLIDO_PATERNO    VARCHAR(120) NULL,
    APELLIDO_MATERNO    VARCHAR(120) NULL,
    NOMBRE_COMPLETO     VARCHAR(250) NOT NULL COMMENT 'Formato canonico: "APELLIDO_P APELLIDO_M, NOMBRES"',
    CODIGO_VERIFICACION INT          NULL,
    CONSULTAS           INT          NOT NULL DEFAULT 1 COMMENT 'Cantidad de hits en cache (auditoria)',
    FECHA_CONSULTA      DATETIME     NOT NULL COMMENT 'Fecha/hora de la ultima consulta al proveedor',
    FECHA_EXPIRA        DATETIME     NOT NULL COMMENT 'FECHA_CONSULTA + TTL (cache-dias)',
    PRIMARY KEY (DNI),
    KEY IDX_RENIEC_CACHE_EXP (FECHA_EXPIRA)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;