-- V28__usuarios_sucursal.sql
-- Cada usuario queda ligado a una sucursal (alcance de datos por sucursal)

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_usuarios_sucursal_id ON usuarios(sucursal_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE usuarios TO app_vendex;
        GRANT USAGE, SELECT ON SEQUENCE usuarios_id_seq TO app_vendex;
    END IF;
END $$;
