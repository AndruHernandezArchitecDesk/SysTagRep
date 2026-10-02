# Contrato mínimo API para sincronización offline

Este documento describe los endpoints que el backend debe exponer para que `SyncService` pueda replayar las operaciones encoladas por `LocalOperationQueue`.

## Headers comunes
- `Content-Type: application/json`
- `Idempotency-Key: {tipo}-{id}-{creadoEn}` (generado por `SyncService`; el backend debe usarlo para evitar duplicados)

## Endpoints

| Tipo offline | Método | Endpoint | Payload mínimo esperado |
|---|---|---|---|
| `FACTURA` | POST | `/facturas/registro` | `{clienteId, formaPago, ambienteSri, descuentoPct, items}` |
| `NOTA_CREDITO` | POST | `/nota-credito` | `{facturaId, motivo, ambienteSri, ...}` |
| `NOTA_VENTA` | POST | `/nota-venta` | `{clienteId, items, formaPago, ...}` |
| `NOTA_DEBITO` | POST | `/nota-debito` | `{facturaId, formaPago, ambienteSri, motivos}` |
| `GUIA_REMISION` | POST | `/guia-remision` | `{destinatarios, transportista, fechas, ambienteSri}` |
| `RETENCION` | POST | `/retencion` | `{periodo, tipoIdentificacionSujeto, razonSocialSujeto, docs}` |
| `INVENTARIO` | POST | `/inventario` | `{descripcion, cantidad, precioVenta, costo_sin_iva, ...}` |
| `INVENTARIO_ELIMINAR` | POST/DELETE | `/inventario/eliminar` | `{inventarioId}` |
| `INGRESO_MERCADERIA` | POST | `/ingreso-mercaderia` | `{proveedorId, numeroFactura, productos, formaPago}` |
| `CAJA_ABRIR` | POST | `/caja/abrir` | `{montoInicial, observaciones, usuarioId}` |
| `CAJA_CERRAR` | POST | `/caja/cerrar` | `{sesionId, montoFisico, observaciones}` |
| `CUENTA_POR_COBRAR` | POST | `/cuentas-por-cobrar/adelanto` | `{cuentaPorCobrarId, adelanto, pagado}` |
| `CUENTA_POR_PAGAR` | POST | `/cuentas-por-pagar/adelanto` | `{cuentaPorPagarId, adelanto, pagado}` |

## Códigos esperados
- `2xx`: éxito → `SyncService` marca `ENVIADA`.
- `4xx`: conflicto → `SyncService` marca `CONFLICTO` y guarda el mensaje.
- `5xx`: reintentar → `SyncService` reintenta en el siguiente ciclo automático.

## Recomendaciones backend
- Idempotencia por cliente usando el header `Idempotency-Key`.
- Validación mínima y respuesta corta para reintentos rápidos.
- Si hay reglas SRI/firma electrónica, mantenerlas igual que en el flujo online.
