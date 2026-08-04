-- =============================================================
-- Limpieza de datos de prueba.
-- Conserva únicamente los datos esenciales:
--   · empresa (datos de la empresa)
--   · usuarios (usuarios del sistema)
--   · secuenciales (secuencia SRI para comprobantes electrónicos)
--   · secuencia_documento (numeración de documentos)
-- Reinicia la numeración de documentos a 001-001-000000001.
-- También reinicia la secuencia SRI (tabla secuenciales): la siguiente
-- factura electrónica usará el secuencial 001-001-000000001.
-- =============================================================
BEGIN;

-- Elimina todas las tablas de datos de prueba / transaccionales.
TRUNCATE TABLE
  historial_producto, cuentas_por_pagar, cuentas_por_cobrar,
  comprobantes_electronicos, xml_enviados, factura_detalle,
  factura_registro, factura_proveedor, nota_venta_detalle,
  nota_venta_registro, inventario, ubicacion, perchero,
  ubicacion_percha, logs, codigo, cliente,
  vendedor, grupo, marca, proveedor
RESTART IDENTITY CASCADE;

-- Reinicia la numeración de documentos a 001-001-000000001
-- (establecimiento 001, punto de emisión 001, secuencial 1).
UPDATE secuencia_documento
SET establecimiento = '001', punto_emision = '001', siguiente_numero = 1
WHERE tipo IN ('PROFORMA', 'FACTURA');

-- Asegura que existan las filas de numeración para proforma y factura.
INSERT INTO secuencia_documento (tipo, establecimiento, punto_emision, siguiente_numero)
SELECT 'PROFORMA', '001', '001', 1
WHERE NOT EXISTS (SELECT 1 FROM secuencia_documento WHERE tipo = 'PROFORMA');
INSERT INTO secuencia_documento (tipo, establecimiento, punto_emision, siguiente_numero)
SELECT 'FACTURA', '001', '001', 1
WHERE NOT EXISTS (SELECT 1 FROM secuencia_documento WHERE tipo = 'FACTURA');

COMMIT;
