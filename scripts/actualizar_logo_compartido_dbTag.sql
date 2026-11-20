-- Actualización manual e idempotente para dbTag.
-- Ejecutar como postgres/propietario en la base compartida, por ejemplo:
--   psql -h <servidor> -U postgres -d dbTag -f scripts/actualizar_logo_compartido_dbTag.sql

-- Repara la columna tributaria faltante que utiliza el guardado de Mi Empresa.
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS obligado_contabilidad VARCHAR(2) DEFAULT 'NO';
UPDATE empresa SET obligado_contabilidad = 'NO' WHERE obligado_contabilidad IS NULL;

-- Bytes del logo compartido.
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS logo_bytes BYTEA;

DO $$
BEGIN
    IF to_regclass('configuracion_marca_agua') IS NOT NULL THEN
        UPDATE empresa e
        SET logo_bytes = c.logo_bytes
        FROM configuracion_marca_agua c
        WHERE c.id = 1
          AND c.logo_bytes IS NOT NULL
          AND e.logo_bytes IS NULL
          AND e.id = (SELECT id FROM empresa ORDER BY estado DESC, id LIMIT 1);

        UPDATE configuracion_marca_agua SET logo_bytes = NULL WHERE id = 1;
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_vendex') THEN
        GRANT SELECT, UPDATE ON TABLE empresa TO app_vendex;
    END IF;
END
$$;

-- empresa.logo_bytes es el origen compartido; logo_url queda para clientes antiguos.
