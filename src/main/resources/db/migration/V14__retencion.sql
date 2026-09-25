-- Comprobante de Retencion Electronico SRI tipo 07 v2.0.0
CREATE TABLE IF NOT EXISTS retencion_registro (
    id                          SERIAL PRIMARY KEY,
    clave_acceso                VARCHAR(49) UNIQUE NOT NULL,
    establecimiento             VARCHAR(3) NOT NULL DEFAULT '001',
    punto_emision               VARCHAR(3) NOT NULL DEFAULT '001',
    secuencial                  VARCHAR(9) NOT NULL,
    fecha_emision               TIMESTAMP NOT NULL DEFAULT now(),
    periodo_fiscal              VARCHAR(7) NOT NULL,
    proveedor_id                INTEGER REFERENCES proveedor(id),
    tipo_identificacion_sujeto  VARCHAR(2) NOT NULL,
    razon_social_sujeto         VARCHAR(300) NOT NULL,
    identificacion_sujeto       VARCHAR(13) NOT NULL,
    estado_sri                  VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_sri                 VARCHAR(500),
    numero_autorizacion         VARCHAR(49),
    fecha_autorizacion          TIMESTAMP,
    xml_firmado                 TEXT,
    usuario_id                  INTEGER NOT NULL REFERENCES usuarios(id),
    creado_en                   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS retencion_documento_sustento (
    id                          SERIAL PRIMARY KEY,
    retencion_id                INTEGER NOT NULL REFERENCES retencion_registro(id) ON DELETE CASCADE,
    cod_sustento                VARCHAR(2) NOT NULL DEFAULT '01',
    cod_doc_sustento            VARCHAR(2) NOT NULL DEFAULT '01',
    num_doc_sustento            VARCHAR(17) NOT NULL,
    fecha_emision_doc_sustento  DATE NOT NULL,
    total_sin_impuestos         NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS retencion_detalle (
    id                          SERIAL PRIMARY KEY,
    doc_sustento_id             INTEGER NOT NULL REFERENCES retencion_documento_sustento(id) ON DELETE CASCADE,
    codigo                      VARCHAR(1) NOT NULL,
    codigo_retencion            VARCHAR(4) NOT NULL,
    base_imponible              NUMERIC(12,2) NOT NULL,
    porcentaje_retener          NUMERIC(5,2) NOT NULL,
    valor_retenido              NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS tabla_retencion (
    id                          SERIAL PRIMARY KEY,
    codigo_retencion            VARCHAR(4) NOT NULL,
    descripcion                 VARCHAR(200) NOT NULL,
    tipo                        VARCHAR(1) NOT NULL,
    porcentaje                  NUMERIC(5,2) NOT NULL,
    vigente_desde               DATE NOT NULL,
    vigente_hasta               DATE
);

INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('RETENCION','001','001','001',1) ON CONFLICT (tipo) DO NOTHING;

-- Ejemplos vigentes desde 2026-03-01 (Res. NAC-DGERCGC26-00000009)
INSERT INTO tabla_retencion (codigo_retencion, descripcion, tipo, porcentaje, vigente_desde) VALUES
('303','Honorarios profesionales','1',10.00,'2026-03-01') ON CONFLICT DO NOTHING,
('312','Transporte privado pasajeros','1',1.00,'2026-03-01') ON CONFLICT DO NOTHING,
('725','IVA bienes','2',30.00,'2026-03-01') ON CONFLICT DO NOTHING,
('723','IVA servicios','2',70.00,'2026-03-01') ON CONFLICT DO NOTHING,
('322','Seguros y reaseguros','1',1.75,'2026-03-01') ON CONFLICT DO NOTHING
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_retencion_clave ON retencion_registro(clave_acceso);
CREATE INDEX IF NOT EXISTS idx_retencion_estado ON retencion_registro(estado_sri);
CREATE INDEX IF NOT EXISTS idx_retencion_doc_retencion ON retencion_documento_sustento(retencion_id);
CREATE INDEX IF NOT EXISTS idx_retencion_det_doc ON retencion_detalle(doc_sustento_id);
CREATE INDEX IF NOT EXISTS idx_tabla_retencion_codigo ON tabla_retencion(codigo_retencion);
