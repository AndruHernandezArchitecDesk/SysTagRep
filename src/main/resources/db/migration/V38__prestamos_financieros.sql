-- =============================================================
-- Módulo de Préstamos Financieros (bancos / entidades)
-- Entidad financiera, préstamo con amortización francesa (tasa
-- directa por período), tabla de cuotas, pagos y metas de venta.
-- Idempotente. También aplicado lazy por DatabaseConnection.ensurePrestamoSchema()
-- =============================================================
BEGIN;

-- 1. Entidades financieras (banco, privado, otro)
CREATE TABLE IF NOT EXISTS entidad_financiera (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  tipo VARCHAR(20) DEFAULT 'BANCO',
  identificacion VARCHAR(20),
  telefono VARCHAR(30),
  contacto VARCHAR(80),
  notas TEXT,
  estado BOOLEAN DEFAULT TRUE,
  fecha_registro TIMESTAMP DEFAULT now()
);

-- 2. Préstamos (tasa directa por período: mensual o semanal)
CREATE TABLE IF NOT EXISTS prestamo (
  id SERIAL PRIMARY KEY,
  entidad_id INT NOT NULL REFERENCES entidad_financiera(id),
  monto_principal NUMERIC(12,2) NOT NULL,
  tasa_periodo_pct NUMERIC(6,3) NOT NULL,
  periodicidad CHAR(1) NOT NULL,
  numero_cuotas INT NOT NULL,
  cuota_monto NUMERIC(12,2) NOT NULL,
  total_a_pagar NUMERIC(12,2) NOT NULL,
  total_interes NUMERIC(12,2) NOT NULL,
  fecha_inicio DATE NOT NULL,
  estado VARCHAR(15) DEFAULT 'ACTIVO',
  meta_periodicidad VARCHAR(10),
  meta_monto NUMERIC(12,2),
  notas TEXT,
  fecha_registro TIMESTAMP DEFAULT now()
);

-- 3. Cuotas (tabla de amortización francesa)
CREATE TABLE IF NOT EXISTS prestamo_cuota (
  id SERIAL PRIMARY KEY,
  prestamo_id INT NOT NULL REFERENCES prestamo(id) ON DELETE CASCADE,
  numero INT NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  monto NUMERIC(12,2) NOT NULL,
  capital NUMERIC(12,2) NOT NULL,
  interes NUMERIC(12,2) NOT NULL,
  saldo_anterior NUMERIC(12,2) NOT NULL,
  saldo_restante NUMERIC(12,2) NOT NULL,
  monto_pagado NUMERIC(12,2) DEFAULT 0,
  interes_pagado NUMERIC(12,2) DEFAULT 0,
  estado VARCHAR(12) DEFAULT 'PENDIENTE',
  fecha_pago DATE,
  UNIQUE(prestamo_id, numero)
);

-- 4. Pagos / abonos registrados
CREATE TABLE IF NOT EXISTS prestamo_pago (
  id SERIAL PRIMARY KEY,
  prestamo_id INT NOT NULL REFERENCES prestamo(id),
  cuota_id INT REFERENCES prestamo_cuota(id),
  monto NUMERIC(12,2) NOT NULL,
  interes_parte NUMERIC(12,2) DEFAULT 0,
  capital_parte NUMERIC(12,2) DEFAULT 0,
  fuente VARCHAR(20) NOT NULL,
  referencia_id INT,
  usuario_id INT,
  notas TEXT,
  fecha TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_prestamo_estado ON prestamo(estado);
CREATE INDEX IF NOT EXISTS idx_prestamo_cuota_prestamo ON prestamo_cuota(prestamo_id, estado, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_prestamo_pago_prestamo ON prestamo_pago(prestamo_id);

-- 5. Permisos granulares
INSERT INTO permiso (codigo, descripcion, categoria) VALUES
  ('PRESTAMO_VER','Ver préstamos, cuotas y estados','PRESTAMOS'),
  ('PRESTAMO_GESTIONAR','Crear y administrar préstamos y entidades financieras','PRESTAMOS'),
  ('PRESTAMO_ABONAR','Registrar abonos a cuotas de préstamos','PRESTAMOS')
ON CONFLICT (codigo) DO NOTHING;

-- ADMINISTRADOR: los 3
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('PRESTAMO_VER'),('PRESTAMO_GESTIONAR'),('PRESTAMO_ABONAR')) AS p(codigo)
JOIN permiso pp ON pp.codigo=p.codigo
JOIN rol rr ON rr.nombre='ADMINISTRADOR' AND rr.id=r.id
ON CONFLICT DO NOTHING;

-- CAJERO: VER + ABONAR (abona al cerrar caja)
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('PRESTAMO_VER'),('PRESTAMO_ABONAR')) AS p(codigo)
JOIN permiso pp ON pp.codigo=p.codigo
JOIN rol rr ON rr.nombre='CAJERO' AND rr.id=r.id
ON CONFLICT DO NOTHING;

-- 6. Grants mínimo privilegio
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE entidad_financiera TO app_vendex;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE prestamo TO app_vendex;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE prestamo_cuota TO app_vendex;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE prestamo_pago TO app_vendex;
GRANT USAGE, SELECT ON SEQUENCE entidad_financiera_id_seq TO app_vendex;
GRANT USAGE, SELECT ON SEQUENCE prestamo_id_seq TO app_vendex;
GRANT USAGE, SELECT ON SEQUENCE prestamo_cuota_id_seq TO app_vendex;
GRANT USAGE, SELECT ON SEQUENCE prestamo_pago_id_seq TO app_vendex;

COMMIT;
