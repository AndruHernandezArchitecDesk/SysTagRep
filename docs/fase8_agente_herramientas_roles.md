# Fase 8 — Vendexi agente: herramientas de lectura y roles contextuales

**Fecha:** 21/10/2026 · Spec Vendexi §4 (roles IA), §5 (contexto), §16 Fase 8 · Anteriores: `docs/fase7_multiusuario_sucursales.md`

## Objetivo

Convertir al chatbot en un **agente**: clasificar la intención del usuario y responder
con **datos reales del ERP** (stock, ventas, saldos, rentabilidad) en lugar de solo
buscar repuestos, respetando los **roles IA de la licencia** (§4).

## 1. Herramientas de lectura (Fase A)

### Flujo (3 pasos)
1. **Clasificador** — una sola llamada a Gemini (`promptClasificacion`) devuelve JSON con
   `accion` + parámetros (`descripcion`, `cliente`, `marca`, `modelo`, `anio`, `motivo`,
   `esSintoma`). JSON sin `accion` o desconocida → degrada a `BUSCAR_REPUESTO`
   (compatibilidad con prompts viejos).
2. **Herramienta local** — `ejecutarLocal(c, texto)` ejecuta SQL solo-SELECT sobre las
   tablas reales vía `HerramientasAgenteDAOPostgres` (fail-soft: nunca rompe el chat).
3. **Redacción** — segunda llamada a Gemini (`promptRedaccion`) resume los datos reales
   en max 3 líneas; si falla, se devuelve la respuesta local sin tocar.

### Acciones

| Acción | Herramienta | Datos |
|---|---|---|
| `BUSCAR_REPUESTO` | flujo clásico `repuestoDao.buscar` | código, stock, precio |
| `CONSULTAR_PRODUCTO` | `productos(criterio, 5)` | stock, precio, costo, margen % |
| `VENTAS_CLIENTE` | `clientes` + `ventas(id)` | facturas, total, última compra |
| `CUENTAS_COBRAR` | `clientes` + `saldo(id)` | saldo = facturas − adelantos (Pendiente) |
| `RENTABILIDAD` | `mejorMargen(5)` + `masVendidos(5)` | ranking (criterio del Análisis IA) |
| `GENERAL` | — | redacción de la IA; si falla, oferta de ayuda |

Cliente ambiguo (varios "Juan") → lista de candidatos; cliente sin datos → se informa.
Filtros de la UI (marca/modelo/año) tienen prioridad sobre lo clasificado.

### Archivos nuevos
- `chatbot/HerramientasAgente.java` — interfaz + records `Producto`, `Cliente`, `Ventas`, `Top`.
- `dao/HerramientasAgenteDAOPostgres.java` — 6 consultas solo-SELECT, fail-soft.
- `config/AppContext.java` — cableo `herramientasAgente` (local) / `null` (modo REST).

## 2. Roles contextuales (Fase B, spec §4)

- `rolesDeAccion(Accion)` → roles IA de la licencia requeridos:
  - repuestos/stock → `VENTAS` **o** `INVENTARIO` (un vendedor también consulta disponibilidad)
  - ventas de cliente → `VENTAS`
  - saldos/rentabilidad → `FINANCIERO`
  - `GENERAL` → sin rol
- Gate en `ejecutarLocal` **y** antes de la redacción: sin rol permitido → mensaje
  `🔒 Tu licencia no incluye los roles IA [...]` **sin ejecutar la herramienta y sin
  gastar la 2ª llamada IA**.
- `LicenseManager.licenciaPermiteRolIA`: v1 o licencia sin roles → todo permitido
  (no se degradan instalaciones existentes).
- `rolContextual(Accion)` — la redacción adopta personalidad: asesor comercial (VENTAS),
  analista financiero (FINANCIERO), especialista en inventario (INVENTARIO).

## 3. Fix incluido: panel contextual nunca se montaba

`view/VendexiSugerencias.fxml` declaraba `<BorderPane>` pero `VendexiSugerenciasPane`
hace `setRoot(this)` → «Root value already specified» y el panel de fase 2 no aparecía.
Corregido a `<fx:root type="javafx.scene.layout.BorderPane">` + test FxmlSmoke #13.

## 4. Comportamiento degradado

- Sin IA / licencia sin Vendexi / capacidad agotada → búsqueda local (flujos previos intactos).
- `herramientas == null` (modo REST) → todas las acciones caen al flujo clásico de búsqueda.
- Clasificador con JSON roto → flujo clásico; redacción que falla o devuelve basura → respuesta local.
- Formateo monetario/porcentual con `Locale.US` (consistente en cualquier equipo).

## 5. Tests

`src/test/java/com/vendex/chatbot/GeminiChatbotServiceHerramientasTest.java` — **25 tests**
sin red (proveedor Gemini falso con cola de respuestas, DAOs falsos): parseo del
clasificador (4), herramientas (11), flujo completo clasificar→ejecutar→redactar (3),
roles §4 (7). Suite total: **350 tests, 0 fallos** (25 skipped).

## 6. Pendientes / siguientes

- Indicador de rol IA en las respuestas; historial de conversación al reabrir el widget.
- `HerramientasAgenteDAORest` para paridad en modo remoto (fase 6).
- Plan completo: `~/Escritorio/plan-vendexi-agente.md`.

## 7. Panel contextual en Nota de Crédito (22/10/2026)

- `NotaCreditoController` monta `VendexiSugerenciasPane` por código desde `cmbFactura`.
  No requiere modificar el FXML del formulario.
- Contexto `nota_credito`: cliente de la factura seleccionada y líneas de
  `DetalleNCInput` (identificador, código, descripción, cantidad y precio).
- Verifica cliente, líneas y precios. Omite stock insuficiente porque una devolución
  no consume existencias, incluso si el contexto informa stock cero.
- Copiar lleva la observación al portapapeles. La NC mantiene sus líneas vinculadas
  a la factura de origen; el panel se usa para verificaciones del documento.
- `localizarContenidoDesde` contempla que el contenido de un `TitledPane` aún no
  tenga parent durante `initialize`, antes de crear el skin. Esto permite montar
  el panel durante la carga del FXML también en los formularios existentes.

## 8. Complementos locales desde el chat (22/10/2026)

- Tras un hallazgo de `buscarRepuesto`, `HerramientasAgente.complementarios` obtiene
  hasta dos referencias activas con stock y reutiliza `seleccionarCandidatos` del
  servicio contextual: mismo grupo, marca o coincidencia de tokens.
- El DAO consulta solo los campos necesarios del inventario y resuelve la base por
  código exacto, sin distinguir mayúsculas. Si el código es ambiguo, omite complementos.
- Se excluyen la referencia base, códigos duplicados y productos sin stock.
- La redacción existente recibe los complementos y sus precios reales. Se mantienen
  las dos llamadas IA (clasificación y redacción), sin una consulta IA adicional.
- Se identifican como asociaciones locales con compatibilidad sin confirmar.
- Sin hallazgo, sin herramientas (modo REST) o ante un error al obtener complementos,
  se conserva el comportamiento de búsqueda anterior.

### Verificación de esta ampliación

`./mvnw test`: **361 tests, 0 fallos reportados por Maven, 0 errores, 25 omitidos**.
Se añaden 11 pruebas: 5 de chat, 3 de DAO con inventario SQL en memoria, 2 de
verificación NC y 1 de montaje/contexto NC en FxmlSmoke.

La suite también imprime una aserción en un hilo secundario de
`SesionActualAislamientoTest` que Surefire no contabiliza como fallo. Corresponde a
un test existente, ajeno a estos cambios; el resultado de Maven no detecta ese caso.
