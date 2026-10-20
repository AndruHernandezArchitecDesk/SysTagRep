# Fase 2 — Vendexi AI contextual (facturación y proformas)

**Fecha:** 19/10/2026 · Spec Vendexi §5, §16 Fase 2 · Anteriores: `docs/FASE0_AUDITORIA.md`, `docs/fase1_licencias.md`

## Objetivo

Asistencia dentro de los flujos de facturación electrónica y proformas: sugerencias con explicación, detección de datos incompletos, aceptación/descarte/copiado por el usuario y verificación de que el ERP sigue funcionando sin IA.

## Qué se implementó

### Panel contextual (componente reutilizable)

- **`controller/VendexiSugerenciasPane.java`** — componente compuesto `BorderPane` (FXML propio) insertado por código al inicio del VBox central de `FacturaView` y `NotaVentaView` (localizado subiendo la jerarquía desde `cmbCliente`; si falla, la vista carga igual sin panel).
- Tarjetas con: título, **motivo/explicación** (indica procedencia: verificación local o IA) y acciones **Aceptar / Descartar / Copiar**.
- **Aceptar** exige confirmación explícita (diálogo) antes de agregar 1 unidad al detalle — la IA nunca modifica el documento en silencio.
- **Copiar** lleva el código del producto al cuadro de búsqueda.
- Botones **Verificar documento** (verificaciones locales, sin IA) y **Limpiar**.

### Servicio contextual

- **`service/VendexiSugerenciaService.java`** (sin JavaFX; lógica pura testeable):
  - `verificarDocumento(Contexto)` — detección local de: cliente sin seleccionar, sin productos, precio en cero, cantidad > stock. Fuente declarada en el motivo («sin IA»).
  - `sugerenciasComplementarias(...)` — venta cruzada con Gemini: candidatos priorizados por mismo grupo/marca/overlap de tokens (máx. 40, con stock, excluye el agregado y los ya en el documento), prompt con reglas (no afirmar compatibilidad sin evidencia; usar códigos exactos; no inventar), respuesta JSON con máx. 3 sugerencias; **códigos inventados por la IA se descartan**.
  - Gate: `LicenseManager.licenciaIncluyeIA() && GeminiConfig.tieneApiKey()`; si no, devuelve vacío y el ERP no se ve afectado.
- **Trigger**: al agregar un producto en factura o proforma (hilo daemon; resultado en UI). En proformas se pasa el filtro de vehículo activo (marca/modelo/año) como contexto a la IA.

### Integraciones

| Archivo | Cambio |
|---|---|
| `controller/FacturaController.java` | montaje del panel, contexto, hook en `agregarProducto`, `agregarSugeridoVendexi` |
| `controller/NotaVentaController.java` | ídem + contexto de vehículo |
| `view/VendexiSugerencias.fxml` | UI del panel |
| `service/VendexiSugerenciaService.java` | nuevo |
| `controller/VendexiSugerenciasPane.java` | nuevo |

No se modificaron `FacturaView.fxml`/`NotaVentaView.fxml` (la inserción es por código para no tocar los FXML de una línea existentes).

## Comportamiento sin IA

- «Verificar documento» funciona siempre (verificaciones locales).
- El botón de venta cruzada no aparece/sugiere nada si la licencia no incluye IA o no hay key: cero llamadas de red, cero diálogos.
- La licencia v2 sin IA (Fase 1) bloquea el proveedor en el servicio; el ERP continúa operativo (criterio §17.7 y §10.2).

## Tests

`src/test/java/com/vendex/service/VendexiSugerenciaServiceTest.java` — 12 tests: verificaciones locales (4), selección de candidatos (2), parseo de IA incl. descarte de códigos inventados (4), prompt (1), gate sin IA (1). `FxmlSmokeTest` (11) carga `FacturaView` y `NotaVentaView` con el panel montado.

## Pendientes / siguientes

- Fase 3: medición de tokens de estas llamadas y capacidad por licencia (hoy la clave del cliente y sin tope).
- Ampliar a módulos posteriores (ventas, inventario) según spec §5.
- Estilo: `vendexi-panel` / `vendexi-aceptar` pueden integrarse mejor en los temas CSS existentes.
- Historial de sugerencias descartadas/aceptadas (auditoría) — futuro.
