-- Repara bases donde V35 conste aplicada pero la columna no exista realmente.
ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS obligado_contabilidad VARCHAR(2) DEFAULT 'NO';

UPDATE empresa
SET obligado_contabilidad = 'NO'
WHERE obligado_contabilidad IS NULL;
