-- Fase 7: multiusuario y sucursales
-- 1) Alineación de columnas de usuarios usadas por el código pero nunca migradas
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email VARCHAR(120);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_login TIMESTAMP;

-- 2) Permisos granulares por usuario: amplían (otorgar=true) o restringen (otorgar=false) los del rol
CREATE TABLE IF NOT EXISTS usuario_permiso (
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    permiso_codigo VARCHAR(50) NOT NULL REFERENCES permiso(codigo) ON DELETE CASCADE,
    otorgar BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (usuario_id, permiso_codigo)
);

-- 3) Multi-sucursal por usuario (además de su sucursal principal en usuarios.sucursal_id)
CREATE TABLE IF NOT EXISTS usuario_sucursal (
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    sucursal_id INTEGER NOT NULL REFERENCES sucursal(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, sucursal_id)
);
CREATE INDEX IF NOT EXISTS idx_usuario_sucursal ON usuario_sucursal(sucursal_id);

-- 4) Trazabilidad: documentos de venta atribuidos a su emisor
ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS usuario_id INTEGER;
ALTER TABLE nota_venta_registro ADD COLUMN IF NOT EXISTS usuario_id INTEGER;
CREATE INDEX IF NOT EXISTS idx_factura_usuario ON factura_registro(usuario_id);
CREATE INDEX IF NOT EXISTS idx_nota_venta_usuario ON nota_venta_registro(usuario_id);

-- 5) Trazabilidad: auditoría por sucursal
ALTER TABLE auditoria_accion ADD COLUMN IF NOT EXISTS sucursal_id INTEGER;
