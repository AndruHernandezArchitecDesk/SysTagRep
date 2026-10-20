# Fase 5 — Análisis empresarial

Spec: §7 (análisis con autorización, periodos/datos/supuestos/limitaciones,
sin acceso irrestricto a la BD) y plan de fases §16 Fase 5.

## Qué se implementó

### Datos autorizados, limitados (§7.3)
- Nuevo `service/AnalisisEmpresarialService` construye **agregados locales**:
  el modelo **no consulta la BD**; solo recibe este resumen:
  - **ROTACION**: unidades vendidas por producto en el periodo (desde
    `HistorialProductoDAO`) + *capital inmovilizado* (stock sin ventas en el periodo).
  - **AGOTAMIENTO**: `listarStockBajo(5)` + `listarStockNegativo()`.
  - **MARGENES**: % = (precioVenta − costoSinIVA) / precioVenta; menores y mayores,
    con conteo de productos con costo registrado.
  - **TENDENCIAS**: ventas por día (`DashboardDAO.ventasPorDia`), compras por día,
    total del periodo.
  - **GENERAL**: tendencias + agotamiento + márgenes.
- Todo agregado incluye `PERIODO: últimos N días (hasta fecha)`.

### Análisis con IA (§7.1/§7.2)
- Prompt obligatorio a indicar: **Periodo, Datos utilizados, Supuestos,
  Limitaciones e incertidumbre**; prohíbe garantizar ganancias y presentar
  predicciones como hechos; prohíbe inventar cifras fuera de los datos.
- Gates Fase 1/3 (licencia con Vendexi AI + capacidad); tokens medidos
  (módulo `analisis`). Modelos: `gemini-3.1-flash-lite` → `gemini-flash-latest`.
- Si la IA falla, el panel muestra igual los **datos locales** (ya visibles).

### Panel UI
- Vista `view/AnalisisIaView.fxml` + `AnalisisIaController`: combo de tipo
  (General/Rotación/Agotamiento/Márgenes/Tendencias), periodo (7–365 días),
  botón **Analizar**, área de resultado y área de **"Datos autorizados enviados"**.
- Menú lateral: **Análisis IA** (`MainController.irAnalisisIa`, permiso
  `REPORTE_VER`); en `CatalogoVistas` para accesos directos.
- Hilos daemon: la UI no se bloquea.

## Verificación
- `AnalisisEmpresarialServiceTest` (6 tests, DAOs falsos por Proxy, sin red/BD):
  agotamiento, márgenes (%), rotación + capital inmovilizado, tendencias/total,
  prompt §7, `resumenCorto`.
- Suite completa: `./mvnw test` → 0 fallos.

## Limitaciones / futuro
- Rotación usa el historial interno de ventas; si no hay historial en el periodo
  se informa (no se infiere demanda externa).
- No se modelan: caducidades, estacionalidad por marca ni comparativas interanuales.
- Escenarios "what-if" (§7) quedan para iteraciones futuras con datos de compra.
