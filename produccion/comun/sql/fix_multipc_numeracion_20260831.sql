-- Fix multi-PC 192.168.1.7 (host) + 192.168.1.5 (cliente) - numeracion sin solapamiento
-- Ejecutar UNA VEZ en la BD central (dbTag en 192.168.1.7) como postgres/app_systag:
-- psql -h localhost -U postgres -d dbTag -f fix_multipc_numeracion_20260831.sql

-- 1. Asegurar tabla secuencia_documento existe y tiene filas 001-001 para ambas PCs compartiendo secuencia
CREATE TABLE IF NOT EXISTS secuencia_documento (
    tipo VARCHAR(20) PRIMARY KEY,
    prefijo VARCHAR(10) DEFAULT '001',
    establecimiento VARCHAR(3) DEFAULT '001',
    punto_emision VARCHAR(3) DEFAULT '001',
    siguiente_numero INTEGER NOT NULL DEFAULT 1
);

-- Si vino de instalacion antigua sin columnas establecimiento/punto
ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS prefijo VARCHAR(10) DEFAULT '001';
ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS establecimiento VARCHAR(3) DEFAULT '001';
ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS punto_emision VARCHAR(3) DEFAULT '001';
ALTER TABLE secuencia_documento ADD COLUMN IF NOT EXISTS siguiente_numero INTEGER DEFAULT 1;

-- Normalizar existentes a 001-001 (centralizado para ambas PCs)
UPDATE secuencia_documento SET establecimiento='001', punto_emision='001' WHERE establecimiento IS NULL OR punto_emision IS NULL;

-- Crear filas si no existen (FACTURA y PROFORMA comparten numeracion 001-001)
INSERT INTO secuencia_documento(tipo, prefijo, establecimiento, punto_emision, siguiente_numero)
VALUES ('FACTURA','001','001','001',1) ON CONFLICT (tipo) DO NOTHING;
INSERT INTO secuencia_documento(tipo, prefijo, establecimiento, punto_emision, siguiente_numero)
VALUES ('PROFORMA','001','001','001',1) ON CONFLICT (tipo) DO NOTHING;

-- Sincronizar siguiente_numero al MAX+1 por si hay huecos previos (evita colision tras migracion)
DO $$
DECLARE
    v_max_fact INT;
    v_max_nota INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SPLIT_PART(codigo,'-',3) AS INTEGER)),0) INTO v_max_fact FROM factura_registro WHERE codigo ~ '^[0-9]{3}-[0-9]{3}-[0-9]{9}$';
    SELECT COALESCE(MAX(CAST(SPLIT_PART(codigo,'-',3) AS INTEGER)),0) INTO v_max_nota FROM nota_venta_registro WHERE codigo ~ '^[0-9]{3}-[0-9]{3}-[0-9]{9}$';
    -- FACTURA
    UPDATE secuencia_documento SET siguiente_numero = GREATEST(siguiente_numero, v_max_fact+1) WHERE tipo='FACTURA';
    -- PROFORMA
    UPDATE secuencia_documento SET siguiente_numero = GREATEST(siguiente_numero, v_max_nota+1) WHERE tipo='PROFORMA';
END $$;

-- 2. Evitar duplicados aunque haya race condition entre 192.168.1.7 y 192.168.1.5
-- Si ya existen duplicados, primero limpiar manualmente: SELECT codigo, COUNT(*) FROM factura_registro GROUP BY codigo HAVING COUNT(*)>1;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uq_factura_codigo') THEN
        ALTER TABLE factura_registro ADD CONSTRAINT uq_factura_codigo UNIQUE (codigo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uq_factura_num_comprobante') THEN
        -- solo si la columna tiene datos unicos; si ya hay duplicados fallara -> limpiar primero
        BEGIN
            ALTER TABLE factura_registro ADD CONSTRAINT uq_factura_num_comprobante UNIQUE (num_comprobante);
        EXCEPTION WHEN duplicate_table THEN NULL;
        END;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uq_nota_codigo') THEN
        ALTER TABLE nota_venta_registro ADD CONSTRAINT uq_nota_codigo UNIQUE (codigo);
    END IF;
END $$;

-- Verificacion
SELECT 'secuencia_documento' AS tabla, tipo, establecimiento||'-'||punto_emision AS serie, siguiente_numero FROM secuencia_documento ORDER BY tipo;
SELECT 'constraints' AS check, conname, contype FROM pg_constraint WHERE conname IN ('uq_factura_codigo','uq_factura_num_comprobante','uq_nota_codigo');
