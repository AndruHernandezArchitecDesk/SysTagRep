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

## Verificación
- `ApiAuthServiceTest` (9): roundtrip de claims, token alterado/expirado/vacío,
  revocación por jti, Bearer, paths públicos, matriz de permisos.
- `SesionActualAislamientoTest` (5): no compartido entre hilos, logout API no
  borra sesión global de UI, sucursal/punto por hilo.
- Suite completa: `./mvnw test` → **248 tests, 0 fallos**.

## Pendiente (6b)
- Administración **central de licencias** (endpoint oficial de emisión/revocación).
- **Respaldos**, recuperación y pruebas de fallos de conexión (spec §16).
- TLS/terminación HTTPS (hoy: red de confianza/localhost; el bind por defecto es local).
- Rate limiting / bloqueo por IP en login.
