# Fase 6 — Backend y modalidad centralizada (6a: API segura + auth + aislamiento)

Spec: §13.2 (backend seguro: autenticación, autorización, aislamiento) y §16 Fase 6.
Decisión del propietario: **mismo repo**, **Java + el mismo Postgres**, prioridad
primero API + auth + aislamiento (la administración central de licencias y
respaldos quedan en 6b).

## Qué se implementó

### Autenticación (`com.vendex.api.ApiAuthService`)
- **Token Bearer** firmado HMAC-SHA256 (`body64.firma`), TTL 12 h configurable
  (`-Dapi.token.ttl.minutos`), con `jti` **revocable** en logout (lista en memoria).
- Secreto: `VENDEX_API_HMAC_KEY` (env/sysprop) → store local `api.hmac.key`
  (keyring/`~/.vendex/secrets`) → semilla aleatoria persistida. Nunca en el binario.
- `POST /api/auth/login` emite el token; `Authorization: Bearer <token>` en el resto.
- **Anti fuerza bruta**: bloqueo 15 min tras 5 intentos (`bloqueado_hasta`,
  respuestas 423/401) reutilizando `UsuarioDAO`.

### Autorización (`ApiAuthService.permisoRequerido`)
- GET → solo sesión válida.
- Escrituras → permiso del módulo ERP: facturas/nota-venta → `FACTURA_EMITIR`,
  NC → `NOTA_CREDITO_EMITIR`, ND → `NOTA_DEBITO_EMITIR`, guías → `GUIA_REMISION_EMITIR`,
  retenciones → `RETENCION_EMITIR`, inventario/transferencias → `INVENTARIO_AJUSTAR`,
  caja → `CAJA_ABRIR`, empresa/sucursales → `USUARIO_GESTIONAR`.
- Denegaciones auditadas en `auditoria_accion` (DENEGADO vía API).
- Públicos: `GET /api/health`, `POST /api/auth/login`.

### Aislamiento
- **Sesión por-hilo** en `SesionActual`: `iniciarApi/cerrarApi` con `ThreadLocal`;
  cada petición HTTP (hilo Javalin) ve solo su usuario; la sesión global de la UI
  local JavaFX no se toca (login `iniciar` sigue siendo global). Filtro `before`
  limpia herencia y `after` libera en cada petición.
- **Bind**: por defecto `localhost` (`-Dapi.bind=0.0.0.0` para exponer).
- **CORS deshabilitado por defecto** (`-Dapi.cors=true` para habilitar).
- Usuario debe existir y estar `activo` en cada petición (desactivación surte
  efecto inmediato, no depende del token).

### Cliente remoto (`RestClient`)
- Todas las llamadas envían `Authorization: Bearer` (token estático por proceso).
- `RestClient.login(host,user,pass)` + **auto-login** con credenciales de
  `api.user`/`api.pass` o `VENDEX_API_USER`/`VENDEX_API_PASS` al crear
  `AppContext` en modo remoto; reintento automático una vez ante 401 (token vencido).

### Endpoints de sesión
- `POST /api/auth/login` → `{token, tokenType, expiraEnMinutos, usuario, permisos…}`
- `POST /api/auth/logout` → revoca el `jti`
- `GET /api/auth/me` → usuario de la petición actual

## Fase 6b — licencias centrales, respaldos y resiliencia

### Administración central de licencias
- Tablas `licencia_emitida` (hash SHA-256 de la clave, nunca la clave en claro) y
  `licencia_revocada` (por machineCode, con motivo) — migración
  `V37__licencias_centrales.sql` + `DatabaseConnection.ensureLicenciasCentralSchema()`.
- `LicenciaCentralService`:
  - `emitir(machineCode, LOCAL|CENTRAL, incluyeIA, roles[], capacidad, expiracion|null)`
    → genera clave v2 con el HMAC de la instancia central y registra la emisión.
  - `validarCentral(machineCode, clave)` → firma (LicenseManager) **y** revocación;
    `valida=false` si la firma es válida pero el machineCode está revocado.
  - `revocar(machineCode, motivo, usuarioId)` idempotente.
- Endpoints (admin = `USUARIO_GESTIONAR`, también en GET de listados):
  - `POST /api/licencias/emitir` → 201 `{clave, machineCode, modalidad, ...}`
  - `POST /api/licencias/revocar`
  - `GET|POST /api/licencias/validar?machineCode=&clave=` → solo sesión válida
    (clientes en modalidad CENTRAL autentican con su usuario de negocio)
  - `GET /api/licencias/emitidas`, `GET /api/licencias/revocadas`
- `LicenciaEmitida` (model) + `LicenciaCentralDAO`/`LicenciaCentralDAOPostgres`
  (registro en `AppContext.licenciaCentralDAO`).

### Respaldos vía API
- `BackupApiController`: `POST /api/respaldos` (ejecuta el `BackupService` existente:
  pg_dump custom + verify + cifrado GPG + offsite; 200 exito / 502 falla) y
  `GET /api/respaldos` (listado del directorio de respaldos). Permiso `USUARIO_GESTIONAR`.

### Pruebas de fallos de conexión (resiliencia)
- `RestClientResilienciaTest` (5, Javalin embebido sin BD): login ok/credenciales
  malas, servidor caído → excepción controlada, **token vencido (401) → re-login y
  reintento automático** (header Authorization actualizado), sin credenciales no
  se recupera. Corrección aplicada: `RestClient` ahora reenvía la petición con
  `Authorization` renovado (`setHeader`, rebuild por Java 11).

## Verificación
- `ApiAuthServiceTest` (9), `SesionActualAislamientoTest` (5),
  `LicenciaCentralServiceTest` (10, DAO falso en memoria),
  `RestClientResilienciaTest` (5).
- Suite completa: `./mvnw test` → **263 tests, 0 fallos**.

## Pendiente futuro (fuera de 6a/6b)
- TLS/terminación HTTPS (hoy: red de confianza/localhost; el bind por defecto es local).
- Rate limiting / bloqueo por IP en login.
- Consumo del endpoint `validar` desde el cliente (modalidad CENTRAL en runtime).
