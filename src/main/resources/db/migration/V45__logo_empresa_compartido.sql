-- El logo de empresa se guarda una sola vez en la fila de empresa para todos los clientes.
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS logo_bytes BYTEA;

-- Migra el logo compartido existente de la configuración de marca de agua, si lo había.
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

        -- La copia canónica queda en empresa.logo_bytes; elimina la duplicada.
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
