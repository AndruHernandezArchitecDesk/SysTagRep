CREATE TABLE IF NOT EXISTS comprobante_temp (
    id SERIAL PRIMARY KEY,
    proforma_id INT REFERENCES nota_venta_registro(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario NUMERIC(12,2) NOT NULL,
    precio_total NUMERIC(12,2) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_comprobante_temp_proforma ON comprobante_temp(proforma_id);
