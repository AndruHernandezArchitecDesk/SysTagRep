-- V24__multi_sucursal.sql — Multi-sucursal V1 (misma BD dbVendex en vendex-db)
-- Crea catálogo sucursal y extiende tablas transaccionales con sucursal_id DEFAULT 1 (preserva datos existentes)

CREATE TABLE IF NOT EXISTS sucursal (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(3) UNIQUE NOT NULL, -- 001, 002...
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(300),
    telefono VARCHAR(20),
    es_central BOOLEAN NOT NULL DEFAULT false,
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO sucursal(codigo, nombre, direccion, es_central, activo)
VALUES ('001', 'Matriz', 'Matriz', true, true)
ON CONFLICT (codigo) DO NOTHING;

-- Inventario por sucursal (stock separado)
ALTER TABLE inventario ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_inventario_sucursal ON inventario(sucursal_id);

-- Facturación por sucursal (origen)
ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_factura_registro_sucursal ON factura_registro(sucursal_id);

ALTER TABLE nota_venta_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_nota_venta_sucursal ON nota_venta_registro(sucursal_id);

ALTER TABLE nota_credito_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_nota_credito_sucursal ON nota_credito_registro(sucursal_id);

ALTER TABLE nota_debito_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_nota_debito_sucursal ON nota_debito_registro(sucursal_id);

ALTER TABLE guia_remision_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_guia_sucursal ON guia_remision_registro(sucursal_id);

ALTER TABLE retencion_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_retencion_sucursal ON retencion_registro(sucursal_id);

ALTER TABLE caja_sesion ADD COLUMN IF NOT EXISTS sucursal_id INTEGER REFERENCES sucursal(id) DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_caja_sesion_sucursal ON caja_sesion(sucursal_id);

-- Secuencia por sucursal (opcional V2): por ahora se mantiene global (001/001) y se documenta mapeo sucursal->establecimiento
-- Si se requiere numeración independiente por sucursal, crear tabla secuencia_sucursal en V25

-- Grants mínimo privilegio (app_vendex solo SELECT/INSERT/UPDATE donde corresponde)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE sucursal TO app_vendex;
        GRANT USAGE, SELECT ON SEQUENCE sucursal_id_seq TO app_vendex;
        GRANT SELECT, UPDATE ON TABLE inventario TO app_vendex;
        GRANT SELECT, INSERT ON TABLE factura_registro TO app_vendex;
    END IF;
END $$;
