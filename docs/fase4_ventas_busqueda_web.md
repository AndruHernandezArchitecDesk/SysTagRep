# Fase 4 — Ventas y búsqueda web

Spec: §6 (venta cruzada justificada) y §8 (investigación web de repuestos).

## Qué se implementó

### Investigación web (§8)
- Nuevo servicio `service/InvestigacionWebService`:
  - Llama a Gemini con la herramienta **`google_search`** (grounding) — modelos
    `gemini-2.5-flash` (principal) y `gemini-flash-latest` (fallback).
  - Parsea `groundingMetadata.groundingChunks[].web.{uri,title}` y expone solo
    enlaces **http(s) reales** del proveedor (nada inventado, máx. 8 fuentes).
  - Compara con el inventario interno (resumen `código | nombre | stock` del
    `RepuestoChatbotDAO`): el stock mostrado es SOLO el del ERP.
  - Gates de Fase 1/3: licencia con Vendexi AI y capacidad disponible; consume
    tokens registrados en `vendexi_consumo` (módulo `web`).
- Prompt estricto §8: prioriza fabricantes/distribuidores, exige diferenciar
  `COMPAT_CONFIRMADA` vs `COINCIDENCIA_PROBABLE` vs `SIN_EVIDENCIA`, y pedir
  "no se encontró evidencia suficiente" cuando aplique.
- **UI (ChatWidgetController):**
  - Detección de intención: palabras clave ("investiga…", "busca en internet…",
    "google", "consulta web"…) **o** un mensaje que es un número de parte
    (5–40 chars, con guion/barra y dígito — así `FILTRO1` no roba la búsqueda local).
  - Respuesta renderizada con sección **"Fuentes verificables:"** de hipervínculos
    clickeables (`Desktop.browse`) con tooltip de la URL.
  - Badge "⏳ investigando web..." durante la consulta.

### Venta cruzada justificada (§6)
- `VendexiSugerenciaService.Sugerencia` ahora lleva **`evidencia`**:
  - `CONFIRMADA` (compatibilidad técnica confirmada),
  - `PROBABLE` (coincidencia razonable no confirmada),
  - `ASOCIACION` (asociación por ventas/uso habitual; default).
- El prompt de venta cruzada pide el campo `evidencia` en el JSON; el parser lo
  normaliza (los códigos inventados siguen descartándose).
- `VendexiSugerenciasPane` muestra un chip de color por tarjeta:
  verde = confirmada, ámbar = probable, gris = asociación de ventas.

### Limitaciones conscientes (sin inventar)
- **Imágenes:** el grounding de Gemini no devuelve URLs de imagen estructuradas;
  por ahora se muestran las fuentes (páginas con imágenes) como enlaces. No se
  muestran imágenes que no vengan del proveedor (regla §8: no inventar).
- La investigación requiere API key + conexión; sin ella el chat sigue en modo
  local (búsqueda en inventario).
- El recordatorio de alcance temático (§9) no forma parte de esta fase.

## Verificación
- `InvestigacionWebServiceTest` (8 tests): intención, parseo de grounding
  (duplicados/URI inválida), texto de respuesta, prompt §8, gate de consulta vacía.
- `VendexiSugerenciaServiceTest` (16): evidencia confirmada/probable/default,
  normalización, prompt con campo `evidencia`.
- Suite completa: `./mvnw test` → 0 fallos (Testcontainers omitido sin Docker).

## Pendiente / futuro
- Imágenes de productos desde fuentes confiables (si el proveedor expone URLs).
- Búsqueda web también desde Factura/NotaVenta (hoy: chat del asistente).
- Deduplicar fuentes por dominio (hoy por URI exacta).
