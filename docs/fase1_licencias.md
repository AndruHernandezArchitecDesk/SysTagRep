# Fase 1 — Licencias (evolución incremental)

**Fecha:** 19/10/2026 · Spec Vendexi §3, §16 Fase 1 · Auditoría base: `docs/FASE0_AUDITORIA.md`

## Objetivo

Pasar del licenciamiento binario (activada/no) a un modelo con **dimensiones** — modalidad (local/central), habilitación de Vendexi AI, roles de IA y capacidad — **sin romper las licencias ya emitidas**.

## Formato

### v1 (existente, se conserva)

`<hmac20hex>-<YYYYMMDD|00000000>` — HMAC-SHA256 sobre `machineCode|expiryStr`, presentado con guiones cada 5 caracteres.

- **Semántica v1 heredada (decisión de compatibilidad):** LOCAL + Vendexi AI habilitada + todos los roles + capacidad 0 (sin límite definido). Las instalaciones actuales no pierden el chatbot ni sufren restricciones nuevas.
- El secreto HMAC se resuelve: `VENDEX_LICENSE_HMAC_KEY` (env/sysprop) > keyring/`~/.vendex/secrets/license.hmac.key` > **migración**: si no existe, se siembra el valor legacy en el store local (no se invalida ninguna licencia vigente; el secreto queda fuera del binario en futuras instalaciones nuevas). Rotar el HMAC legacy rompería licencias emitidas — pendiente como migración deliberada futura.

### v2 (nuevo, generado por el CLI)

`<hmac20hex><YYYYMMDD|00000000><payloadBase64>` donde el payload (UTF-8, firmado dentro del HMAC) es:

```
V2|<LOCAL|CENTRAL>|<0|1>|<roles csv o ->|<capacidad long>
```

- `capacidad = 0` → sin límite definido (la medición y el tope llegan en Fase 3).
- Roles válidos: `OPERATIVO, FACTURACION, PROFORMAS, VENTAS, INVENTARIO, FINANCIERO, ASESOR, INVESTIGADOR` (spec §4). Desconocidos se ignoran; vacío/`-` con IA = todos.
- Cualquier alteración del payload invalida la clave (HMAC).
- `validateLicenseKey`/`getInfoDeClave` aceptan v1 y v2 indistintamente.

## Generación (CLI)

Sin flags → v1 (comportamiento histórico, 30 días por defecto, `vitalicia`, `<días>` o `<YYYYMMDD>`).
Con cualquier flag `--` → v2:

```bash
java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> vitalicia --ia --roles=FACTURACION,PROFORMAS --capacidad=50000
java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> 90 --central --no-ia
```

Flags: `--local` (default) | `--central` · `--ia` | `--no-ia` · `--roles=A,B` · `--capacidad=N`.

## API (`util/LicenseManager`)

- `generateLicenseKeyV2(machineCode, expiry, modalidad, incluyeIA, roles, capacidad)`
- `getInfoDeClave(machineCode, key)` → `LicenseInfo` (valida, version2, perpetua, vencimiento, modalidad, incluyeIA, rolesIA, capacidadIA)
- `getLicenseInfo()` → info de la licencia almacenada en la máquina
- `licenciaIncluyeIA()` · `licenciaPermiteRolIA(rol)`

**Gate de IA:** solo una **licencia v2 válida con `incluyeIA=false`** bloquea Vendexi AI. Sin licencia almacenada (modo remoto), claves v1 o inválidas → sin restricción adicional (el gate de activación ya se aplica al arrancar). El control estricto por capacidad/límites es Fase 3.

## Puntos de integración (gate)

| Punto | Comportamiento con licencia v2 sin IA |
|---|---|
| `chatbot/GeminiChatbotService.preguntar` | No llama al proveedor; responde con aviso y **búsqueda local** (función ERP permitida, spec §10.2.7) |
| `controller/ChatWidgetController` | Badge `○ Local (licencia)` con tooltip explicativo |
| `controller/MainController.irConfigurarIA` | Diálogo informativo; no permite configurar API key |
| `controller/LicenseActivatorController` | Al previsualizar/activar muestra modalidad, IA, roles y capacidad (v2) |

`MainApp` distingue dos casos: el host local conserva la validación local; una PC con `db.url` remota debe tener una licencia v2 **CENTRAL** registrada en `licencia_emitida` y no revocada. Si no puede consultar la BD central, el arranque se bloquea.

## Validación de clientes remotos

1. El instalador/cliente arranca con `DbConfig.esRemota() == true`.
2. Se lee `~/.vendex_license` (o `%APPDATA%\.vendex_license`) y se valida firma, vigencia y modalidad CENTRAL.
3. Se calcula el SHA-256 de la clave y se busca en `licencia_emitida`; debe coincidir el `machine_code`.
4. Se consulta `licencia_revocada`; si la máquina está revocada, no arranca.
5. Cualquier fallo de conexión o de consulta se trata como licencia no válida.

La emisión se hace desde el host con el CLI:

```bash
java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> \
  vitalicia --central --ia --registrar-central
```

Si el host no apunta a la BD central (por ejemplo, desarrollo local con `dbVendex`), indica la BD donde validan los clientes:

```bash
DB_PASSWORD=<clave-postgres> java -cp Vendex.jar com.vendex.util.LicenseGenerator \
  <machine-code> vitalicia --central --ia --registrar-central \
  --db-url=jdbc:postgresql://192.168.1.4:5432/dbTag --db-user=postgres
```

El comando genera la clave v2, registra el hash en `licencia_emitida` y muestra la clave para introducirla en la pantalla de activación del cliente.

## Archivos tocados

- `src/main/java/com/vendex/util/LicenseManager.java` — v2, `LicenseInfo`, gate `licenciaIncluyeIA`, resolución/ migración de secreto
- `src/main/java/com/vendex/util/LicenseGenerator.java` — flags v2
- `src/main/java/com/vendex/chatbot/GeminiChatbotService.java` — gate de IA (fallback local)
- `src/main/java/com/vendex/controller/ChatWidgetController.java` — badge
- `src/main/java/com/vendex/controller/MainController.java` — gate «Configurar IA»
- `src/main/java/com/vendex/controller/LicenseActivatorController.java` — detalles v2 en UI
- `src/test/java/com/vendex/util/LicenseManagerTest.java` — 15 tests (v1 compat, v2, expiración, tampering)

## Criterios de aceptación (spec §17 items aplicables)

2. La licencia determina las capacidades autorizadas ✅ (modalidad, IA, roles, capacidad en v2)
3. Modalidades local/central, con/sin IA diferenciadas ✅ (validación central de clientes remotos contra `licencia_emitida`/`licencia_revocada`)
9. Los cambios de licencia quedan registrados — parcial: `saveActivation` sobrescribe el archivo; el **auditoría/panel** (quién/cuándo) es Fase 3. El cliente local no tiene panel del propietario de Vendex.

## Pendientes (Fase 3+)

- Medición de tokens/costos y aplicación real de `capacidadIA` (hoy el campo existe pero no se consume).
- Panel de administración de licencias del propietario de Vendex (spec §11) y registro de cambios.
- Rotación deliberada del HMAC legacy (rompe licencias v1 — requiere reemisión).
- Endurecer permisos de cliente2: dejar `app_vendex` con solo `SELECT` sobre las tablas de licencias cuando la emisión se haga exclusivamente desde el host.
