CREATE TABLE IF NOT EXISTS historial_producto (
    id SERIAL PRIMARY KEY,
    producto_id INTEGER NOT NULL REFERENCES inventario(id),
    producto_codigo VARCHAR(100),
    producto_descripcion VARCHAR(500),
    cantidad INTEGER NOT NULL,
    precio_unitario DECIMAL(12,2),
    tipo_comprobante VARCHAR(20) NOT NULL,
    codigo_comprobante VARCHAR(100),
    cliente_nombre VARCHAR(300),
    proveedor_nombre VARCHAR(300),
    fecha_venta TIMESTAMP NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_historial_producto_id ON historial_producto(producto_id);
CREATE INDEX IF NOT EXISTS idx_historial_fecha ON historial_producto(fecha_venta);
