-- La factura conserva la cantidad comprada; inventario conserva el saldo real.
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS inventario_id INTEGER;
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS sucursal_id INTEGER;
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS cuenta_por_pagar_id INTEGER;
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS precio_venta DECIMAL(12,2);
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS forma_pago VARCHAR(50);
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS meses_plazo INTEGER;
ALTER TABLE factura_proveedor ADD COLUMN IF NOT EXISTS interes DECIMAL(12,2);
CREATE TABLE IF NOT EXISTS ingreso_stock_movimiento (
    factura_proveedor_id INTEGER NOT NULL REFERENCES factura_proveedor(id) ON DELETE CASCADE,
    inventario_id INTEGER NOT NULL REFERENCES inventario(id),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (factura_proveedor_id, inventario_id)
);
CREATE INDEX IF NOT EXISTS idx_factura_proveedor_inventario ON factura_proveedor(inventario_id);
CREATE INDEX IF NOT EXISTS idx_factura_proveedor_sucursal ON factura_proveedor(sucursal_id, numero_factura, proveedor_id);
CREATE INDEX IF NOT EXISTS idx_inventario_codigo_sucursal_normalizado
    ON inventario(sucursal_id, UPPER(TRIM(codigo))) WHERE estado=true;
