-- Notas de Debito Electronicas SRI tipo 05 - Vendex 2.0
-- Esquema notaDebito v1.0.0 - referencia obligatoria a factura autorizada

CREATE TABLE IF NOT EXISTS nota_debito_registro (
    id                      SERIAL PRIMARY KEY,
    clave_acceso            VARCHAR(49) UNIQUE NOT NULL,
    factura_registro_id     INTEGER NOT NULL REFERENCES factura_registro(id),
    establecimiento         VARCHAR(3) NOT NULL DEFAULT '001',
    punto_emision           VARCHAR(3) NOT NULL DEFAULT '001',
    secuencial              VARCHAR(9) NOT NULL,
    fecha_emision           TIMESTAMP NOT NULL DEFAULT now(),
    cliente_id              INTEGER NOT NULL REFERENCES cliente(id),
    forma_pago              VARCHAR(2) NOT NULL,
    total_sin_impuestos     NUMERIC(12,2) NOT NULL,
    valor_iva               NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_total             NUMERIC(12,2) NOT NULL,
    estado_sri              VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_sri             VARCHAR(500),
    numero_autorizacion     VARCHAR(49),
    fecha_autorizacion      TIMESTAMP,
    xml_firmado             TEXT,
    usuario_id              INTEGER NOT NULL REFERENCES usuarios(id),
    creado_en               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS nota_debito_motivo (
    id                      SERIAL PRIMARY KEY,
    nota_debito_id          INTEGER NOT NULL REFERENCES nota_debito_registro(id) ON DELETE CASCADE,
    razon                   VARCHAR(300) NOT NULL,
    valor                   NUMERIC(12,2) NOT NULL,
    grava_iva               BOOLEAN NOT NULL DEFAULT true,
    codigo_porcentaje_iva   VARCHAR(2) NOT NULL DEFAULT '4'
);

INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero)
VALUES ('NOTA_DEBITO', '001', '001', '001', 1)
ON CONFLICT (tipo) DO NOTHING;

ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS estado_nd VARCHAR(20) NOT NULL DEFAULT 'NINGUNA';

CREATE INDEX IF NOT EXISTS idx_nota_debito_factura ON nota_debito_registro(factura_registro_id);
CREATE INDEX IF NOT EXISTS idx_nota_debito_estado ON nota_debito_registro(estado_sri);
CREATE INDEX IF NOT EXISTS idx_nota_debito_motivo ON nota_debito_motivo(nota_debito_id);
