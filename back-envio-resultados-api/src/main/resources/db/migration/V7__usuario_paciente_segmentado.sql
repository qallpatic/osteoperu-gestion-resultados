-- ============================================================
--  Segmentacion del nombre del paciente en columnas separadas.
--  Antes: una sola columna NOMBRE con el string completo.
--  Ahora: NOMBRES + APELLIDO_PATERNO + APELLIDO_MATERNO (segmentos)
--         + NOMBRE (sigue guardando el string canonico "APELLIDO_P
--         APELLIDO_M, NOMBRES" para compat con listados y busquedas).
--  Backfill: lo que ya existe en NOMBRE queda como NOMBRES; los
--  apellidos quedan en null (se corrigen al re-consultar RENIEC).
-- ============================================================

ALTER TABLE USUARIO
    ADD COLUMN NOMBRES             VARCHAR(120) NULL AFTER NOMBRE,
    ADD COLUMN APELLIDO_PATERNO    VARCHAR(120) NULL AFTER NOMBRES,
    ADD COLUMN APELLIDO_MATERNO    VARCHAR(120) NULL AFTER APELLIDO_PATERNO;

ALTER TABLE PACIENTE
    ADD COLUMN NOMBRES             VARCHAR(120) NULL AFTER NOMBRE,
    ADD COLUMN APELLIDO_PATERNO    VARCHAR(120) NULL AFTER NOMBRES,
    ADD COLUMN APELLIDO_MATERNO    VARCHAR(120) NULL AFTER APELLIDO_PATERNO;

-- Backfill: el valor que existia en NOMBRE se mueve a NOMBRES (es la mejor
-- aproximacion sin parsear el string completo).
UPDATE USUARIO SET NOMBRES = NOMBRE WHERE NOMBRE IS NOT NULL AND NOMBRES IS NULL;
UPDATE PACIENTE SET NOMBRES = NOMBRE WHERE NOMBRE IS NOT NULL AND NOMBRES IS NULL;