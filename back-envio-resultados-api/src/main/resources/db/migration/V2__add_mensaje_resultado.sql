-- ============================================================
--  Agrega el mensaje personalizado (max 500) que el agente
--  ingresa al cargar los resultados.
-- ============================================================
ALTER TABLE RESULTADO
    ADD COLUMN MENSAJE VARCHAR(500) NULL COMMENT 'Mensaje del agente (max 500 caracteres)' AFTER NOMBRE_PACIENTE;
