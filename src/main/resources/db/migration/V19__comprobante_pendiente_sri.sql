-- V19__comprobante_pendiente_sri.sql — cola persistente SRI contingencia (24h)
CREATE TABLE IF NOT EXISTS comprobante_pendiente_sri (
    id SERIAL PRIMARY KEY,
    comprobante_id INTEGER REFERENCES comprobantes_electronicos(id),
    tipo_comprobante VARCHAR(20) NOT NULL,
    clave_acceso VARCHAR(49) UNIQUE NOT NULL,
    numero_comprobante VARCHAR(30),
    ambiente VARCHAR(10),
    intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_intento TIMESTAMP,
    proximo_intento TIMESTAMP NOT NULL DEFAULT now(),
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    ultimo_mensaje_sri VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_pendiente_estado_proximo ON comprobante_pendiente_sri(estado, proximo_intento);
CREATE INDEX IF NOT EXISTS idx_pendiente_clave ON comprobante_pendiente_sri(clave_acceso);
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE comprobante_pendiente_sri TO app_vendex;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex;
