-- ============================================================
--  Soft-delete a nivel de archivo (RESULTADO_ARCHIVO)
--  ------------------------------------------------------------
--  Justificacion: el operador de resultados debe poder corregir
--  un envio equivocado (PDF/img/video de otro paciente) sin
--  perder el resto de la entrega. Hasta ahora la unica opcion
--  era anular la cabecera completa (soft delete de RESULTADO_CARGA).
--
--  Estrategia: replicar el patron de soft-delete que ya existe
--  en RESULTADO_CARGA (ESTADO + FECHA_ELIMINACION + USUARIO +
--  MOTIVO). Los archivos fisicos en S3 permanecen: se sirven
--  siempre que ESTADO=1, y se bloquean (404) cuando ESTADO=0.
--
--  Compatible con la regla de negocio "al menos 1 archivo
--  activo por cabecera": eso se valida en service, no en BD.
-- ============================================================

ALTER TABLE RESULTADO_ARCHIVO
    ADD COLUMN ESTADO                TINYINT      NOT NULL DEFAULT 1
        COMMENT '1=activo (visible al paciente), 0=inactivo (soft-delete)',
    ADD COLUMN FECHA_ELIMINACION     DATETIME     NULL
        COMMENT 'Cuando se desactivo el archivo',
    ADD COLUMN USUARIO_ELIMINACION   VARCHAR(100) NULL
        COMMENT 'Quien lo desactivo',
    ADD COLUMN MOTIVO_ELIMINACION    VARCHAR(500) NULL
        COMMENT 'Motivo documentado de la desactivacion';

-- Indice compuesto para los listados del visor/portal que filtran
-- por cabecera + solo activos (evita tocar archivos desactivados).
CREATE INDEX IDX_RESULTADO_ARCHIVO_CARGA_ESTADO
    ON RESULTADO_ARCHIVO (ID_RESULTADO_CARGA, ESTADO);