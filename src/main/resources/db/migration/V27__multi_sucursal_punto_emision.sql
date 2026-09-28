-- V27__multi_sucursal_punto_emision.sql
-- Tabla punto_emision: cada sucursal tiene sus propios puntos de emisión (EEE-PPP-NNNNNNNNN)

CREATE TABLE IF NOT EXISTS punto_emision (
    id SERIAL PRIMARY KEY,
    sucursal_id INTEGER NOT NULL REFERENCES sucursal(id),
    codigo VARCHAR(3) NOT NULL,
    descripcion VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (sucursal_id, codigo)
);

-- Un punto de emisión por sucursal existente (001 = punto principal)
INSERT INTO punto_emision(sucursal_id, codigo, descripcion)
 SELECT id, '001', 'Punto emisión principal' FROM sucursal
 ON CONFLICT (sucursal_id, codigo) DO NOTHING;

-- Grants
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE punto_emision TO app_vendex;
        GRANT USAGE, SELECT ON SEQUENCE punto_emision_id_seq TO app_vendex;
    END IF;
END $$;
