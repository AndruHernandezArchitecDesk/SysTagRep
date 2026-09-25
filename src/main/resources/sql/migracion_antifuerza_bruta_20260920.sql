-- =============================================================
-- Migración anti fuerza bruta (Fase 4)
-- Ejecutar UNA VEZ como postgres/superuser en dbVendex (host vendex-db, IP varia por cliente)
-- También aplicado lazy por DatabaseConnection.ensureLoginBruteForceSchema()
-- Idempotente. Ver LINEAMIENTO_ANTIFUERZA_BRUTA.md
-- =============================================================
BEGIN;

-- 1. Columnas en usuarios (bloqueo por cuenta)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_intento_fallido TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_usuarios_bloqueado_hasta ON usuarios(bloqueado_hasta) WHERE bloqueado_hasta IS NOT NULL;

-- 2. Tabla de auditoría de intentos (solo INSERT desde app, ver permisos_bd.md)
CREATE TABLE IF NOT EXISTS login_intento_log (
  id            SERIAL PRIMARY KEY,
  usuario_input VARCHAR(150) NOT NULL,
  exitoso       BOOLEAN NOT NULL,
  equipo        VARCHAR(150),
  creado_en     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_login_log_usuario_input ON login_intento_log(usuario_input);
CREATE INDEX IF NOT EXISTS idx_login_log_creado_en ON login_intento_log(creado_en);

-- 3. Tabla genérica de configuración (para política configurable no hardcodeada)
CREATE TABLE IF NOT EXISTS configuracion (
  clave       VARCHAR(100) PRIMARY KEY,
  valor       TEXT NOT NULL,
  descripcion VARCHAR(300),
  actualizado_en TIMESTAMP NOT NULL DEFAULT now()
);
-- Valores por defecto de política de bloqueo (ver PoliticaBloqueo.java)
INSERT INTO configuracion (clave, valor, descripcion) VALUES
  ('bloqueo.intentos_permitidos', '5', 'Intentos fallidos antes de bloquear cuenta')
ON CONFLICT (clave) DO NOTHING;
INSERT INTO configuracion (clave, valor, descripcion) VALUES
  ('bloqueo.ventana_minutos', '15', 'Ventana en minutos para reseteo de contador sin nuevos fallos')
ON CONFLICT (clave) DO NOTHING;
INSERT INTO configuracion (clave, valor, descripcion) VALUES
  ('bloqueo.duracion_minutos', '15', 'Duración inicial de bloqueo en minutos (backoff exponencial duplica)')
ON CONFLICT (clave) DO NOTHING;
INSERT INTO configuracion (clave, valor, descripcion) VALUES
  ('bloqueo.max_horas', '24', 'Tope máximo de bloqueo en horas')
ON CONFLICT (clave) DO NOTHING;

-- 4. Permisos mínimo privilegio
REVOKE ALL ON TABLE login_intento_log FROM app_vendex;
GRANT SELECT, INSERT ON TABLE login_intento_log TO app_vendex;
GRANT USAGE, SELECT ON SEQUENCE login_intento_log_id_seq TO app_vendex;

REVOKE ALL ON TABLE configuracion FROM app_vendex;
GRANT SELECT ON TABLE configuracion TO app_vendex;
-- Solo admin (o migrador) debe UPDATE configuracion; app_vendex solo SELECT. Si se quiere permitir edición desde UI admin, cambiar a SELECT,UPDATE y restringir en app.

-- DEFAULT PRIVILEGES para futuras tablas (opcional, ya existe en migracion_minimo_privilegio)
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT ON TABLES TO app_vendex;

COMMIT;

-- Verificación:
-- SELECT intentos_fallidos, bloqueado_hasta FROM usuarios WHERE username='admin';
-- SELECT COUNT(*) FROM login_intento_log;
-- SELECT * FROM configuracion WHERE clave LIKE 'bloqueo.%';
-- Manual desbloqueo emergencia:
-- UPDATE usuarios SET intentos_fallidos=0, bloqueado_hasta=NULL, ultimo_intento_fallido=NULL WHERE username='admin';
