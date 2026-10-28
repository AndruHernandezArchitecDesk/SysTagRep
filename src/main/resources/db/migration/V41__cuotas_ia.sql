-- Una instalación/base compartida representa un proyecto Gemini.
CREATE TABLE IF NOT EXISTS ia_cuota (
    modelo VARCHAR(100) PRIMARY KEY, configuracion TEXT NOT NULL, pausa_hasta BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS ia_solicitud (
    id VARCHAR(36) PRIMARY KEY, modelo VARCHAR(100) NOT NULL REFERENCES ia_cuota(modelo),
    operacion VARCHAR(100) NOT NULL, fecha BIGINT NOT NULL, estimados BIGINT NOT NULL,
    http INTEGER NOT NULL DEFAULT -1, duracion BIGINT NOT NULL DEFAULT 0,
    entrada BIGINT NOT NULL DEFAULT 0, salida BIGINT NOT NULL DEFAULT 0,
    pensamiento BIGINT NOT NULL DEFAULT 0, total BIGINT NOT NULL DEFAULT 0,
    detalle VARCHAR(300) NOT NULL DEFAULT '', costo DECIMAL(20,8)
);
CREATE INDEX IF NOT EXISTS idx_ia_solicitud_modelo_fecha ON ia_solicitud(modelo, fecha);
INSERT INTO ia_cuota(modelo, configuracion)
SELECT TRIM(m), '{"modelo":"' || TRIM(m) || '","nivel":"Gratuito","habilitado":true,"rpm":0,"tpm":0,"rpd":0,"maxSalida":2048,"ahorro":true}'
FROM (VALUES ('gemini-3.1-flash-lite'), ('gemini-2.5-flash'), ('gemini-flash-latest'), ('gemini-flash-lite-latest')) AS modelos(m)
WHERE NOT EXISTS (SELECT 1 FROM ia_cuota WHERE modelo = TRIM(m));
