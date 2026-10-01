CREATE TABLE IF NOT EXISTS secuencia_bloque (
    id SERIAL PRIMARY KEY,
    punto_emision_id INTEGER NOT NULL REFERENCES punto_emision(id),
    tipo VARCHAR(20) NOT NULL,
    numero_inicio INTEGER NOT NULL,
    numero_fin INTEGER NOT NULL,
    usado_hasta INTEGER NOT NULL,
    reservado_en TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (punto_emision_id, tipo, numero_inicio, numero_fin)
);

CREATE INDEX IF NOT EXISTS idx_secuencia_bloque_punto_tipo ON secuencia_bloque(punto_emision_id, tipo);
