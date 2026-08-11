CREATE TABLE IF NOT EXISTS alertas (
    id SERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    mensaje TEXT NOT NULL,
    referencia_id INTEGER,
    referencia_tipo VARCHAR(50),
    leida BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT NOW(),
    fecha_lectura TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alertas_leida ON alertas(leida);
CREATE INDEX IF NOT EXISTS idx_alertas_tipo ON alertas(tipo);
