-- V33__mensajes_sri_text.sql — Mensajes SRI a TEXT (eran VARCHAR(500))
-- Los detalles NO AUTORIZADO/DEVUELTA del SRI superan 500 chars y tumbaban
-- marcarResultado/actualizarEstado ("value too long"), dejando la cola atascada
-- y re-enviando correos cada ciclo. El codigo ademas trunca a 450 como cinturon.
-- Guardas por columna: backups viejos pueden no traer mensaje_sri en alguna tabla.

DO $$ DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT 'public.comprobante_pendiente_sri' AS t, 'ultimo_mensaje_sri' AS c
             UNION ALL SELECT 'public.comprobantes_electronicos', 'mensaje_sri'
             UNION ALL SELECT 'public.factura_registro', 'mensaje_sri'
             UNION ALL SELECT 'public.xml_enviados', 'mensaje_sri'
             UNION ALL SELECT 'public.nota_credito_registro', 'mensaje_sri'
             UNION ALL SELECT 'public.nota_debito_registro', 'mensaje_sri'
             UNION ALL SELECT 'public.guia_remision_registro', 'mensaje_sri'
             UNION ALL SELECT 'public.retencion_registro', 'mensaje_sri'
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public'
                   AND table_name = split_part(r.t, '.', 2)
                   AND column_name = r.c
                   AND data_type <> 'text') THEN
            EXECUTE format('ALTER TABLE %s ALTER COLUMN %I TYPE TEXT', r.t, r.c);
        END IF;
    END LOOP;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE comprobante_pendiente_sri TO app_vendex;
        GRANT SELECT, UPDATE ON TABLE comprobantes_electronicos TO app_vendex;
    END IF;
END $$;
