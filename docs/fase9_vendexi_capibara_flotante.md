# Fase 9 — Vetxi capibara: asistente flotante y sugerencias contextuales

**Fecha:** 22/10/2026 · Anterior: `docs/fase8_agente_herramientas_roles.md`

## Objetivo

Concentrar el chat, las verificaciones del documento y las sugerencias en un
capibara flotante. Se retiran los paneles incrustados de Factura, Proforma/Nota
de Venta y Nota de Crédito.

## 1. Experiencia de uso

- Capibara vectorial propio (`view/Capibara.fxml`), con fondo transparente,
  respiración suave y un breve balanceo cuando hay sugerencias nuevas.
- Se arrastra con el mouse dentro de la ventana principal. Arrastrar no abre el
  chat. La posición relativa se recuerda en Preferences
  `/com/tag/vendex/asistente` (`x`, `y`), por instalación/perfil del sistema operativo.
- Al redimensionar, la mascota, el chat y los globos se mantienen dentro de la
  ventana. Las zonas vacías de la capa flotante dejan pasar los clicks.
- Pulsar el capibara recupera las sugerencias pendientes; sin pendientes, abre
  o cierra el chat. El acceso del menú abre ese mismo chat, sin otra ventana modal.

## 2. Burbujas breves

- Agregar un producto en Factura o Proforma utiliza la selección/consulta IA
  contextual existente, respetando licencia, clave y capacidad.
- Una espera de 650 ms agrupa cambios rápidos; una nueva versión del documento
  cancela la espera o consulta anterior.
- Cada lote muestra una sugerencia por vez: título, código, stock y precio.
  Acciones: **Agregar 1**, **Descartar**, **Siguiente**, **Ver detalles**, **Copiar**
  y **Abrir chat**. Los detalles incluyen motivo y nivel de evidencia.
- El globo se oculta tras 12 segundos; el contador conserva los pendientes para
  recuperarlos. Al pasar el mouse o ampliar detalles, se pausa el ocultamiento.
- Los avisos automáticos no solicitan foco ni interrumpen el chat abierto; en
  ese caso se actualiza el contador y el botón de sugerencias del chat.
- Agregar es una decisión explícita del usuario y usa la acción del formulario
  activo, actualizando líneas y totales. Se excluyen duplicados, productos sin
  stock y productos ya presentes en el documento.

## 3. Verificar documento desde el asistente

El chat ofrece **Verificar documento** cuando hay un formulario compatible activo:

| Formulario | Verificaciones | Complementos |
|---|---|---|
| Factura | cliente, líneas, precios, stock | sí |
| Proforma / Nota de Venta | cliente, líneas, precios, stock | sí |
| Nota de Crédito | cliente, líneas, precios; sin exigir stock | no se agregan referencias ajenas a la factura de origen |

Las verificaciones son locales, disponibles sin licencia IA ni API key. Si no
hay observaciones, el globo lo informa. Fuera de estos formularios, el chat
permanece disponible y la acción contextual se deshabilita.

## 4. Integración y ciclo de vida

- `DocumentoVendexi`: contrato de contexto, conexión del asistente y acción
  opcional de agregar productos; implementado por los tres controllers.
- `MainView.fxml`: `fx:include` con id `asistente`; `MainController` recibe
  `asistenteController` y conecta el documento al terminar cada navegación.
- `ChatWidgetController`: una sola cola de sugerencias y una revisión del
  documento. Cada consulta captura formulario y revisión. Cambiar de pantalla,
  cliente, líneas o totales invalida los resultados pendientes y tardíos.
- El formulario anterior se desconecta al navegar. Al ocultar/cerrar la ventana
  se detienen animaciones y consultas contextuales y se finaliza la sesión de chat.
- Se eliminan `VendexiSugerenciasPane.java` y `VendexiSugerencias.fxml`, sustituidos
  por el globo del widget. El chat conserva sus indicadores de rol y la recuperación
  ante HTTP 503 del servicio de conversación existente.

## 5. Verificación

```bash
./mvnw -Dtest=VendexiMascotaTest,FxmlSmokeTest,VendexControllerFactoryTest,VendexiSugerenciaServiceTest,GeminiChatbotServiceHerramientasTest,GeminiChatbotServiceDisponibilidadTest,HerramientasAgenteComplementariosTest test
```

**91 tests, 0 fallos, 0 errores, 0 omitidos.**

`VendexiMascotaTest` añade 9 pruebas de interacción: verificación local y
recuperación del globo, resultados obsoletos, aceptación única, exclusiones,
devoluciones, límites de posición, integración con navegación real de Main,
arrastre sin click y respuesta IA tardía que ignora cancelación.

`FxmlSmokeTest` comprueba la ausencia del panel en los tres formularios, el
contexto NC y la inyección del asistente único. También se revisaron visualmente
el capibara, el globo y el chat sobre un formulario de demostración.

Empaquetar con limpieza para retirar recursos antiguos del JAR:
`./mvnw clean -DskipTests package`; actualizar `dist/Vendex-portable-win.zip`.

## 6. Identidad Vetxi y animaciones contextuales

El nombre visible pasa de Vendexi a **Vetxi** en la mascota, chat, globos,
accesibilidad, tooltips, mensajes de licencia y prompts de los servicios IA.
Los identificadores técnicos existentes de clases, tablas y configuración se
conservan para mantener los datos y la integración de las instalaciones.

`VetxiAnimaciones` coordina cuatro estados y utiliza piezas independientes del
vector `Capibara.fxml`:

| Estado | Comportamiento |
|---|---|
| REPOSO | Respiración suave. Programa un rascado ocasional cada 18–32 segundos. |
| RASCANDO | Levanta la pata trasera, se sacude y aparecen pequeñas pulgas saltando. Dura aproximadamente 1,8 segundos. |
| ANOTANDO | En Factura o Proforma se pone lentes y una libreta; hace trazos con el lápiz y breves pausas. |
| PASEANDO | Tras 60 segundos sin actividad recorre trayectos suaves, mueve las patas y gira según la dirección. |

- El movimiento de mouse, pulsaciones, arrastre, scroll y entrada táctil se
  observan en la escena principal; la actividad detiene el paseo inmediatamente
  en su posición actual. En un documento comercial vuelve a tomar notas.
- No pasea con el chat o un globo abierto, sugerencias pendientes, una consulta
  en curso, mientras se arrastra ni cuando el mouse está encima del capibara.
- Las animaciones se pausan cuando la ventana se oculta, minimiza o pierde el
  foco. Al regresar comienza una nueva espera de inactividad. Los filtros de
  entrada y listeners se desconectan al cambiar de escena/ventana.
- El paseo no escribe posiciones en Preferences: únicamente el arrastre manual
  guarda una posición. El desplazamiento automático no afecta los documentos,
  el foco del formulario ni el consumo de IA.

### Verificación de la ampliación

```bash
./mvnw -Dtest=VetxiAnimacionesTest,VendexiMascotaTest,FxmlSmokeTest,GeminiChatbotServiceHerramientasTest,VendexiSugerenciaServiceTest,InvestigacionWebServiceTest,AnalisisEmpresarialServiceTest,LicenseManagerTest test
```

**115 tests, 0 fallos, 0 errores, 0 omitidos.** Incluye 6 pruebas nuevas del
motor de animaciones con reloj controlado y 1 de identidad/interacción de la
escena. Se comprueban lentes, movimiento del lápiz, rascado, tiempos, límites,
interrupción de movimiento, bloqueo por interacción y suspensión de la ventana.
También se revisaron visualmente las poses de reposo, escritura, rascado y paseo.

## 7. Cancelación normal de venta cruzada y diagnóstico de errores

Cambiar el producto o documento interrumpe el hilo contextual anterior. Si ocurre
durante `HttpClient.send`, este puede lanzar `InterruptedException` sin mensaje.
El manejador genérico anterior lo registraba como
`Vendexi: venta cruzada IA no disponible: null`, ocultando que era una cancelación
esperada del formulario.

`VendexiSugerenciaService` ahora:

- Omite trabajo si el hilo ya fue interrumpido y descarta resultados cancelados.
- Captura `InterruptedException` por separado, restaura la señal de interrupción
  y devuelve lista vacía sin advertencias. `CancellationException` también se trata
  como finalización normal; el detalle queda en nivel `FINE`.
- Conserva las advertencias de fallos reales, indicando el tipo de excepción y
  su mensaje si existe, además de la causa técnica completa. Ya no concatena `null`.
- Utiliza el nombre Vetxi en el diagnóstico.

Verificado con:
`./mvnw -Dtest=VendexiSugerenciaCancelacionTest,VendexiSugerenciaServiceTest,VendexiMascotaTest,FxmlSmokeTest test`
→ **49 tests, 0 fallos, 0 errores, 0 omitidos**. Las 6 pruebas nuevas reproducen
cancelaciones antes, durante y después de consultar, recuperación en la siguiente
consulta y diagnóstico de errores con/sin mensaje, sin claves reales ni red.

## 8. Rediseño frontal

`Capibara.fxml` presenta a Vetxi mirando al usuario en su estado natural:
rostro simétrico, dos ojos con reflejos, hocico centrado, sonrisa suave,
orejas y mejillas equilibradas, pelaje con degradados cálidos y la planta
característica en la parte superior.

Los lentes se alinean sobre ambos ojos; la libreta y el lápiz se sitúan frente
al cuerpo. La pata de rascado se levanta por el costado izquierdo, conservando
la lectura de la cara. Se mantienen los identificadores de las piezas y el
lienzo de 100 × 82 para conservar la integración con el motor de animaciones.

Verificación: revisión visual de reposo, anotación, rascado y paseo;
`./mvnw -Dtest=VetxiAnimacionesTest,VendexiMascotaTest,FxmlSmokeTest test`
→ **31 tests, 0 fallos, 0 errores, 0 omitidos**.

Por indicación del usuario, el portable no se actualiza con este rediseño.
Las futuras regeneraciones del portable requieren que el usuario las solicite.

Siguiente fase implementada: `docs/fase10_vetxi_historial_conversaciones.md`,
historial persistente por usuario y sucursal integrado en el asistente flotante.
