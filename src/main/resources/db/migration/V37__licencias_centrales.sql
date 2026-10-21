-- Fase 6b: administración central de licencias (emisión/revocación/validación)
CREATE TABLE IF NOT EXISTS licencia_emitida (
    id BIGSERIAL PRIMARY KEY,
    machine_code VARCHAR(64) NOT NULL,
    clave_hash VARCHAR(64) NOT NULL,
    modalidad VARCHAR(16) NOT NULL,
    incluye_ia BOOLEAN NOT NULL DEFAULT true,
    roles VARCHAR(255) NOT NULL DEFAULT '-',
    capacidad BIGINT NOT NULL DEFAULT 0,
    expiracion DATE,
    emitida_por INT,
    fecha TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_licencia_emitida_machine ON licencia_emitida(machine_code);

CREATE TABLE IF NOT EXISTS licencia_revocada (
    id BIGSERIAL PRIMARY KEY,
    machine_code VARCHAR(64) UNIQUE NOT NULL,
    motivo VARCHAR(255),
    revocada_por INT,
    fecha TIMESTAMP NOT NULL DEFAULT now()
);
