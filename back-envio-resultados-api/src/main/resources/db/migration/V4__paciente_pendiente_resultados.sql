-- ============================================================
--  Marca de "resultados pendientes de subir" para el paciente.
--  Admision registra al paciente (pendiente=1); Resultados lo
--  ve en su campanita y al subir resultados pasa a 0.
-- ============================================================
ALTER TABLE PACIENTE
    ADD COLUMN PENDIENTE_RESULTADOS TINYINT NOT NULL DEFAULT 0
    COMMENT 'Registrado por Admision con resultados pendientes de subir' AFTER ESTADO;
