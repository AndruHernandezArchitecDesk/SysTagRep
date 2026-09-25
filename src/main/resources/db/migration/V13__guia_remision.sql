-- Guia de Remision Electronica SRI tipo 06
CREATE TABLE IF NOT EXISTS guia_remision_registro (
    id                              SERIAL PRIMARY KEY,
    clave_acceso                    VARCHAR(49) UNIQUE NOT NULL,
    establecimiento                 VARCHAR(3) NOT NULL DEFAULT '001',
    punto_emision                   VARCHAR(3) NOT NULL DEFAULT '001',
    secuencial                      VARCHAR(9) NOT NULL,
    fecha_emision                   TIMESTAMP NOT NULL DEFAULT now(),
    dir_partida                     VARCHAR(300) NOT NULL,
    razon_social_transportista      VARCHAR(300) NOT NULL,
    tipo_identificacion_transportista VARCHAR(2) NOT NULL,
    ruc_transportista               VARCHAR(13) NOT NULL,
    placa                           VARCHAR(10) NOT NULL,
    fecha_ini_transporte            DATE NOT NULL,
    fecha_fin_transporte            DATE NOT NULL,
    estado_sri                      VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_sri                     VARCHAR(500),
    numero_autorizacion             VARCHAR(49),
    fecha_autorizacion              TIMESTAMP,
    xml_firmado                     TEXT,
    usuario_id                      INTEGER NOT NULL REFERENCES usuarios(id),
    creado_en                       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS guia_remision_destinatario (
    id                              SERIAL PRIMARY KEY,
    guia_remision_id                INTEGER NOT NULL REFERENCES guia_remision_registro(id) ON DELETE CASCADE,
    identificacion_destinatario     VARCHAR(20) NOT NULL,
    razon_social_destinatario       VARCHAR(300) NOT NULL,
    direccion_destinatario          VARCHAR(300) NOT NULL,
    motivo_traslado                 VARCHAR(300) NOT NULL,
    factura_registro_id             INTEGER REFERENCES factura_registro(id),
    cod_doc_sustento                VARCHAR(2),
    num_doc_sustento                VARCHAR(17),
    num_aut_doc_sustento            VARCHAR(49),
    fecha_emision_doc_sustento      DATE
);

CREATE TABLE IF NOT EXISTS guia_remision_detalle (
    id                              SERIAL PRIMARY KEY,
    guia_remision_destinatario_id   INTEGER NOT NULL REFERENCES guia_remision_destinatario(id) ON DELETE CASCADE,
    inventario_id                   INTEGER REFERENCES inventario(id),
    codigo_interno                  VARCHAR(25) NOT NULL,
    descripcion                     VARCHAR(300) NOT NULL,
    cantidad                        NUMERIC(10,2) NOT NULL
);

INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('GUIA_REMISION','001','001','001',1) ON CONFLICT (tipo) DO NOTHING;

ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS estado_gr VARCHAR(20) NOT NULL DEFAULT 'NINGUNA';

CREATE INDEX IF NOT EXISTS idx_guia_remision_clave ON guia_remision_registro(clave_acceso);
CREATE INDEX IF NOT EXISTS idx_guia_remision_estado ON guia_remision_registro(estado_sri);
CREATE INDEX IF NOT EXISTS idx_guia_destinatario_guia ON guia_remision_destinatario(guia_remision_id);
CREATE INDEX IF NOT EXISTS idx_guia_detalle_dest ON guia_remision_detalle(guia_remision_destinatario_id);
