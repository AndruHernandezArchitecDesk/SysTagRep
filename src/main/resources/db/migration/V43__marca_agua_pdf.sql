-- Configuración única, compartida por los clientes conectados a la misma base.
CREATE TABLE IF NOT EXISTS configuracion_marca_agua (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    factura_activa BOOLEAN NOT NULL DEFAULT FALSE,
    proforma_activa BOOLEAN NOT NULL DEFAULT FALSE,
    distribucion VARCHAR(16) NOT NULL DEFAULT 'CENTRO',
    opacidad DECIMAL(4,3) NOT NULL DEFAULT 0.12 CHECK (opacidad >= 0.05 AND opacidad <= 0.40),
    logo_bytes BYTEA,
    actualizado_en TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO configuracion_marca_agua
    (id, factura_activa, proforma_activa, distribucion, opacidad)
VALUES (1, FALSE, FALSE, 'CENTRO', 0.12)
ON CONFLICT (id) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE configuracion_marca_agua TO app_vendex;
    END IF;
END
$$;
