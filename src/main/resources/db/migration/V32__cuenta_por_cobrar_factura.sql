-- V32__cuenta_por_cobrar_factura.sql — Creditos de FACTURA con FK propia
-- Bug: FacturaService guardaba facturaId en cuentas_por_cobrar.nota_venta_id (FK a
-- nota_venta_registro). Como ambas tablas llevan secuencias independientes, al divergir
-- los ids la insercion falla: "violates foreign key cuentas_por_cobrar_nota_venta_id_fkey".
-- Fix: columna factura_registro_id NULL con FK a factura_registro(id). Proformas siguen
-- usando nota_venta_id; facturas usan factura_registro_id con nota_venta_id NULL.

ALTER TABLE cuentas_por_cobrar ADD COLUMN IF NOT EXISTS factura_registro_id INTEGER REFERENCES factura_registro(id);
CREATE INDEX IF NOT EXISTS idx_cpc_factura_registro ON cuentas_por_cobrar(factura_registro_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE cuentas_por_cobrar TO app_vendex;
    END IF;
END $$;
