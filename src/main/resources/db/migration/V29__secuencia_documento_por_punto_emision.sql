-- V29__secuencia_documento_por_punto_emision.sql
-- La numeración ahora es por (punto_emision_id, tipo): cada sucursal/punto de emisión
-- tiene su propia secuencia (EEE-PPP-NNNNNNNNN).

ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS punto_emision_id INTEGER;

-- Vincular filas existentes al punto_emision de su sucursal (establecimiento=punto_emision='001')
UPDATE secuencia_documento SET punto_emision_id = (
    SELECT pe.id FROM punto_emision pe
    JOIN sucursal s ON pe.sucursal_id = s.id
    WHERE s.codigo = COALESCE(secuencia_documento.establecimiento,'001')
      AND pe.codigo = COALESCE(secuencia_documento.punto_emision,'001')
) WHERE punto_emision_id IS NULL;
UPDATE secuencia_documento SET punto_emision_id = (SELECT id FROM punto_emision LIMIT 1) WHERE punto_emision_id IS NULL;

-- Semilla: una fila por cada (punto_emision, tipo) con siguiente_numero=1
INSERT INTO secuencia_documento(punto_emision_id, tipo, prefijo, establecimiento, punto_emision, siguiente_numero)
 SELECT pe.id, t.tipo, '001', '001', '001', 1
 FROM punto_emision pe,
      (VALUES ('FACTURA'),('PROFORMA'),('NOTA_CREDITO'),('NOTA_DEBITO'),('GUIA_REMISION'),('RETENCION')) AS t(tipo)
 ON CONFLICT (punto_emision_id, tipo) DO NOTHING;

-- PK por (punto_emision_id, tipo)
ALTER TABLE secuencia_documento DROP CONSTRAINT IF EXISTS secuencia_documento_pkey;
ALTER TABLE secuencia_documento ADD PRIMARY KEY (punto_emision_id, tipo);
ALTER TABLE secuencia_documento ALTER COLUMN punto_emision_id SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE secuencia_documento TO app_vendex;
        GRANT USAGE, SELECT ON SEQUENCE secuencia_documento_punto_emision_id_seq TO app_vendex;
    END IF;
END $$;
