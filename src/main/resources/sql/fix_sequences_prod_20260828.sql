-- =============================================================
-- fix_sequences_prod_20260828.sql
-- Corrige secuencias SERIAL desincronizadas tras pg_restore
-- en producción (dbTag). Cubre error reportado:
--   PSQLException: duplicate key violates unique constraint "marca_pkey"
--   Detail: Key (id)=(16) already exists  en MarcaDAO.java:18
-- Causa: dump/restore con INSERTs explícitos de id sin actualizar
--        la secuencia asociada (marca_id_seq, etc.)
-- Estrategia: explícita por tabla (recomendada) + fallback genérico
-- Idempotente y seguro: usa pg_get_serial_sequence + COALESCE
-- =============================================================

-- -------------------------------------------------------------
-- 0) Diagnóstico previo (opcional, solo lectura)
--    Ejecutar antes del fix para dejar evidencia en logs
-- -------------------------------------------------------------
-- SELECT 'marca' AS tabla, pg_get_serial_sequence('marca','id') AS seq,
--        (SELECT last_value FROM marca_id_seq) AS last_value,
--        (SELECT MAX(id) FROM marca) AS max_id;
-- SELECT table_name, column_default FROM information_schema.columns
--  WHERE table_schema='public' AND column_name='id' AND column_default LIKE 'nextval%';

-- -------------------------------------------------------------
-- 1) Fix explícito por tabla (RECOMENDADO - auditado)
--    Lista basada en src/main/resources/sql/reset_test_data.sql:21
--    + tablas SERIAL del proyecto (caja, alertas, etc.)
--    Si una tabla/secuencia no existe, se skipea con NOTICE.
-- -------------------------------------------------------------
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT 'marca' AS t UNION ALL
    SELECT 'grupo' UNION ALL
    SELECT 'codigo' UNION ALL
    SELECT 'cliente' UNION ALL
    SELECT 'proveedor' UNION ALL
    SELECT 'vendedor' UNION ALL
    SELECT 'inventario' UNION ALL
    SELECT 'perchero' UNION ALL
    SELECT 'ubicacion' UNION ALL
    SELECT 'ubicacion_percha' UNION ALL
    SELECT 'factura_registro' UNION ALL
    SELECT 'factura_detalle' UNION ALL
    SELECT 'nota_venta_registro' UNION ALL
    SELECT 'nota_venta_detalle' UNION ALL
    SELECT 'factura_proveedor' UNION ALL
    SELECT 'comprobante_temp' UNION ALL
    SELECT 'alertas' UNION ALL
    SELECT 'caja_sesion' UNION ALL
    SELECT 'caja_movimiento' UNION ALL
    SELECT 'historial_producto' UNION ALL
    SELECT 'cuentas_por_cobrar' UNION ALL
    SELECT 'cuentas_por_pagar' UNION ALL
    SELECT 'logs' UNION ALL
    SELECT 'usuarios' UNION ALL
    SELECT 'empresa' UNION ALL
    SELECT 'comprobantes_electronicos' UNION ALL
    SELECT 'xml_enviados'
  LOOP
    BEGIN
      -- setval(seq, MAX(id)+1, false) => próximo nextval = MAX+1
      -- COALESCE maneja tabla vacía => 1
      EXECUTE format('SELECT setval(pg_get_serial_sequence(%L,''id''), COALESCE((SELECT MAX(id) FROM %I),0)+1, false)', r.t, r.t);
      RAISE NOTICE 'Fix OK: % -> setval MAX+1', r.t;
    EXCEPTION WHEN undefined_table OR undefined_object OR undefined_column THEN
      RAISE NOTICE 'Skip % (tabla o secuencia no existe)', r.t;
    WHEN OTHERS THEN
      RAISE NOTICE 'Skip % (error %)', r.t, SQLERRM;
    END;
  END LOOP;
END $$;

-- -------------------------------------------------------------
-- 2) Verificación post-fix (ejecutar después del DO)
-- -------------------------------------------------------------
-- Ver estado de las 3 tablas críticas reportadas:
SELECT 'marca' AS tabla, last_value, is_called FROM marca_id_seq
UNION ALL SELECT 'grupo', last_value, is_called FROM grupo_id_seq
UNION ALL SELECT 'codigo', last_value, is_called FROM codigo_id_seq;

-- Ver MAX vs last_value para todas (si alguna sigue desincronizada, last_value <= MAX):
-- SELECT 'marca' AS tabla, (SELECT MAX(id) FROM marca) AS max_id, last_value, is_called FROM marca_id_seq
-- UNION ALL SELECT 'grupo', (SELECT MAX(id) FROM grupo), last_value, is_called FROM grupo_id_seq
-- UNION ALL SELECT 'codigo', (SELECT MAX(id) FROM codigo), last_value, is_called FROM codigo_id_seq
-- UNION ALL SELECT 'cliente', (SELECT MAX(id) FROM cliente), last_value, is_called FROM cliente_id_seq
-- UNION ALL SELECT 'proveedor', (SELECT MAX(id) FROM proveedor), last_value, is_called FROM proveedor_id_seq;

-- Test sin efecto colateral (opcional):
-- SELECT nextval(pg_get_serial_sequence('marca','id')) AS next_marca_preview;
-- -- Si quieres revertir el consumo del nextval de prueba:
-- SELECT setval(pg_get_serial_sequence('marca','id'), (SELECT MAX(id) FROM marca));

-- -------------------------------------------------------------
-- 3) Fallback genérico (COMENTADO - usar solo si aparecen tablas nuevas)
--    Recorre TODAS las secuencias del schema public asociadas a columnas id SERIAL/IDENTITY
--    Descomentar si prefieres fix automático sin mantener lista explícita.
-- -------------------------------------------------------------
-- DO $$
-- DECLARE rec RECORD;
-- BEGIN
--   FOR rec IN
--     SELECT c.relname AS seq, t.relname AS tabla, a.attname AS col
--     FROM pg_class c
--     JOIN pg_depend d ON d.objid = c.oid
--     JOIN pg_class t ON t.oid = d.refobjid
--     JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = d.refobjsubid
--     WHERE c.relkind = 'S' AND t.relkind = 'r' AND a.attname = 'id'
--       AND t.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname='public')
--   LOOP
--     BEGIN
--       EXECUTE format('SELECT setval(%L, COALESCE((SELECT MAX(%I) FROM %I),0)+1, false)',
--                      'public.'||rec.seq, rec.col, rec.tabla);
--       RAISE NOTICE 'Fallback fix: %.% -> %', rec.tabla, rec.col, rec.seq;
--     EXCEPTION WHEN OTHERS THEN
--       RAISE NOTICE 'Fallback skip %.%: %', rec.tabla, rec.col, SQLERRM;
--     END;
--   END LOOP;
-- END $$;
