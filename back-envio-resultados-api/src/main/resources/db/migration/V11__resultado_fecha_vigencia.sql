-- ============================================================
--  Vigencia de los resultados para el paciente
--  ------------------------------------------------------------
--  Politica: cada resultado esta disponible para el paciente
--  durante 180 dias (6 meses) desde la fecha de carga. Pasado
--  ese plazo el visor publico (/r/{token}) y el portal
--  (/api/paciente/...) bloquean la descarga/visualizacion.
--  El ADMIN sigue viendo todo (registro/auditoria).
--
--  Estrategia: columna persistente calculada al momento de la
--  carga (FECHA_HORA_CARGA + 180 dias) y backfileada para las
--  filas existentes en este mismo script. Asi el calculo es
--  estable aunque la politica cambie en el futuro: cada fila
--  tiene su propia fecha de corte.
-- ============================================================

ALTER TABLE RESULTADO_CARGA
    ADD COLUMN FECHA_VIGENCIA_HASTA DATETIME NULL
        COMMENT 'Fecha hasta la cual el resultado esta vigente para el paciente (FECHA_HORA_CARGA + 180 dias). Pasado ese plazo se bloquea el acceso al paciente; el ADMIN sigue viendo todo.';

-- Backfill para filas existentes: 180 dias despues de la carga original.
UPDATE RESULTADO_CARGA
SET FECHA_VIGENCIA_HASTA = DATE_ADD(FECHA_HORA_CARGA, INTERVAL 180 DAY)
WHERE FECHA_VIGENCIA_HASTA IS NULL;

ALTER TABLE RESULTADO_CARGA
    MODIFY COLUMN FECHA_VIGENCIA_HASTA DATETIME NOT NULL
        COMMENT 'Fecha hasta la cual el resultado esta vigente para el paciente (FECHA_HORA_CARGA + 180 dias). Pasado ese plazo se bloquea el acceso al paciente; el ADMIN sigue viendo todo.';

CREATE INDEX IDX_RESULTADO_CARGA_VIGENCIA
    ON RESULTADO_CARGA (FECHA_VIGENCIA_HASTA);
