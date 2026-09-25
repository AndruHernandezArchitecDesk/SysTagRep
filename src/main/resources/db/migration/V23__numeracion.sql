-- Numeración de documentos: establecimiento, punto de emisión y secuencial.
-- Reemplaza el prefijo TAGVIC/TAGFAC por el formato 001-001-000000001 (SRI).

ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS establecimiento VARCHAR(3) DEFAULT '001';
ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS punto_emision VARCHAR(3) DEFAULT '001';

UPDATE secuencia_documento SET establecimiento = '001', punto_emision = '001';
