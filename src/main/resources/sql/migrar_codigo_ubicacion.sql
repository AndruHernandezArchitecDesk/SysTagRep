-- ============================================================
-- MIGRACIÓN: Formato de código de ubicación con guiones
-- Convierte "A11" -> "A-1-1" (perchero-sección-lugar)
-- ============================================================

UPDATE ubicacion u
SET codigo_ubicacion = p.nombre_perchero || '-' || p.seccion || '-' ||
    substring(u.codigo_ubicacion, length(p.nombre_perchero || p.seccion) + 1)
FROM perchero p
WHERE p.id = u.id_perchero
  AND u.codigo_ubicacion NOT LIKE '%-%'
  AND substring(u.codigo_ubicacion, length(p.nombre_perchero || p.seccion) + 1) ~ '^[0-9]+$';
