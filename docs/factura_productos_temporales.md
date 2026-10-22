# Factura de Venta — productos temporales con stock negativo

**Fecha:** 22/10/2026

## Uso

Factura de Venta incorpora **Producto temporal** junto al buscador, con los campos
código, descripción, cantidad y precio unitario **sin IVA** (la factura suma el 15 %
al total; ver `docs/convencion_precio_factura.md`). La captura es
equivalente a la de Proforma; el destino del producto de Factura es inventario.

- Se exige un código nuevo en la sucursal de la sesión. La comprobación ignora
  espacios exteriores y diferencias de mayúsculas; también rechaza registros inactivos.
- El mismo código puede existir en otra sucursal. Los códigos temporales no pueden
  repetirse dentro de la factura ni coincidir con otra línea del documento.
- Cantidad entera positiva; precio positivo con hasta 2 decimales; código hasta
  50 caracteres y descripción hasta 300. Los errores se muestran dentro del diálogo.
- El detalle muestra `[TEMPORAL]` como indicador visual. Ese texto no se añade al
  código o descripción almacenados ni a los campos del XML.
- Añadir o quitar una línea del borrador no escribe inventario. Al guardar la factura
  se crea la referencia con stock cero y se descuenta la cantidad vendida una vez.
  **Ejemplo: cantidad 3 → stock final −3.**
- Se registra activo, con el precio de venta indicado. Costo, grupo, marca, ubicación
  y proveedor quedan sin completar, para su gestión posterior en inventario.

## Transacción

`FacturaService` valida y copia el borrador antes de calcular los totales. Dentro
de la transacción de emisión resuelve los temporales mediante
`InventarioDAO.crearTemporalFactura(Connection, ...)` e inserta detalles con IDs reales.
El descuento de stock y el historial usan esos mismos IDs.

El alta inicial, secuencial, factura, detalles, stock, historial, crédito y comprobante
comparten la conexión/transacción. Si falla la firma o el comprobante, todo se revierte;
el borrador no recibe IDs que podrían apuntar a filas eliminadas por el rollback.
La respuesta del servicio contiene los detalles registrados, independientes del borrador.

En PostgreSQL, el alta temporal toma un bloqueo transaccional de inventario
(`SHARE ROW EXCLUSIVE`) antes de comprobar el código y crear la referencia. Este
bloqueo se libera en commit/rollback e incluye las altas normales concurrentes.
El inventario existente conserva su funcionamiento por registros/lotes.

Las alertas de stock negativo se publican después del commit, porque su DAO usa
otra conexión; una factura fallida no deja alertas de una venta inexistente.

El catálogo de Factura se filtra por la sucursal de sesión y se refresca después
de registrar la venta, incluyendo las referencias nuevas con stock negativo.

## Remoto y offline

Los envíos inmediato y offline comparten `ProductoTemporalFactura.payload`:

```json
{
  "inventarioId": 0,
  "temporal": true,
  "codigo": "TMP-001",
  "descripcion": "REPUESTO NUEVO",
  "cantidad": 3,
  "precioUnitario": "11.50"
}
```

El servidor valida los datos y usa la misma emisión transaccional. Los productos
existentes se siguen enviando por su ID positivo; el servidor exige que pertenezcan
a la sucursal de la petición. Errores de datos o de código existente devuelven HTTP 400.

La API genera el código de registro a partir del número de comprobante cuando el
cliente no envía un código local y devuelve el ID de la factura registrada.

En offline no se adelanta un alta en inventario: el producto se crea al procesarse
la factura en el servidor. El payload conserva código, descripción e indicador temporal;
el mecanismo existente de sincronización conserva su clave de idempotencia.

## Edición de precio unitario (doble clic)

El precio unitario es la única columna editable de la tabla de detalle, tanto en
`FacturaController` (Factura de Venta) como en `NotaVentaController` (Proforma):

- `tblDetalle.setEditable(true)` y el resto de columnas con `setEditable(false)`,
  de modo que el doble clic solo abra edición en el precio.
- Celda `TextFieldTableCell` con `PrecioCeldaConverter`: acepta `,` o `.` decimal y
  devuelve `null` ante texto no numérico (no lanza `NumberFormatException`) mostrando
  siempre 2 decimales.
- `setOnEditCommit` valida con `ProductoTemporalFactura.precioValido` (misma regla que
  el guardado temporal y el XML: mayor a cero, hasta 2 decimales, máximo 10 cifras
  enteras). Si es válido aplica `setPrecioUnitario` (que recalcula `precioTotal`) y
  `calcularTotales()`; si no, no se modifica el modelo (**revertir silencioso**).
- `tblDetalle.refresh()` tras cada intento: `TableCell.commitEdit` ejecuta
  `updateItem(newValue)` después de disparar el evento, por lo que sin refrescar la
  celda quedaría mostrando el valor descartado.

### Facturación electrónica

La UI (`sumaBasesSinIva` → `precioNeto2`) y el XML (`sumarBasesSinIva`/`armarDetalles`)
derivan del mismo `precioUnitario` con la convención vigente de precio **sin IVA**
(`ElectronicoUtil.precioNeto2`/`netoLinea`; ver `docs/convencion_precio_factura.md`),
así que editar el precio y recalcular mantiene pantalla == XML. El tope del descuento
de cabecera se recalcula desde la nueva base y `copiarYValidar` vuelve a validar el
precio al guardar.

## Verificación

```bash
./mvnw -Dtest=FacturaTemporalTransaccionTest,FacturaTemporalApiTest,FxmlSmokeTest,FacturaServiceTest,FacturaCalculatorTest,FacturaDescuentoSriTest test
```

**59 tests, 0 fallos, 0 errores, 0 omitidos**, incluidas 13 pruebas nuevas
(8 transaccionales, 4 de API/contrato offline y 1 de interfaz).

Las pruebas nuevas verifican emisión mixta, dos temporales independientes, stock −3,
relación entre detalle e historial, rollback por firma/comprobante, reintento con el
mismo borrador, códigos existentes/inactivos y otra sucursal, validaciones, emisión
HTTP y serialización offline, además del montaje del botón y los totales de la UI.

La edición de precio unitario se cubre en `FxmlSmokeTest` con los tests
`factura_precioUnitarioEditableConDobleClicRecalculaTotales` y
`proforma_precioUnitarioEditableConDobleClicRecalculaTotales`, que disparan eventos
`TableColumn.CellEditEvent` reales: 11.50 → 12.00 ⇒ subtotal 36.00, IVA 5.40,
total 41.40 en factura; 10.00 → 12.50 ⇒ total 25.00 sin IVA y 28.75 con IVA en proforma;
y que precios inválidos (cero, negativos, más de 2 decimales, texto) no modifican el modelo.

Se utiliza SQL HSQLDB aislado y HTTP local. La firma se sustituye por una implementación
de prueba: no se usan certificados reales ni se envían comprobantes al SRI. El bloqueo
de tabla específico de PostgreSQL no se ejecuta en HSQLDB.

El portable no se actualiza hasta que el usuario lo solicite.
