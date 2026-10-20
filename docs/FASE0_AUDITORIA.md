# Fase 0 — Auditoría del repositorio Vendex

**Fecha:** 19/10/2026
**Repositorio:** SysTagRep · ERP Vendex 2.0-SNAPSHOT
**Objetivo:** documentar la arquitectura real, identificar activos reutilizables para Vendexi AI, registrar hallazgos de seguridad/deuda y proponer cambios pequeños para la Fase 1.

---

## 1. Arquitectura real

| Aspecto | Estado |
|---|---|
| Cliente | JavaFX 21.0.6, Java 17, Maven (fat-jar con `maven-shade-plugin`) |
| Main | `com.vendex.Launcher` (evita `module-info`) → `MainApp` |
| BD | PostgreSQL (`org.postgresql:postgresql:42.7.3`), pool HikariCP |
| Migraciones | Flyway 10.17.0 — **V1…V35** en `src/main/resources/db/migration/` |
| API REST | Javalin 6.3.0 embebido (`config/ApiServer.java`, puerto 7070, **deshabilitado por defecto**, se activa con `-Dapi.enabled=true`) |
| Patrón acceso a datos | Triple: interfaz `XDAO` + `XDAOPostgres` (JDBC) + `XDAORest` (58 adaptadores en `remote/`, modo cliente delgado) |
| Composition root | `config/AppContext.java` — cablea DAOs y servicios; elige Postgres vs Rest |
| Test | JUnit 5.10.0, Mockito 5.7.0, HSQLDB, Testcontainers (Postgres 15, se omite sin Docker) |
| Empaquetado | `target/Vendex-2.0-SNAPSHOT.jar` (shade), `dist/Vendex-portable-win.zip`, instaladores `jpackage` (`.msi`/`.deb`) |
| Secretos | Keyring OS (DPAPI/Keychain/libsecret) + AES/GCM en `~/.vendex/` (no en el repo) |

**Métricas:** 372 clases main, 45+ clases de test, 64 controladores JavaFX, 20 controladores REST, 44 FXML (todos con `fx:controller`), 50 modelos, ~53–56 tablas.

Arranque: `MainApp.java` → `DatabaseConnection.initFromConfig()` → Flyway (solo desde host central con password de superuser) → opcional `ApiServer` → modo offline → **validación de licencia** (`LicenseManager.isActivated()`; en modo remoto no se exige).

## 2. Módulos funcionales (rutas reales)

Convención: FXML en `src/main/resources/view/`, resto bajo `src/main/java/com/vendex/`.

| Módulo | Controlador principal | Servicio / motor | DAOs clave | Migraciones |
|---|---|---|---|---|
| Inventario | `controller/InventarioController`, `GestionStockController`, `Ubicacion*`, `HistorialProducto` | — | `InventarioDAO`, `HistorialProductoDAO`, `PercheroDAO`, `Ubicacion*` | V1, V3–V7 |
| Vehículos / compatibilidad | `controller/api/VehiculoApiController`, filtros en `InventarioController`/`NotaVentaController` | `service/VehiculoImportService` (NHTSA vPIC) | `VehiculoDAO`, `InventarioVehiculoDAO` | V26 |
| Proformas (Nota de venta) | `controller/NotaVentaController` (vista `NotaVentaView.fxml`, header "PROFORMA") | — | `NotaVentaRegistroDAO`, `NotaVentaDetalleDAO`, `ComprobanteTempDAO` | V8, V32 |
| Facturación electrónica SRI | `controller/FacturaController`, `SeguimientoSriController`, `NumeracionController`, `FirmaController` | `service/FacturaService`, `ServicioReintentoSri`; motor: `util/SRIWebService`, `XmlSriBuilder`, `ClaveAcceso`, `FirmaDigital`, `PdfElectronico`, `ConfigAmbiente`, `SRIContingenciaConfig` | `FacturaRegistroDAO`, `ComprobantePendienteSriDAO`, `SecuenciaDocumentoDAO`, `SecuenciaBloqueDAO`, `PuntoEmisionDAO`, `CertificadoEstadoDAO` | V15, V19, V23, V29, V30, V33, V34 |
| Notas de crédito | `controller/NotaCreditoController` | `service/NotaCreditoService` | `NotaCreditoRegistroDAO`, `NotaCreditoDetalleDAO` | V11 |
| Notas de débito | `controller/NotaDebitoController` | `service/NotaDebitoService` | `NotaDebitoRegistroDAO`, `NotaDebitoMotivoDAO` | V12 |
| Guías de remisión | `controller/GuiaRemisionController` | `service/GuiaRemisionService` | `GuiaRemision*DAO` | V13 |
| Retenciones | `controller/RetencionController` | `service/RetencionService` | `Retencion*DAO`, `TablaRetencionDAO` | V14 |
| Caja / arqueo | `controller/CajaController` (arqueo) | `service/CajaService` | `CajaSesionDAO`, `CajaMovimientoDAO` | V2 |
| Alertas | `controller/AlertaController` | `service/AlertaService`, `CertificadoAlertaService` | `AlertaDAO` | V10 |
| Crédito (por cobrar/pagar) | `controller/PorCobrarController`, `PorPagarController` | — | `CuentaPorCobrarDAO`, `CuentaPorPagarDAO`, `FacturaProveedorDAO` | V9, V32 |
| Compras / proveedores | `controller/ProveedorController`, `FacturasIngresadasController`, `HistorialCompraController`, `IngresoMercaderiaController` | — | `ProveedorDAO`, `VentaResumenDAO` | — |
| Ventas / dashboards | `controller/HistorialVentaController`, `DashboardController`, `Dashboard2Controller` | — | `VentaResumenDAO`, `DashboardDAO` | — |
| Multi-sucursal | `controller/api/SucursalApiController`, `TransferenciaApiController` | `service/TransferenciaInventarioService` | `SucursalDAO`, `TransferenciaInventarioDAO`, `PuntoEmisionDAO` | V24, V25, V27, V28 |
| Usuarios / roles / auditoría | `controller/UsuarioController`, `GestionRolesController`, `LoginController` | `util/PasswordDebilValidator`, `PoliticaBloqueo` | `UsuarioDAO`, `RolDAO`, `PermisoDAO`, `AuditoriaAccionDAO`, `LoginIntentoLogDAO` | V16–V18, V28 |
| Empresas / clientes / vendedores | `controller/EmpresaController`, `ClienteController`, `VendedorController` | — | `EmpresaDAO`, `ClienteDAO`, `VendedorDAO` | V31, V35 |
| Respaldos | `controller/BackupController` | `backup/BackupService`, `BackupConfig`, `BackupPassphraseManager`, `BackupRetentionService` | — | — |
| Email transaccional | `controller/ConfiguracionEmailController` | `util/EmailService` | `ConfiguracionEmailDAO` | — |
| Etiquetas / QR | — | `util/EtiquetaUtil`, `HojaEtiquetasPDF` (ZXing) | — | — |
| Offline / sync | `controller/ColaOfflineController` | `offline/OfflineModeManager`, `LocalOperationQueue`, `SyncService`, `OfflineHelper`, `OfflineUI` | `SecuenciaBloqueDAO` | V30 |
| **Chatbot IA (Gemini)** | `controller/ChatWidgetController` + `view/ChatWidget.fxml` (incluido en `MainView.fxml`) | **`chatbot/GeminiChatbotService`** | `RepuestoChatbotDAO` (Postgres y Rest) | — |
| **Licencias** | `controller/LicenseActivatorController` + `view/LicenseActivatorView.fxml` | **`util/LicenseManager`**, `util/LicenseGenerator` (CLI) | — | — |

Todos los FXML tienen controlador declarado 1:1 (verificado; mapa en `util/CatalogoVistas.java`).

## 3. Activos reutilizables para Vendexi AI

1. **Chatbot Gemini (base de Vendexi AI)**
   - `chatbot/GeminiChatbotService.java` — cliente HTTP a Gemini (`gemini-3.1-flash-lite` con fallbacks), estados `IA_ACTIVA`/`SIN_API_KEY`/`SIN_IA_TOKENS`/`SIN_IA_SERVIDOR`/`SIN_IA_CONEXION`/`SIN_IA_KEY_INVALIDA`; fallback local a búsqueda ILIKE en inventario.
   - `controller/ChatWidgetController.java`, `view/ChatWidget.fxml`, `css/chat-widget.css` — widget flotante en `MainView.fxml:414` con badge de estado y filtros de vehículo.
   - `config/GeminiConfig.java` — `GEMINI_API_KEY` cifrada en `~/.vendex/gemini.properties` (prioridad: env > system property > archivo).
   - `config/NvidiaConfig.java` + `service/NvidiaCodeService.java` — API Nvidia (utilidad dev de generación de código; no es flujo de negocio).
   - Menú «Configurar IA» en `MainController.java:385-414`.
   - **Hoy:** el usuario final configura su propia key; el chatbot es «asistente de repuestos» (búsqueda + respuesta), sin roles, sin medición de tokens ni capacidad por licencia.

2. **Licenciamiento propio**
   - `util/LicenseManager.java` — código de máquina por MAC, archivo `~/.vendex_license`, HMAC-SHA256, validez 30 días o vitalicia (`PERPETUA_MARKER`). Secreto `license.hmac.key` desde `SecureConfigStore` con fallback hardcodeado (ver §4).
   - `util/LicenseGenerator.java` — CLI: `<machine-code> [vitalicia|días|YYYYMMDD]`.
   - `MainApp.java:77-82` — en modo local, sin licencia activa se muestra `LicenseActivatorView` y se bloquea la app. **No se exige en modo remoto** (BD remota).
   - **Hoy:** binaria (activada/no). Sin modalidades, sin IA, sin capacidad ni vigencia por rol.

3. **Backend REST (base de la Fase 6)**
   - `config/ApiServer.java` (Javalin, puerto 7070) + 20 `controller/api/*ApiController` + 58 `remote/*DAORest` + `remote/RestClient`, `remote/ApiConfig`.
   - Docs: `docs/API_REST.md`, `docs/fase2_backend_api.md`.
   - **Hoy:** deshabilitado por defecto; la modalidad «central» es viable pero sin aislamiento por empresa, auth de licencias ni medición de IA.

4. **Infraestructura de soporte**
   - Secretos: `security/MasterKeyManager`, `KeyringSecretProvider`, `util/SecureConfigStore` (tier1 máquina / tier2 instalación), `util/Cifrado` (AES-GCM).
   - Multi-sucursal: `model/Sucursal`, `PuntoEmision`, `util/SucursalActual`, migraciones V24–V29.
   - Auditoría: `auditoria_accion` (V18), `AuditoriaAccionDAO`.
   - Offline: `offline/*` + `SecuenciaBloque` (reserva de bloques de numeración).
   - Alertas: `AlertaService` + tabla `alertas` (V10) — base para alertas internas de consumo.

## 4. Hallazgos de seguridad

| # | Hallazgo | Ruta | Acción |
|---|---|---|---|
| S1 | Password de aplicación Gmail **hardcodeado** como fallback de correo | `util/EmailService.java:24` (usado en 116-122) | **Corregido en esta fase:** eliminado el fallback; si no hay config activa en BD, `enviarCorreo*` falla con `ultimoError` claro |
| S2 | Clave API Nvidia **en el historial de git** (commit `505a42f` la eliminó del código; presente en `2aef036`, `5f991c9`, `d9c3bdf`, `2108474`) | historial git | **Manual:** rotar/revocar la clave en Nvidia si sigue válida; el código actual ya no la contiene |
| S3 | Contraseña SMTP de S1 sigue viva en Gmail (aplicación) | cuenta Gmail | **Manual:** revocar la contraseña de aplicación y regenerar en Configuración → Email de cada instalación |
| S4 | HMAC de licencia con fallback hardcodeado `Vendex2024!@#SecureKey_9f8a2b` | `util/LicenseManager.java:27` | **Resuelto en Fase 1:** el secreto se resuelve env > keyring/store > migración (siembra legacy en store local, sin romper licencias vigentes). Ver `docs/fase1_licencias.md` |
| S5 | `.gitignore` no cubre `*.p12`, `*.jks`, `*.pem`, `gemini.properties`, `nvidia.properties`, `.env` | `.gitignore` | **Corregido en esta fase** |

Verificado: **no hay** `.p12`, claves `AIza`/`nvapi-`/`sk-`, ni properties tracked en el working tree actual.

## 5. Deuda técnica / observaciones

1. **Desalineación prod ↔ dev:** `scripts/faltantes_dbTag_prd.sql` (2.738 líneas) nivela `dbTag` (producción) contra el esquema Flyway de desarrollo. Producción está desfasada; conviene un plan de convergencia antes de añadir tablas de licencias.
2. **Scripts SQL sueltos** en `src/main/resources/sql/` (~24) duplican funcionalidad de `db/migration/` — riesgo de divergencia.
3. **`docs/TESTING.md` desactualizado:** declara 50 tests; hoy hay ~190 anotaciones `@Test` en 46 clases.
4. **Logo ausente:** `empaquetar_instalador.sh:11` referencia `src/main/resources/img/logoVendex.png`, que no existe (sí `logoTAG.png`).
5. **Error runtime conocido:** logs en `vendex-errors/error-20261012-*.txt` — `javafx.fxml.LoadException` al cargar `FacturaView.fxml` vía accesos directos (`MainController.cargarVista`).
6. **Pruebas parciales sin infraestructura:** Testcontainers requieren Docker; `SRIRealIntegracionTest` requiere `.p12` real. Sin Docker solo corren unitarios + HSQLDB.
7. **Licencia solo en host local:** `MainApp` no exige licencia cuando la BD es remota — relevante para el diseño de Fase 1 (modalidad central).

## 6. Decisiones tomadas (con el propietario)

1. Informe de auditoría en `docs/FASE0_AUDITORIA.md` (este documento).
2. Estrategia IA: **evolucionar el chatbot Gemini local primero** (Fases 1–3 sobre la base actual); el backend central (Fase 6) viene después.
3. Seguridad: incluir en Fase 0 la limpieza del código + endurecer `.gitignore`; la rotación de claves (S2, S3) queda como acción manual del propietario.
4. Licencias: **evolución incremental** de `LicenseManager` (modalidad, IA, roles, capacidad) manteniendo compatibilidad con licencias ya emitidas; no un diseño nuevo desde cero.

## 7. Ejecución de pruebas (Fase 0)

Ejecutado con `./mvnw test` — resultados en §9.

## 8. Propuestas de cambios para la Fase 1 (solo propuestas)

Sin implementar todavía; rutas reales:

1. **Modelo de licencia ampliado** — evolucionar `util/LicenseManager.java` + `util/LicenseGenerator.java`:
   - Campos nuevos (compatibles con el formato HMAC actual o con versión incrementada): `modalidad` (LOCAL|CENTRAL), `incluye_ia` (bool), `roles_ia` (lista), `capacidad_ia` (unidades), `vigencia`.
   - Persistir la licencia parseada en `~/.vendex/` (ya existe `~/.vendex_license`); no migrar a BD todavía (la BD del cliente no debe controlar su propia licencia).
   - Eliminar el fallback hardcodeado S4; exigir `license.hmac.key` en keyring/`SecureConfigStore`.
2. **Gate de IA por licencia** — en `controller/ChatWidgetController` / `MainController` (menú «Configurar IA»): ocultar o deshabilitar el widget y la configuración si la licencia no incluye IA.
3. **API de validación** — exponer en `config/ApiServer` (futuro backend) un endpoint de verificación de licencia firmada, preparado para la modalidad central; el cliente sigue validando localmente.
4. **Tests** — añadir `LicenseManagerTest`/`LicenseGeneratorTest` (formato, vitalicia, expiración, machine code) en `src/test/java/com/vendex/util/`.
5. **Docs** — `docs/fase1_licencias.md` siguiendo el patrón de `docs/fase1_vpn.md`.

## 9. Resultados de Fase 0 (cambios, pruebas, pendientes)

### Cambios realizados en esta fase
- `docs/FASE0_AUDITORIA.md` — creado (este informe).
- `src/main/java/com/vendex/util/EmailService.java` — eliminadas las constantes `FALLBACK_REMITENTE`/`FALLBACK_PASSWORD`/`FALLBACK_HOST`/`FALLBACK_PORT` y su uso: sin configuración activa en BD, el envío falla con `ultimoError` explícito («Sin configuración de correo activa…»).
- `.gitignore` — añadidos `*.p12`, `*.jks`, `*.pem`, `*.crt`, `*.key`, `gemini.properties`, `nvidia.properties`, `.env`.

### Pruebas
- `./mvnw test` (19/10/2026, sin Docker): **BUILD SUCCESS — 180 tests, 0 fallos, 0 errores, 24 omitidos** (~1 min 23 s).
- Los 24 omitidos son los suites Testcontainers (`disabledWithoutDocker = true`); en una máquina con Docker se ejecutarán los DAOs contra PostgreSQL 15 real.
- `EmailServiceTest` (2 tests) pasa tras la eliminación del fallback: no dependía de las credenciales embebidas.
- Verificado también `./mvnw compile` tras los cambios (solo warnings de JDK/Jansi, sin errores).

### Pendientes (acciones manuales del propietario)
- [ ] Rotar/revocar clave Nvidia del historial git (S2).
- [ ] Revocar la contraseña de aplicación Gmail de S1/S3 y regenerar en cada instalación afectada.
- [ ] Definir plan de convergencia de `dbTag` (producción) con el esquema Flyway (deuda §5.1).

### Pendientes (siguiente fase)
- Fase 1: modelo de licencia con modalidades/IA/capacidad, gate del chatbot, tests de licencia.
