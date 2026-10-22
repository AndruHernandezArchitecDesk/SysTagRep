# Factura de Venta — precio unitario sin IVA (convención vigente)

**Fecha:** 22/10/2026

## Qué cambió

En **Factura de Venta** el precio unitario que se captura y se muestra es el
**precio sin IVA**. El IVA del 15 % se calcula automáticamente sobre la base menos el
descuento y se suma al total:

| Campo | Antes (precio con IVA incluido) | Ahora (precio sin IVA) |
| --- | --- | --- |
| 3 × 11.50 — precio unitario | 11.50 (ya traía IVA) | 11.50 (base) |
| Subtotal | 30.00 (11.50 ÷ 1.15 × 3) | **34.50** (3 × 11.50) |
| IVA 15 % | 4.50 (incluido en el precio) | **5.18** |
| Total | 34.50 | **39.68** |

Ejemplo de edición con doble clic: 11.50 → 12.00 ⇒ subtotal 36.00, IVA 5.40, total **41.40**.

Las fórmulas SRI **no cambian**: `Subtotal = Σ netos`, `IVA = 15 % × (base − adicional)`,
`Total = Subtotal − adicional + IVA`. Solo deja de dividirse el precio entre 1.15.

## Alcance

- **Sí cambia** (Factura de Venta):
  - UI: `FacturaController.sumaBasesSinIva`/`calcularTotales`, diálogo de producto
    temporal («P. unitario (sin IVA)») y columna `colInvPrecio` «Precio Unit. (sin IVA)».
  - Servicio/XML: `FacturaService.sumarBasesSinIva`, `armarDetalles`,
    `regenerarRide` y `ElectronicoUtil.precioNeto2`.
  - Base de datos ya estaba alineada: `FacturaDetalleDAOPostgres.insertarDetalle` siempre
    guardó `subtotal = precio × cantidad`, `iva = 15 %`, `total = subtotal + iva`; el
    cambio **alinea** cabecera y XML con la BD.
- **No cambia:**
  - **Proforma (Nota de Venta)**: sigue con su checkbox/manual de IVA y sus totales.
  - **Nota de Crédito**: sigue usando `ElectronicoUtil.precioSinIva2` (intacto).
  - Nota de Débito, retenciones, guías, ingresos de proveedor.
- `ElectronicoUtil.precioSinIva2` queda intacto y solo lo consume NC;
  el nuevo `precioNeto2` normaliza a 2 decimales **sin dividir** (null ⇒ 0.00).

## Compatibilidad con facturas antiguas

- **RIDE regenerado** (`FacturaService.regenerarRide`): si el subtotal guardado en BD no
  coincide con la derivación vigente (tolerancia 0.05) —facturas antiguas emitidas con
  precio con IVA incluido—, las líneas se rederivan con la convención vieja vía
  `armarDetallesConIva` para no desalinear resumen y líneas. Detección: método
  `FacturaService.usaConvencionAntigua(subtotalGuardado, detalles)`.
- **Nota de Crédito sobre factura antigua**: acreditará ~15 % menos de lo que se emitió
  (aceptado; NC lee los precios tal cual). Sobre facturas nuevas: correcta.
- **Tope de Consumidor Final ($50)**: ahora evalúa totales 15 % mayores que antes.

## Despliegue conjunto

`FacturaApiController.emitir` (ApiServer, `/api/facturas`) y `SyncService` offline usan
el mismo `FacturaService.guardarFactura`: **cliente y servidor deben correr el mismo
build**; si no, un cliente nuevo y un servidor viejo emitirían totales distintos.

## Verificación

```bash
./mvnw -Dtest=FxmlSmokeTest,FacturaServiceTest,FacturaServiceRideTest,FacturaDescuentoSriTest,FacturaTemporalTransaccionTest,FacturaTemporalApiTest,FacturaCalculatorTest,ElectronicoUtilTest,XmlSriBuilderTest,PdfElectronicoTest,NotaCreditoServiceTest test
```

**87 tests, 0 fallos, 0 errores.** Cobertura clave:

- Coherencia UI↔XML: `FacturaDescuentoSriTest.coherenciaUiXml_precioSinIva_3x1150_sub3450_iva518_tot3968`
  (mismos números en `sumarBasesSinIva`, `armarDetalles`, prevalidación SRI y `XmlSriBuilder`).
- Shim de facturas antiguas: `FacturaServiceTest.usaConvencionAntigua_*` y
  `FacturaServiceTest.armarDetallesConIva_facturaAntigua_divideEntre115`.
- `ElectronicoUtilTest.precioNeto2_precioYaSinIva_soloNormalizaASinDividir`.
- Totales de UI actualizados en `FxmlSmokeTest` (factura 39.68 / 41.40; proforma sin cambios).

El portable **no** se genera hasta que el usuario lo solicite. Sin commits.
