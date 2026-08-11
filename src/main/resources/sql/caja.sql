CREATE TABLE IF NOT EXISTS caja_sesion (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    fecha_apertura TIMESTAMP DEFAULT NOW(),
    fecha_cierre TIMESTAMP,
    monto_inicial NUMERIC(12,2) NOT NULL,
    monto_fisico NUMERIC(12,2),
    diferencia NUMERIC(12,2),
    estado VARCHAR(20) DEFAULT 'ABIERTA',
    observaciones TEXT
);

CREATE TABLE IF NOT EXISTS caja_movimiento (
    id SERIAL PRIMARY KEY,
    sesion_id INTEGER NOT NULL REFERENCES caja_sesion(id),
    tipo VARCHAR(20) NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    descripcion TEXT,
    referencia_id INTEGER,
    referencia_tipo VARCHAR(50),
    fecha TIMESTAMP DEFAULT NOW(),
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_caja_sesion_estado ON caja_sesion(estado);
CREATE INDEX IF NOT EXISTS idx_caja_movimiento_sesion ON caja_movimiento(sesion_id);
CREATE INDEX IF NOT EXISTS idx_caja_movimiento_fecha ON caja_movimiento(fecha);
