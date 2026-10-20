-- Fase 3 Vendexi AI: medición de consumo (tokens/costos), periodo de capacidad y sesiones de interacción.
-- La capacidad de la licencia se mide en tokens totales (entrada + salida) del periodo vigente.

CREATE TABLE IF NOT EXISTS vendexi_consumo (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL DEFAULT now(),
    modulo VARCHAR(40) NOT NULL,
    operacion VARCHAR(40) NOT NULL,
    modelo VARCHAR(80),
    tokens_entrada BIGINT NOT NULL DEFAULT 0,
    tokens_salida BIGINT NOT NULL DEFAULT 0,
    costo_estimado NUMERIC(12,6) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_vendexi_consumo_fecha ON vendexi_consumo(fecha);

CREATE TABLE IF NOT EXISTS vendexi_periodo (
    id INT PRIMARY KEY DEFAULT 1,
    inicio TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT vendexi_periodo_unico CHECK (id = 1)
);

INSERT INTO vendexi_periodo (id, inicio) VALUES (1, now()) ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS vendexi_sesion (
    id BIGSERIAL PRIMARY KEY,
    inicio TIMESTAMP NOT NULL DEFAULT now(),
    fin TIMESTAMP,
    duracion_seg BIGINT,
    modulo VARCHAR(40) NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'CONSULTA'
);

CREATE INDEX IF NOT EXISTS idx_vendexi_sesion_inicio ON vendexi_sesion(inicio);
