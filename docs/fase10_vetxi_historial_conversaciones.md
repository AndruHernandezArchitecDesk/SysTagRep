# Fase 10 — Vetxi: historial persistente de conversaciones

**Fecha:** 22/10/2026 · Anterior: `docs/fase9_vendexi_capibara_flotante.md`

## Objetivo

Guardar los mensajes de las nuevas conversaciones de Vetxi y recuperarlos al
reabrir el asistente, separados por usuario y sucursal. Las sesiones de consumo
existentes continúan midiendo interacción; el contenido del chat tiene su propia
persistencia.

## 1. Uso

- Al abrir un widget nuevo, recupera la conversación más reciente del usuario y
  sucursal actuales. Cerrar y abrir el mismo widget conserva la conversación activa.
- El selector **Conversaciones anteriores** muestra hasta 20 conversaciones,
  ordenadas por actividad, con título y fecha.
- **Nueva** inicia otra conversación sin borrar las anteriores. No crea registros
  vacíos: el primer mensaje determina el título.
- Al recuperar una conversación se muestran sus últimos 100 mensajes en orden.
  La base conserva los mensajes anteriores; este límite solo acota la carga de UI.
- Se conservan el autor, texto, modo sin IA, rol utilizado, roles bloqueados y las
  fuentes de investigación web. Los enlaces restaurados solo se activan para URLs
  HTTP/HTTPS válidas.
- Cargar un historial no llama al proveedor IA ni vuelve a guardar los mensajes
  recuperados. El historial es recuperación visual; no se añade memoria contextual
  al clasificador de Gemini en esta fase.

## 2. Datos y arranque

- Modelo: `model/VetxiHistorial.java` (`Ambito`, `Conversacion`, `Mensaje`, `Fuente`).
- Tabla `vetxi_chat_mensaje`, creada por
  `db/migration/V40__vetxi_historial_chat.sql` y por el fallback de arranque
  `DatabaseConnection.ensureVetxiHistorialSchema()`.
- Cada mensaje tiene un UUID y una conversación identificada por otro UUID.
  La clave única `(usuario_id, sucursal_id, mensaje_id)` impide duplicar reintentos.
- Un identificador secuencial de BD conserva el orden, incluso con fechas iguales.
  Fechas asignadas por el servidor; las enviadas por el cliente no se usan al insertar.
- Todas las consultas filtran explícitamente usuario y sucursal. Ningún listado de
  administrador o lectura global del historial se incorpora a esta fase.
- La interfaz `VetxiHistorialDAO` tiene implementaciones PostgreSQL y REST;
  `AppContext` selecciona la apropiada y construye `VetxiHistorialService`.

El historial empieza a existir con esta versión: las sesiones/tokens anteriores
no contienen los mensajes y no permiten reconstruir conversaciones previas.

## 3. Modo remoto

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/vetxi/conversaciones` | Conversaciones recientes |
| GET | `/api/vetxi/conversaciones/{id}/mensajes` | Mensajes de una conversación |
| POST | `/api/vetxi/mensajes` | Guardado idempotente de un mensaje |

Los endpoints usan la autenticación existente de `ApiServer`. El ámbito efectivo
procede de la sesión de la petición. Los identificadores enviados por el cliente
solo se comparan para detectar un cambio de sesión; nunca conceden acceso a otra
identidad o sucursal. Una discrepancia devuelve 403 y la ausencia de sesión, 401.

GET acepta `limite`, acotado a 50 conversaciones o 200 mensajes; el widget utiliza
20 y 100. Las peticiones de historial tienen timeout de 15 segundos, que también
se conserva al reintentar tras refrescar el token.

## 4. Flujo del widget y degradaciones

- Las lecturas y escrituras se ejecutan fuera del hilo FX, en una cola por widget.
  La pregunta se encola antes de consultar IA y la respuesta se encola después.
- Cada turno captura usuario, sucursal, conversación y versión del widget. Las
  respuestas y cargas tardías se descartan si cambian esos datos o se cierra la vista.
- El servicio valida otra vez la identidad antes de acceder al DAO. El servidor
  vuelve a validarla en modo remoto, incluyendo cambios de token durante un envío.
- Al cerrar/ocultar la ventana se invalida la vista, se limpian los mensajes y se
  cierra la cola para nuevas tareas. Las escrituras ya encoladas terminan si su
  identidad todavía es válida.
- Sin sesión autenticada, el chat puede seguir en memoria, sin guardar ni leer
  mensajes de un usuario por defecto.
- Si falla la base/API, el chat sigue disponible y el estado informa que no se pudo
  recuperar/guardar el historial. Los mensajes actuales permanecen en la vista;
  no se promete guardado de las operaciones fallidas ni sincronización offline.
- Si unas escrituras fallan y otras funcionan, el estado mantiene el aviso de
  mensajes sin guardar, sin afirmar que todo quedó persistido.

## 5. Verificación

```bash
./mvnw -Dtest=VetxiHistorialDAOTest,VetxiHistorialRestTest,VetxiHistorialWidgetTest,FxmlSmokeTest,VendexiMascotaTest,VetxiAnimacionesTest,AppContextTest,VendexControllerFactoryTest,RestClientResilienciaTest,ApiAuthServiceTest,GeminiChatbotServiceHerramientasTest,GeminiChatbotServiceDisponibilidadTest,VendexiSugerenciaCancelacionTest,VendexiSugerenciaServiceTest test
```

**130 tests, 0 fallos, 0 errores, 0 omitidos.** Incluye 13 pruebas nuevas:

- **5 de DAO/servicio:** persistencia, orden, roles/fuentes, aislamiento por usuario
  y sucursal, límites, idempotencia y rechazo de una sesión cambiada.
- **3 de REST:** lectura/escritura JSON real por HTTP local, límites, reintentos,
  suplantación de identidad, consultas de otro usuario y acceso sin sesión.
- **5 de widget:** recuperación desde otra instancia sin llamadas IA adicionales,
  nueva conversación y selección con fuentes, respuesta posterior al logout,
  carga atrasada de otra identidad y fallo de almacenamiento sin bloquear el chat.

Las pruebas nuevas utilizan SQL HSQLDB aislado (con adaptación del tipo TEXT),
HTTP local y proveedor IA falso. No envían consultas a Google ni guardan mensajes
de prueba en la base productiva. La migración de producción es para PostgreSQL.

Revisión final del renderizado conjunto de roles y fuentes:
`./mvnw -Dtest=VetxiHistorialWidgetTest,FxmlSmokeTest test` → **20 tests aprobados**.
También se revisó visualmente el selector, el estado de recuperación y el chat.

## 6. Distribución y siguientes

El portable permanece sin actualizar por indicación del usuario. Los cambios
se entregan en código, migración y documentación.

Siguientes del plan: auditoría de sugerencias, mejoras de investigación web y
completitud administrativa de licencias. La paridad de herramientas del agente
por REST, explicación de precios y búsqueda de compatibilidad estructurada se
completan en `docs/fase11_agente_rest_precios_compatibilidad.md`.
