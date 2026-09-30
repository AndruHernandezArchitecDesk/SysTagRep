# Fase 2 — Backend API REST + clientes delgados

> Objetivo: centralizar lógica, SRI, firma, permisos y secretos en un servidor backend.
> Clientes JavaFX consumen API REST en vez de JDBC directo.
> Ya preparado por Fase 0 y Fase 1.

## Estado actual

### Backend API (Javalin)
- **Puerto**: 7070
- **Habilitación**: `-Dapi.enabled=true -Dapi.port=7070` o env `API_ENABLED=true` / `API_PORT=7070`
- **Arranque**: `ApiServer.startIfEnabled()` desde `MainApp`
- **CORS**: cualquier host (`anyHost()`) — ajustar en producción

### Endpoints implementados

#### Auth
- `POST /api/auth/login` — autenticación, retorna usuario + sucursal + permisos
- `POST /api/auth/logout` — cierra sesión
- `GET /api/auth/me` — usuario actual

#### Catálogos
- `GET /api/sucursales` — listar sucursales
- `GET /api/sucursales/{id}` — obtener sucursal
- `POST /api/sucursales` — crear sucursal
- `GET /api/empresa` — listar empresas
- `GET /api/empresa/{id}` — obtener empresa
- `PUT /api/empresa/{id}` — actualizar empresa
- `GET /api/clientes` — listar clientes (con filtro `q`)
- `GET /api/clientes/{id}` — obtener cliente
- `POST /api/clientes` — crear cliente
- `PUT /api/clientes/{id}` — actualizar cliente
- `DELETE /api/clientes/{id}` — eliminar cliente
- `GET /api/vehiculos` — listar vehículos
- `GET /api/vehiculos/{id}` — obtener vehículo
- `POST /api/vehiculos` — crear vehículo
- `GET /api/vehiculos/vin/{vin}` — buscar por VIN
- `POST /api/vehiculos/import` — importar vehículos
- `GET /api/inventario` — listar inventario (soporta `sucursalId`, `q`, `page`, `size`, filtros vehículo)
- `GET /api/inventario/{id}` — obtener item de inventario
- `POST /api/inventario` — crear item
- `PUT /api/inventario/{id}` — actualizar item
- `DELETE /api/inventario/{id}` — eliminar item
- `POST /api/inventario/{id}/stock/descontar` — descontar stock
- `POST /api/inventario/{id}/stock/devolver` — devolver stock
- `GET /api/inventario/sucursal/{sucursalId}` — listar por sucursal
- `GET /api/inventario/codigo/{codigo}/sucursal/{sucursalId}` — obtener por código y sucursal

#### Operaciones
- `GET /api/facturas` — listar facturas (filtro sucursal)
- `GET /api/facturas/{id}` — obtener factura
- `GET /api/facturas/clave` — obtener factura por claveAcceso
- `POST /api/facturas` — emitir factura (body: `clienteId`, `items`, `formaPago`, `ambienteSri`, `descuentoPct`)
- `POST /api/facturas/registro` — insertar registro de factura
- `PUT /api/facturas/estado` — actualizar estado factura
- `GET /api/facturas/pendientes-sri` — listar pendientes SRI
- `GET /api/facturas/contar` — contar facturas
- `POST /api/facturas/{id}/detalles` — insertar detalles de factura
- `GET /api/facturas/{id}/detalles` — listar detalles de factura
- `GET /api/nota-venta` — listar proformas
- `GET /api/nota-venta/{id}` — obtener proforma
- `POST /api/nota-venta` — emitir proforma (body: `clienteId`, `items`, `formaPago`)
- `GET /api/nota-credito` — listar NC
- `GET /api/nota-credito/{id}` — obtener NC
- `GET /api/nota-credito/clave` — obtener NC por claveAcceso
- `POST /api/nota-credito` — insertar NC
- `PUT /api/nota-credito/estado` — actualizar estado NC
- `PUT /api/nota-credito/xml` — actualizar XML firmado NC
- `POST /api/nota-credito/emitir` — emitir NC sobre factura
- `GET /api/nota-credito/por-factura/{facturaRegistroId}` — listar NC por factura
- `GET /api/nota-credito/pendientes-sri` — listar NC pendientes SRI
- `GET /api/nota-credito/contar` — contar NC
- `GET /api/guia-remision` — listar GR
- `GET /api/guia-remision/{id}` — obtener GR
- `GET /api/guia-remision/clave` — obtener GR por claveAcceso
- `POST /api/guia-remision` — insertar GR
- `PUT /api/guia-remision/estado` — actualizar estado GR
- `GET /api/guia-remision/pendientes-sri` — listar GR pendientes SRI
- `GET /api/retencion` — listar retenciones
- `GET /api/retencion/{id}` — obtener retención
- `GET /api/retencion/clave` — obtener retención por claveAcceso
- `POST /api/retencion` — insertar retención
- `PUT /api/retencion/estado` — actualizar estado retención
- `GET /api/retencion/pendientes-sri` — listar retenciones pendientes SRI

#### Caja
- `GET /api/caja/abierta` — sesión abierta del usuario/sucursal
- `POST /api/caja/abrir` — abrir caja
- `POST /api/caja/cerrar` — cerrar caja
- `GET /api/caja/sesiones/{id}` — obtener sesión por ID
- `GET /api/caja/movimientos/{sesionId}` — movimientos de sesión
- `GET /api/caja/movimientos/{sesionId}/total` — total movimientos
- `GET /api/caja/resumen/{sesionId}` — resumen de sesión
- `GET /api/caja/sesiones` — listar sesiones por fecha

#### Transferencias
- `POST /api/transferencias` — transferir inventario entre sucursales
- `GET /api/transferencias` — listar transferencias

#### Dashboard
- `GET /api/dashboard/metricas` — métricas agregadas por sucursal

#### Comprobantes
- `POST /api/comprobantes` — insertar comprobante
- `PUT /api/comprobantes/estado` — actualizar estado comprobante
- `POST /api/comprobantes/envio` — guardar envío comprobante
- `GET /api/comprobantes/secuencial/{tipo}` — obtener secuencial

#### Cuenta por cobrar
- `POST /api/cuenta-por-cobrar` — insertar cuenta por cobrar
- `POST /api/cuenta-por-cobrar/{id}/pagar` — marcar pagado
- `GET /api/cuenta-por-cobrar/creditos-activos` — listar créditos activos
- `GET /api/cuenta-por-cobrar/cliente/{clienteId}` — listar por cliente
- `GET /api/cuenta-por-cobrar/detalles-venta/{notaVentaId}` — detalles de venta

#### Historial de producto
- `POST /api/historial-producto` — insertar registros de historial
- `GET /api/historial-producto` — listar por fecha

#### Secuencia de documento
- `GET /api/secuencia-documento/{puntoEmisionId}/{tipo}` — obtener secuencia
- `POST /api/secuencia-documento/{puntoEmisionId}/{tipo}/marcar-usado` — marcar secuencial usado

#### Logs
- `POST /api/logs` — guardar log

#### Health
- `GET /api/health` — estado del servicio

### Cliente JavaFX en modo remoto

Configurar:
```bash
-Dapi.enabled=true
-Dapi.remote=true
-Dapi.host=http://servidor-central:7070
```

`AppContext` elige automáticamente entre `DAOPostgres` (local) y `DAORest` (remoto).

#### Controladores migrados a modo remoto
- `FacturaController.guardar()` — rama remota que consume `POST /api/facturas`
- `NotaVentaController.guardar()` — rama remota que consume `POST /api/nota-venta`
- `NotaCreditoController.emitirNotaCredito()` — rama remota que consume `POST /api/nota-credito/emitir`

### DAORest implementados

| DAO | Estado |
|---|---|
| FacturaRegistroDAORest | ✅ Implementado (listar, obtener, insertar, actualizar estado, pendientes SRI, contar) |
| FacturaDetalleDAORest | ✅ Implementado (insertar, listar por factura) |
| CajaSesionDAORest | ✅ Implementado (abrir, obtener abierta, obtener por ID, listar por fecha, cerrar, total movimientos) |
| CajaMovimientoDAORest | ⚠️ Stub |
| SecuenciaDocumentoDAORest | ✅ Implementado (obtener, marcar usado) |
| NotaCreditoDAORest | ✅ Implementado (listar, obtener, insertar, actualizar estado, actualizar XML, pendientes SRI, contar, listar todas) |
| NotaCreditoDetalleDAORest | ⚠️ Stub |
| GuiaRemisionDAORest | ✅ Implementado (insertar, obtener, actualizar estado, pendientes SRI) |
| GuiaRemisionDetalleDAORest | ⚠️ Stub |
| GuiaRemisionDestinatarioDAORest | ⚠️ Stub |
| RetencionDAORest | ✅ Implementado (insertar, obtener, actualizar estado, pendientes SRI) |
| RetencionDetalleDAORest | ⚠️ Stub |
| RetencionDocumentoSustentoDAORest | ⚠️ Stub |
| InventarioDAORest | ⚠️ Parcial (listar, paginado, guardar, actualizar, eliminar, stock, sucursal, código) |
| ClienteDAORest | ✅ Implementado (listar, obtener, guardar, actualizar, eliminar) |
| EmpresaDAORest | ✅ Implementado (listar, obtener, actualizar) |
| ComprobanteDAORest | ⚠️ Parcial (insertar, actualizar estado, guardar envío, obtener secuencial) |
| CuentaPorCobrarDAORest | ⚠️ Parcial (insertar, marcar pagado, listar por cliente, créditos activos) |
| HistorialProductoDAORest | ⚠️ Parcial (insertar, listar, listar por fecha) |
| LogDAORest | ✅ Implementado (guardar) |
| DashboardDAORest | ⚠️ Stub |
| SucursalDAORest | ⚠️ Stub |
| PuntoEmisionDAORest | ⚠️ Stub |
| TransferenciaInventarioDAORest | ⚠️ Stub |
| VehiculoDAORest | ⚠️ Stub |
| InventarioVehiculoDAORest | ⚠️ Stub |
| ProveedorDAORest | ⚠️ Stub |
| GrupoDAORest | ⚠️ Stub |
| MarcaDAORest | ⚠️ Stub |
| UsuarioDAORest | ⚠️ Stub |
| RolDAORest | ⚠️ Stub |
| PermisoDAORest | ⚠️ Stub |
| PercheroDAORest | ⚠️ Stub |
| VendedorDAORest | ⚠️ Stub |
| CodigoDAORest | ⚠️ Stub |
| ConfiguracionEmailDAORest | ⚠️ Stub |
| NotaVentaRegistroDAORest | ⚠️ Parcial (obtener lista, insertar) |
| NotaVentaDetalleDAORest | ⚠️ Stub |
| NotaDebitoRegistroDAORest | ⚠️ Stub |
| NotaDebitoMotivoDAORest | ⚠️ Stub |
| ComprobantePendienteSriDAORest | ⚠️ Stub |
| ComprobanteTempDAORest | ⚠️ Stub |
| FacturaProveedorDAORest | ⚠️ Stub |
| CuentaPorPagarDAORest | ⚠️ Stub |
| VentaResumenDAORest | ⚠️ Stub |
| AuditoriaAccionDAORest | ⚠️ Stub |
| CertificadoEstadoDAORest | ⚠️ Stub |
| TablaRetencionDAORest | ⚠️ Stub |
| UbicacionPerchaDAORest | ⚠️ Stub |
| UbicacionDetalleDAORest | ⚠️ Stub |
| AlertaDAORest | ⚠️ Stub |
| LoginIntentoLogDAORest | ⚠️ Stub |
| RepuestoChatbotDAORest | ⚠️ Stub |

### Pendiente Fase 2

| Módulo | Estado |
|---|---|
| Backend API base (Javalin) | ✅ Creado |
| Endpoints catálogo | ✅ Creado |
| Endpoints factura/NC/ND/GR/Retención | ✅ CRUD + emitir/estado/pendientes |
| Endpoints caja | ✅ Creado |
| Endpoints inventario | ✅ Creado |
| Endpoints cliente/proveedor/usuarios | ✅ Cliente y Empresa; resto pendiente |
| DAORest implementados | ⚠️ Parcial — stubs para módulos no críticos |
| Cliente JavaFX modo remoto | ⚠️ FacturaController, NotaVentaController, NotaCreditoController migrados |
| Tests API | ❌ No hay |
| Documentación OpenAPI | ❌ No hay |

## Próximos pasos recomendados

1. Completar `DAORest` para endpoints usados en modo remoto (inventario, caja, comprobantes, cuenta por cobrar, historial).
2. Migrar `GuiaRemisionController`, `RetencionController`, `CajaController` a modo remoto.
3. Agregar tests de integración para endpoints críticos.
4. Documentar API con OpenAPI/Swagger.
5. Ajustar CORS y autenticación en producción.

## Migración a Fase 3

Una vez que Fase 2 esté en producción y se requiera modo offline, avanzar con Fase 3 (cola local, reserva de bloques de secuenciales, sincronización).
