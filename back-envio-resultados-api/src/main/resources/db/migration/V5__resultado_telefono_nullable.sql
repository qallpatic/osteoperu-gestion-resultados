-- Telefono del paciente pasa a ser opcional en RESULTADO.
-- Antes la columna era NOT NULL; ahora permitimos NULL cuando el paciente no
-- tiene telefono registrado (la entrega del resultado sigue siendo valida).
ALTER TABLE RESULTADO MODIFY COLUMN TELEFONO VARCHAR(20) NULL COMMENT 'Numero destino normalizado (51 + 9 digitos). Opcional.';