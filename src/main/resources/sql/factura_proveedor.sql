CREATE TABLE IF NOT EXISTS factura_proveedor (
    id SERIAL PRIMARY KEY,
    numero_factura VARCHAR(50),
    proveedor_id INTEGER,
    codigo VARCHAR(100),
    codigo_manual VARCHAR(100),
    descripcion VARCHAR(500),
    grupo_id INTEGER,
    marca_id INTEGER,
    costo_sin_iva DECIMAL(12,2),
    iva DECIMAL(12,2),
    cantidad INTEGER,
    total_linea DECIMAL(12,2),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_factura_proveedor_num ON factura_proveedor(numero_factura);
CREATE INDEX IF NOT EXISTS idx_factura_proveedor_prov ON factura_proveedor(proveedor_id);
