-- V35__mi_empresa.sql — Columnas para la vista "Mi Empresa" (CRUD registro único).
-- Idempotente: IF NOT EXISTS + backfill de estado.
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS titulo VARCHAR(300);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS estado BOOLEAN DEFAULT TRUE;
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS obligado_contabilidad VARCHAR(2) DEFAULT 'NO';
UPDATE empresa SET estado = TRUE WHERE estado IS NULL;
UPDATE empresa SET obligado_contabilidad = 'NO' WHERE obligado_contabilidad IS NULL;
