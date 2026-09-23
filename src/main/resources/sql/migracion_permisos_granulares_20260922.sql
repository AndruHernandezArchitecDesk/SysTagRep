-- =============================================================
-- Migración permisos granulares (Fase 5)
-- 1 rol por usuario, matriz rol×permiso editable, tope descuento, auditoría
-- Ejecutar UNA VEZ como postgres/superuser en dbVendex (host 192.168.1.7)
-- También aplicado lazy por DatabaseConnection.ensurePermisosGranularesSchema()
-- Idempotente. Ver LINEAMIENTO_ROLES_PERMISOS.md
-- =============================================================
BEGIN;

CREATE TABLE IF NOT EXISTS rol (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(50) UNIQUE NOT NULL,
  descripcion VARCHAR(200),
  limite_descuento_pct NUMERIC(5,2) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS permiso (
  codigo VARCHAR(50) PRIMARY KEY,
  descripcion VARCHAR(200) NOT NULL,
  categoria VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS rol_permiso (
  rol_id INT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
  permiso_codigo VARCHAR(50) NOT NULL REFERENCES permiso(codigo) ON DELETE CASCADE,
  PRIMARY KEY (rol_id, permiso_codigo)
);

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS rol_id INT REFERENCES rol(id);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS limite_descuento_pct NUMERIC(5,2) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_usuarios_rol_id ON usuarios(rol_id);

CREATE TABLE IF NOT EXISTS auditoria_accion (
  id SERIAL PRIMARY KEY,
  usuario_id INT NOT NULL REFERENCES usuarios(id),
  permiso_codigo VARCHAR(50) NOT NULL,
  resultado VARCHAR(15) NOT NULL,
  detalle VARCHAR(300),
  creado_en TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria_accion(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_permiso ON auditoria_accion(permiso_codigo);
CREATE INDEX IF NOT EXISTS idx_auditoria_creado ON auditoria_accion(creado_en);

-- HSQLDB fallback
-- (se crea también en ensure* con IDENTITY)

-- Permiso: auditoría solo INSERT/SELECT para app_vendex
REVOKE ALL ON TABLE rol FROM app_vendex;
GRANT SELECT ON TABLE rol TO app_vendex;
GRANT SELECT ON TABLE permiso TO app_vendex;
REVOKE ALL ON TABLE rol_permiso FROM app_vendex;
GRANT SELECT ON TABLE rol_permiso TO app_vendex;
-- Solo ADMIN via app (USUARIO_GESTIONAR) debe UPDATE rol/rol_permiso; por ahora SELECT para login, UPDATE se otorga para gestión roles día 1
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE rol TO app_vendex;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE rol_permiso TO app_vendex;
GRANT SELECT, UPDATE ON TABLE usuarios TO app_vendex; -- rol_id, limite_descuento_pct
REVOKE ALL ON TABLE auditoria_accion FROM app_vendex;
GRANT SELECT, INSERT ON TABLE auditoria_accion TO app_vendex;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex;
-- DEFAULT PRIVILEGES futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO app_vendex;

-- Seed roles (VENDEDOR 5% tope, resto 0/null)
INSERT INTO rol (nombre, descripcion, limite_descuento_pct) VALUES
  ('ADMINISTRADOR','Acceso total', NULL),
  ('VENDEDOR','Ventas y facturación', 5.00),
  ('CAJERO','Caja y cobranza', 0.00),
  ('BODEGUERO','Inventario y bodega', 0.00)
ON CONFLICT (nombre) DO NOTHING;

-- Seed permisos (17 catálogo inicial)
INSERT INTO permiso (codigo, descripcion, categoria) VALUES
  ('FACTURA_EMITIR','Emitir una factura nueva','FACTURACION'),
  ('FACTURA_ANULAR','Iniciar NC por anulación total','FACTURACION'),
  ('NOTA_CREDITO_EMITIR','Emitir NC (devolución/descuento)','FACTURACION'),
  ('NOTA_DEBITO_EMITIR','Emitir ND','FACTURACION'),
  ('GUIA_REMISION_EMITIR','Emitir GR','FACTURACION'),
  ('RETENCION_EMITIR','Emitir comprobante de retención','FACTURACION'),
  ('PRECIO_EDITAR','Modificar precio de venta de un producto','INVENTARIO'),
  ('INVENTARIO_AJUSTAR','Ajuste manual de stock fuera de venta/compra','INVENTARIO'),
  ('DESCUENTO_APLICAR','Aplicar descuentos sobre precio de lista al facturar','FACTURACION'),
  ('CAJA_ABRIR','Abrir sesión de caja','CAJA'),
  ('CAJA_CERRAR','Cerrar sesión de caja','CAJA'),
  ('CAJA_VER_HISTORICO','Ver cierres de caja de otros usuarios/días anteriores','CAJA'),
  ('USUARIO_GESTIONAR','Crear/editar/bloquear usuarios y asignar roles','USUARIOS'),
  ('CONFIGURACION_EMAIL_EDITAR','Editar la cuenta de correo saliente','CONFIGURACION'),
  ('CONFIGURACION_BD_VER','Ver/editar configuración de conexión a BD','CONFIGURACION'),
  ('REPORTE_VER','Ver dashboard y reportes generales','REPORTES'),
  ('REPORTE_VER_FINANCIERO','Ver reportes con montos consolidados (ventas totales, márgenes)','REPORTES')
ON CONFLICT (codigo) DO NOTHING;

-- Matriz inicial (lineamiento §3)
-- ADMINISTRADOR: todo
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r CROSS JOIN permiso p WHERE r.nombre='ADMINISTRADOR'
ON CONFLICT DO NOTHING;

-- VENDEDOR: FACTURA_EMITIR, GUIA, DESCUENTO, REPORTE_VER (+ CAJA? según matriz, no)
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('FACTURA_EMITIR'),('GUIA_REMISION_EMITIR'),('DESCUENTO_APLICAR'),('REPORTE_VER')) AS p(codigo)
JOIN permiso pp ON pp.codigo=p.codigo
JOIN rol rr ON rr.nombre='VENDEDOR' AND rr.id=r.id
ON CONFLICT DO NOTHING;

-- CAJERO: FACTURA_EMITIR, NOTA_CREDITO_EMITIR, CAJA_ABRIR, CAJA_CERRAR, REPORTE_VER
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('FACTURA_EMITIR'),('NOTA_CREDITO_EMITIR'),('CAJA_ABRIR'),('CAJA_CERRAR'),('REPORTE_VER')) AS p(codigo)
JOIN permiso pp ON pp.codigo=p.codigo
JOIN rol rr ON rr.nombre='CAJERO' AND rr.id=r.id
ON CONFLICT DO NOTHING;

-- BODEGUERO: GUIA, INVENTARIO_AJUSTAR
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('GUIA_REMISION_EMITIR'),('INVENTARIO_AJUSTAR')) AS p(codigo)
JOIN permiso pp ON pp.codigo=p.codigo
JOIN rol rr ON rr.nombre='BODEGUERO' AND rr.id=r.id
ON CONFLICT DO NOTHING;

-- Backfill usuarios existentes: mapear rol string → rol_id
UPDATE usuarios SET rol_id = (SELECT id FROM rol WHERE rol.nombre = usuarios.rol) WHERE rol_id IS NULL AND rol IS NOT NULL;
-- Si no encaja, asignar ADMINISTRADOR temporal
UPDATE usuarios SET rol_id = (SELECT id FROM rol WHERE nombre='ADMINISTRADOR') WHERE rol_id IS NULL;

COMMIT;

-- Verificación:
-- SELECT r.nombre, p.codigo FROM rol r JOIN rol_permiso rp ON rp.rol_id=r.id JOIN permiso p ON p.codigo=rp.permiso_codigo ORDER BY r.nombre, p.codigo;
-- SELECT username, rol, r.nombre as rol_nuevo, limite_descuento_pct FROM usuarios u LEFT JOIN rol r ON r.id=u.rol_id;
