-- Notas de Credito Electronicas SRI tipo 04 - Vendex 2.0
-- Esquema notaCredito v1.1.0 - referencia obligatoria a factura autorizada

CREATE TABLE IF NOT EXISTS nota_credito_registro (
    id                      SERIAL PRIMARY KEY,
    clave_acceso            VARCHAR(49) UNIQUE NOT NULL,
    factura_registro_id     INTEGER NOT NULL REFERENCES factura_registro(id),
    establecimiento         VARCHAR(3) NOT NULL DEFAULT '001',
    punto_emision           VARCHAR(3) NOT NULL DEFAULT '001',
    secuencial              VARCHAR(9) NOT NULL,
    fecha_emision           TIMESTAMP NOT NULL DEFAULT now(),
    cliente_id              INTEGER NOT NULL REFERENCES cliente(id),
    motivo                  VARCHAR(300) NOT NULL,
    tipo_motivo             VARCHAR(20) NOT NULL CHECK (tipo_motivo IN ('DEVOLUCION','DESCUENTO','ANULACION')),
    total_sin_impuestos     NUMERIC(12,2) NOT NULL,
    valor_iva               NUMERIC(12,2) NOT NULL,
    valor_modificacion      NUMERIC(12,2) NOT NULL,
    reingresa_stock         BOOLEAN NOT NULL DEFAULT false,
    estado_sri              VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_sri             VARCHAR(500),
    numero_autorizacion     VARCHAR(49),
    fecha_autorizacion      TIMESTAMP,
    xml_firmado             TEXT,
    usuario_id              INTEGER NOT NULL REFERENCES usuario(id),
    creado_en               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS nota_credito_detalle (
    id                          SERIAL PRIMARY KEY,
    nota_credito_id             INTEGER NOT NULL REFERENCES nota_credito_registro(id) ON DELETE CASCADE,
    factura_detalle_id          INTEGER REFERENCES factura_detalle(id),
    inventario_id               INTEGER REFERENCES inventario(id),
    descripcion                 VARCHAR(300) NOT NULL,
    cantidad                    NUMERIC(10,2) NOT NULL,
    precio_unitario             NUMERIC(12,4) NOT NULL,
    descuento                   NUMERIC(12,2) NOT NULL DEFAULT 0,
    codigo_porcentaje_iva       VARCHAR(2) NOT NULL DEFAULT '4',
    precio_total_sin_impuesto   NUMERIC(12,2) NOT NULL
);

-- Numeracion independiente SRI serie propia por tipo comprobante
INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero)
VALUES ('NOTA_CREDITO', '001', '001', '001', 1)
ON CONFLICT (tipo) DO NOTHING;

-- Estado NC en factura original
ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS estado_nc VARCHAR(20) NOT NULL DEFAULT 'NINGUNA';

CREATE INDEX IF NOT EXISTS idx_nota_credito_factura ON nota_credito_registro(factura_registro_id);
CREATE INDEX IF NOT EXISTS idx_nota_credito_estado ON nota_credito_registro(estado_sri);
CREATE INDEX IF NOT EXISTS idx_nota_credito_detalle_nc ON nota_credito_detalle(nota_credito_id);
