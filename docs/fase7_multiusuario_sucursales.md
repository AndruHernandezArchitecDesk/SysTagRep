# Fase 7 — Multiusuario y sucursales

Spec §16: «Añadir permisos por usuario, empresa y sucursal. Implementar sincronización
si corresponde. Probar concurrencia, trazabilidad y resolución de conflictos.»

## 1. Permisos por usuario y sucursal

### Nuevas tablas (`V39__fase7_multiusuario_sucursales.sql` + `ensureFase7Schema()`)
- **`usuario_permiso`** `(usuario_id, permiso_codigo, otorgar)` — permisos granulares
  por usuario que **amplían** (`otorgar=true`) o **restringen** (`otorgar=false`) los
  de su rol. Se aplican en `SesionActual.cargar()` sobre los permisos efectivos.
- **`usuario_sucursal`** `(usuario_id, sucursal_id)` — un usuario puede operar en
  varias sucursales. Si no tiene filas, opera en su sucursal principal (compatibilidad).
- Alineación de columnas de `usuarios` usadas por el código y nunca migradas
  (`activo`, `email`, `password_hash`, `fecha_creacion`, `ultimo_login`).

### Login validado (UI y API)
- `UsuarioSucursalDAO.sucursalValidaPara(usuarioId, preferida)`: la sucursal del
  usuario debe estar en sus sucursales permitidas; si no, usa la primera permitida.
- Al hacer login en la UI, `SucursalActual.set(...)` recibe la sucursal real del
  usuario (antes: `SucursalActual.set()` jamás se llamaba).

### Bug corregido: sucursal fija en 1
- `SucursalActual.getId()` (usado por `FacturaService`, `CajaService`, servicios de
  NC/ND/GR/retención) **siempre devolvía la sucursal 1 de Preferences** porque nada
  llamaba `set()`. Ahora deriva de `SesionActual.getSucursalId()` cuando hay sesión.

### API de administración (permiso `USUARIO_GESTIONAR`, también en GET)
- `GET/PUT /api/usuarios/{id}/permisos` — lectura/escritura de los granulares
  (valida códigos contra el catálogo `permiso`).
- `GET/PUT /api/usuarios/{id}/sucursales` — asignación de sucursales (valida
  existencia y actividad).
- Toda actualización queda auditada en `auditoria_accion`.

> **Empresa**: fuera de alcance — `empresa` es *single-tenant* (1 fila por
> instalación, sin `empresa_id` en usuarios/sucursales). Multi-empresa requiere
> rediseño de esquema y está documentado como futuro.

## 2. Sincronización

- **5 endpoints faltantes creados** (el `SyncService` los llamaba y recibía 404):
  - `POST /api/nota-debito` — delega en `NotaDebitoService.emitirNotaDebito` +
    provisional + cola SRI (mismo flujo que el controller online).
  - `POST /api/inventario/eliminar` — `INVENTARIO_AJUSTAR` (también en `permisoRequerido`).
  - `POST /api/ingreso-mercaderia` — reproducción del guardado online
    (productos→inventario, líneas `factura_proveedor`, crédito proveedor TAG Crédito,
    edición única, rechazo de factura duplicada 409).
  - `POST /api/cuentas-por-cobrar/adelanto`, `POST /api/cuentas-por-pagar/adelanto`.
  → Controller: `com.vendex.controller.api.OfflineSyncApiController`.
- **`RestClient.postStringRespuesta`**: envía `Idempotency-Key` y **reintenta una
  vez ante 401** (token vencido) con el header renovado — antes `postString` no
  refrescaba y todo caía a CONFLICTO.
- **Clasificación por status HTTP** (antes: substring `"error"` en el cuerpo):
  - 2xx → ENVIADA; 4xx → CONFLICTO (terminal, resoluble desde la UI);
  - 5xx/red → **sigue PENDIENTE** con intento registrado (hasta 5) → FALLIDA.
  Antes una caída de red de 30 s mataba toda la cola en CONFLICTO.

### Idempotencia server-side (`IdempotencyStore` + `RespuestaIdempotente`)
- El cliente ya enviaba `Idempotency-Key` estable (`tipo-id-creadoEn`); el server la
  ignoraba → un timeout tras el commit **duplicaba facturas**. Ahora: POST con clave
  ya procesada (2xx, TTL 10 min, máx 1000) devuelve la respuesta cacheada sin
  re-ejecutar (replay), registrado en el filtro before/after de `ApiServer`.

## 3. Concurrencia

- **`LocalOperationQueue`**: lock **global** (`LOCK` estático) — antes `synchronized`
  por instancia con archivo compartido: varios controllers encolaban a la vez y se
  pisaban el JSON (operaciones perdidas).
- **Tests multi-hilo**: 8 hilos × 25 encolas = 200 operaciones sin pérdidas ni IDs
  duplicados (`ColaOfflineConcurrenciaTest`); acceso concurrente a `IdempotencyStore`.
- Nota: `descontarStock` permite stock negativo **a propósito** (venta sobre stock
  pendiente de ingreso, decisión de negocio documentada en el DAO); la transferencia
  entre sucursales sí valida stock en Java dentro de transacción.

## 4. Trazabilidad

- **`factura_registro.usuario_id`** y **`nota_venta_registro.usuario_id`**: los dos
  documentos principales ahora se atribuyen a su emisor (sesión activa al INSERT).
- **`auditoria_accion.sucursal_id`**: cada evento de auditoría registra la sucursal
  de la sesión.
- Las actualizaciones de permisos/sucursales por API quedan auditadas con detalle.

## 5. Resolución de conflictos

- `LocalOperationQueue.listarTodas()/reencolar(id)/descartar(id)` — CONFLICTO y
  FALLIDA ya no son estados muertos.
- **UI `ColaOfflineView`**: muestra todos los estados con contadores
  (Pendientes/En conflicto/Fallidas) + botones **«Reintentar seleccionada»** y
  **«Descartar seleccionada»** (con confirmación).

## Verificación
- Tests nuevos: `SesionActualPermisosTest` (5), `UsuarioSucursalResolverTest` (3),
  `ColaOfflineResolucionTest` (5), `ColaOfflineConcurrenciaTest` (1),
  `IdempotencyStoreTest` (5), `SyncServiceIntegrationTest` (+4: fallo conexión,
  5xx, agotar intentos, Idempotency-Key) → **25 tests de fase**.
- Suite completa: `./mvnw test` → **324 tests, 0 fallos**.

## Pendientes futuros
- UI JavaFX de permisos granulares y multi-sucursal por usuario (hoy vía API;
  `UsuariosView` carece de combo de sucursal).
- Multi-empresa (`empresa_id` en sucursal/usuarios).
- `usuario_id`/`sucursal_id` en los payloads offline (atribución exacta del replay;
  hoy se audita con el usuario del token de replay).
- Bloqueo `FOR UPDATE` + guarda `AND cantidad >= ?` en transferencias (hoy la
  validación de stock es en Java dentro de transacción).
- Persistencia local de comprobantes offline (documentado como pendiente desde Fase 3).
