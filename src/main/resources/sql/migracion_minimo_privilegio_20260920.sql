-- =============================================================
-- Migración: De GRANT ALL implícito (postgres superuser) a mínimo privilegio app_vendex
-- Ejecutar UNA VEZ como postgres/superuser en dbVendex (host 192.168.1.7)
-- Pre-requisito: pg_dump de seguridad antes de REVOKE (ver docs/permisos_bd.md §1)
-- Idempotente: re-ejecutable sin duplicar
-- Tablas inventariadas: 42 (sql/*.sql + DatabaseConnection ensure*Schema + dao/*.java)
-- 31 SERIAL → 31 sequences *_id_seq requieren USAGE
-- Basado en LINEAMIENTO_GRANT_MINIMO_PRIVILEGIO.md
-- =============================================================
BEGIN;

-- 0. Rol app_vendex (crear si no existe)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_vendex') THEN
    -- PASSWORD reemplazar por la generada 24 chars con wizard (SecureRandom) y ALTER ROLE después
    CREATE ROLE app_vendex WITH LOGIN PASSWORD 'CambiarEnInstalacion_24!';
    RAISE NOTICE 'Rol app_vendex creado con password temporal - rotar con wizard en cada PC';
  ELSE
    RAISE NOTICE 'Rol app_vendex ya existe - no se recrea';
  END IF;
END $$;

-- Conexión y uso de schema (revocar CREATE para que ensure*Schema no corra como app_vendex)
GRANT CONNECT ON DATABASE "dbVendex" TO app_vendex;
GRANT USAGE ON SCHEMA public TO app_vendex;
REVOKE CREATE ON SCHEMA public FROM app_vendex;

-- 1. Punto de partida limpio: revocar privilegios previos (si existían Grants explícitos)
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM app_vendex;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM app_vendex;

-- 2. Grants por categoría (ver docs/permisos_bd.md Tabla §2)
-- 2a. Catálogos con ABM completo (requieren DELETE)
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
  marca, grupo, codigo, cliente, proveedor, vendedor,
  perchero, ubicacion, ubicacion_percha
TO app_vendex;

-- 2b. Catálogo solo lectura
GRANT SELECT ON TABLE tabla_retencion TO app_vendex;

-- 2c. Transaccionales base (SELECT/INSERT/UPDATE)
GRANT SELECT, INSERT, UPDATE ON TABLE
  factura_registro, factura_detalle,
  nota_venta_registro, nota_venta_detalle,
  nota_credito_registro, nota_credito_detalle,
  nota_debito_registro, nota_debito_motivo,
  guia_remision_registro, guia_remision_destinatario, guia_remision_detalle,
  retencion_registro, retencion_documento_sustento, retencion_detalle,
  comprobantes_electronicos
TO app_vendex;

-- 2d. Transaccionales con DELETE requerido (inventario, comprobantes temp, caja, cuentas, alertas)
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
  inventario, factura_proveedor, comprobante_temp,
  cuentas_por_cobrar, cuentas_por_pagar,
  caja_sesion, alertas
TO app_vendex;

-- 2e. Tablas con solo INSERT (+ SELECT donde DAO lo usa)
GRANT SELECT, INSERT ON TABLE caja_movimiento, historial_producto, logs, xml_enviados TO app_vendex;
-- comprobantes_electronicos ya está en 2c con UPDATE (estado_sri)

-- 2f. Secuencias / numeración (tabla, no SEQUENCE)
GRANT SELECT, INSERT, UPDATE ON TABLE secuencia_documento, secuenciales TO app_vendex;

-- 2g. Sensibles
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE usuarios TO app_vendex;
GRANT SELECT, UPDATE ON TABLE empresa TO app_vendex;
GRANT SELECT, INSERT, UPDATE ON TABLE configuracion_email, certificado_estado TO app_vendex;

-- 3. Sequences SERIAL (USAGE + SELECT para nextval)
-- Todas las SERIAL crean *_id_seq implícitamente; GRANT USAGE necesario para INSERT sin id
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex;

-- 4. Futuras tablas/sequences creadas por migrador (postgres) heredarán a app_vendex
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_vendex;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_vendex;
-- Para tabla_retencion futura (solo lectura) ajustar manualmente a SELECT después si se desea

-- 5. Nunca otorgar a app_vendex (explícito por documentación):
-- TRUNCATE (solo postgres, reset_test_data.sql), CREATE ON SCHEMA, DELETE en tablas de auditoría logs/xml_enviados/historial_producto

COMMIT;

-- =============================================================
-- Verificación post-migración (ejecutar como postgres):
-- SELECT grantee, table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee='app_vendex' ORDER BY table_name, privilege_type;
-- SELECT sequence_name, grantee, privilege_type FROM information_schema.role_usage_grants WHERE grantee='app_vendex' UNION SELECT sequence_name, grantee, privilege_type FROM information_schema.role_routine_grants WHERE grantee='app_vendex';
-- \z factura_registro  -- debe mostrar app_vendex=arwd/ o similar sin 'a' (TRUNCATE)
-- \du
-- Probar como app_vendex: psql -h 192.168.1.7 -U app_vendex -d dbVendex -c "INSERT INTO logs (controlador,metodo,mensaje) VALUES ('test','test','perm ok') RETURNING id;"
-- Debe fallar: psql -h 192.168.1.7 -U app_vendex -d dbVendex -c "TRUNCATE TABLE logs;"  -- permission denied esperado
-- =============================================================
-- Siguiente paso: en cada PC, wizard DbSetupWizard → cambiar usuario a app_vendex + password generada, Probar y Guardar (cifrado keyring)
-- Cambiar DbConfig.DEFAULT_USER a app_vendex en código (ya aplicado) y rotar password en Postgres: ALTER ROLE app_vendex WITH PASSWORD '...'; + ALTER ROLE postgres WITH PASSWORD '...' (rotar superuser también)
-- =============================================================
