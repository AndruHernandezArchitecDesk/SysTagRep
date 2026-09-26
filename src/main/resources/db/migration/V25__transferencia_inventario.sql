-- V25__transferencia_inventario.sql — Transferencias entre sucursales (V1: misma BD)

CREATE TABLE IF NOT EXISTS transferencia_inventario (
    id SERIAL PRIMARY KEY,
    inventario_id INTEGER NOT NULL REFERENCES inventario(id) ON DELETE CASCADE,
    origen_sucursal_id INTEGER NOT NULL REFERENCES sucursal(id),
    destino_sucursal_id INTEGER NOT NULL REFERENCES sucursal(id),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA', -- COMPLETADA, ANULADA
    usuario_id INTEGER REFERENCES usuarios(id),
    motivo VARCHAR(300),
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_sucursal_distinta CHECK (origen_sucursal_id <> destino_sucursal_id)
);

CREATE INDEX IF NOT EXISTS idx_transferencia_inventario ON transferencia_inventario(inventario_id);
CREATE INDEX IF NOT EXISTS idx_transferencia_origen ON transferencia_inventario(origen_sucursal_id);
CREATE INDEX IF NOT EXISTS idx_transferencia_destino ON transferencia_inventario(destino_sucursal_id);
CREATE INDEX IF NOT EXISTS idx_transferencia_fecha ON transferencia_inventario(creado_en);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT ON TABLE transferencia_inventario TO app_vendex;
        GRANT USAGE, SELECT ON SEQUENCE transferencia_inventario_id_seq TO app_vendex;
    END IF;
END $$;
