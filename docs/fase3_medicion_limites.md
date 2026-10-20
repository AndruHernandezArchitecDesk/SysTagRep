# Fase 3 — Medición y límites de Vendexi AI

Spec: §10 (medición de tokens, capacidad, periodos, costos, reset administrativo) y
§12 (sesiones de interacción).

## Qué se implementó

### Medición (§10.1)
- Cada llamada al proveedor captura `usageMetadata` del Gemini API
  (`promptTokenCount` + `candidatesTokenCount`) y la registra mejor-esfuerzo:
  - `GeminiChatbotService.llamarGeminiConModelo` → módulo `chatbot`, operación `chat`.
  - `VendexiSugerenciaService.llamarGemini` → módulo `contextual`, operación `venta_cruzada`.
- Un fallo de medición **nunca** rompe la llamada ni el ERP (try/catch best-effort en
  DAO y servicio).

### Capacidad por licencia (§10.2)
- **Decisión de diseño:** en la licencia v2, `capacidadIA` se interpreta como
  **tokens totales (entrada+salida) por periodo**; `0` = sin límite definido (no se corta).
  Los minutos se registran solo como estadística (sesiones) y no limitan.
- Gate en `GeminiChatbotService.preguntar` y `VendexiSugerenciaService.sugerenciasComplementarias`:
  si la capacidad está agotada **no se llama al proveedor**; el ERP local sigue
  disponible (búsqueda local / datos sin IA).
- Mensaje según spec §10.2: ver `VendexiConsumoService.mensajeCapacidadAgotada()`.
- Compatibilidad: licencias v1 → capacidad 0 → nunca bloquean.

### Costos (§10.4)
- Costo estimado por operación = tokens × precio/1M (configurable):
  `~/.vendex/vendexi.properties` con `costo.usd.por.1m.tokens.entrada|salida`
  (defaults estimados, revisables — **no** son la factura real del proveedor).
- Fórmula y acumulado por periodo en `VendexiConfig` / `VendexiConsumoService`.

### Periodos (§10.5) y reset
- Tablas (migración `V36__vendexi_consumo.sql` + `DatabaseConnection.ensureVendexiSchema()`):
  `vendexi_consumo`, `vendexi_periodo` (fila fija id=1), `vendexi_sesion`.
- Renovación del periodo solo desde el panel **Consumo de IA**, con permiso
  `USUARIO_GESTIONAR` y confirmación (acción administrativa).

### Sesiones (§12)
- Al abrir/cerrar el chat (`ChatWidgetController.onToggleChat`) se registra
  `vendexi_sesion` con duración (estadística de interacción, no es un límite).

### Panel administrativo
- Vista `view/ConsumoIaView.fxml` + `ConsumoIaController`: capacidad, consumido,
  restante, costo acumulado, inicio del periodo, precios de referencia y últimas
  operaciones; botones **Actualizar** y **Renovar periodo**.
- Menú lateral: botón *Consumo de IA* (`MainController.irConsumoIa`,
  permiso `USUARIO_GESTIONAR`); también en `CatalogoVistas` para accesos directos.

## Decisiones pendientes para el usuario
1. **Precios por defecto de `VendexiConfig`** son una estimación; definir los reales
  (o aceptar que es solo informativo hasta la facturación central — Fase 6).
2. Si la capacidad debe contarse en **minutos** además de tokens (spec menciona
  ambos): hoy solo los tokens limitan; los minutos se guardan en sesiones.
3. Modalidad CENTRAL de la licencia v2 aún no cambia el destino de las llamadas
  (llega con Fase 6); `modalidad` se parsea pero no se consume.

## Verificación
- `./mvnw compile` OK.
- Tests: `VendexiConsumoServiceTest` (9 tests, DAO en memoria) — medición,
  capacidad 0/parcial/agotada, renovación de periodo, límite de listado, sesiones.
- Suite completa: `./mvnw test` → **216 tests, 0 fallos, 24 skipped**
  (Testcontainers omitido sin Docker).
