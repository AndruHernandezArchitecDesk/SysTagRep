-- V34__numero_autorizacion_49.sql — numero_autorizacion a VARCHAR(49)
-- Drift: BDs creadas con DDL antiguo tienen comprobantes_electronicos y
-- xml_enviados en VARCHAR(37); el numero real del SRI (49 digitos) tumbaba
-- actualizarEstado/guardarEnvio y la pantalla quedaba en PENDIENTE aunque el
-- SRI habia autorizado. Guardas por longitud actual para no tocar lo que ya esta bien.

DO $$ DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT 'public.comprobantes_electronicos' AS t, 'numero_autorizacion' AS c
             UNION ALL SELECT 'public.xml_enviados', 'numero_autorizacion'
             UNION ALL SELECT 'public.factura_registro', 'numero_autorizacion'
             UNION ALL SELECT 'public.nota_credito_registro', 'numero_autorizacion'
             UNION ALL SELECT 'public.nota_debito_registro', 'numero_autorizacion'
             UNION ALL SELECT 'public.guia_remision_registro', 'numero_autorizacion'
             UNION ALL SELECT 'public.retencion_registro', 'numero_autorizacion'
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public'
                   AND table_name = split_part(r.t, '.', 2)
                   AND column_name = r.c
                   AND character_maximum_length IS NOT NULL
                   AND character_maximum_length < 49) THEN
            EXECUTE format('ALTER TABLE %s ALTER COLUMN %I TYPE VARCHAR(49)', r.t, r.c);
        END IF;
    END LOOP;
END $$;
