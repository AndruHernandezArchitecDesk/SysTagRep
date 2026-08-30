-- =============================================================
-- fix_nota_venta_varchar_20260828.sql
-- Corrige: BatchUpdateException en NotaVentaDetalleDAO.java:41
--   ERROR: value too long for type character varying(50)
--   INSERT nota_venta_detalle descripcion 87 chars > 50
--   Ej: 'KIT CAUCHOS PISTON MORDAZAS HY TUCOSN IX/SPORTAGE R/GRAND I10 1.ELANTRA/CERATO FORTE LS'
-- Homologa a comprobante_temp.sql:5 y factura_proveedor.sql:7 (VARCHAR(500))
-- Idempotente - ejecutar en PRD dbTag
-- =============================================================

-- 0) Diagnóstico
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name='nota_venta_detalle' AND column_name='descripcion';

-- SELECT MAX(length(descripcion)) FROM nota_venta_detalle;
-- SELECT MAX(length(descripcion)) FROM inventario;

-- 1) Fix principal
ALTER TABLE nota_venta_detalle ALTER COLUMN descripcion TYPE VARCHAR(500);

-- 2) Preventivo: homologar factura_detalle si sigue en 100/50
DO $$ BEGIN
  BEGIN
    ALTER TABLE factura_detalle ALTER COLUMN descripcion TYPE VARCHAR(500);
    RAISE NOTICE 'factura_detalle.descripcion -> 500';
  EXCEPTION WHEN undefined_table OR undefined_column THEN
    RAISE NOTICE 'skip factura_detalle';
  END;
END $$;

-- 3) Verificación
SELECT table_name, column_name, character_maximum_length
FROM information_schema.columns
WHERE table_name IN ('nota_venta_detalle','factura_detalle','comprobante_temp','factura_proveedor')
  AND column_name='descripcion';
