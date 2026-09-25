CREATE TABLE IF NOT EXISTS certificado_estado (
    id                    SERIAL PRIMARY KEY,
    ruta_p12              VARCHAR(500) NOT NULL,
    titular               VARCHAR(300),
    fecha_emision         DATE,
    fecha_expiracion      DATE NOT NULL,
    dias_restantes        INTEGER NOT NULL,
    nivel_severidad       VARCHAR(15) NOT NULL,
    ultima_verificacion   TIMESTAMP NOT NULL DEFAULT now(),
    ultimo_email_enviado  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_certificado_ruta ON certificado_estado(ruta_p12);
