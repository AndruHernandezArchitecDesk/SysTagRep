# Cuotas y consumo de IA (multi-PC)

El menú **Cuotas y consumo de IA** requiere `USUARIO_GESTIONAR`. La configuración,
reservas y pausas se guardan en la base compartida. Los clientes REST usan el mismo
control a través del servidor; nunca recurren a contadores locales si éste falla.

## Instalación

1. Actualizar el servidor y todos los clientes que usan la clave de Gemini.
2. Aplicar `src/main/resources/db/migration/V41__cuotas_ia.sql` en la base compartida
   con el rol migrador. El arranque también intenta preparar el esquema de forma
   idempotente. En instalaciones con mínimo privilegio, el administrador de BD debe
   otorgar `SELECT, INSERT, UPDATE ON ia_cuota, ia_solicitud TO app_vendex`.
3. Entrar al panel y activar **Sin límites locales**, luego **Guardar modelo**,
   o pulsar **Quitar límites locales de todos los modelos** para aplicarlo a todos
   los equipos de la misma base/servidor. RPM, TPM y RPD quedan en cero, sin borrar
   historial, tarifas ni pausas activas. No habilita modelos que estaban deshabilitados.
   Si se desean límites propios, desmarcar la opción y configurar RPM, TPM y RPD.
   La cuota oficial de Google se consulta en https://ai.dev/rate-limit.
4. Dejar **Ahorrar redacción** activo. Configurar salida entre 256 y 8192 tokens
   (por defecto 2048, incluye el presupuesto que el modelo destine a razonamiento).
5. Opcionalmente configurar tarifas USD por millón de tokens. Vacío significa tarifa
   desconocida; cero significa costo estimado cero. No se incluyen cargos de búsqueda
   web ni descuentos por caché. Las tarifas nuevas aplican a resultados posteriores.

**Ámbito:** una base/servidor Vendex representa un proyecto Gemini. Todos los equipos
que comparten ese proyecto deben conectarse a esta misma base/servidor. Bases
independientes, aplicaciones externas y clientes antiguos no participan del control.
Las claves diferentes del mismo proyecto siguen compartiendo las cuotas de Google.
Los aliases `*-latest` pueden compartir cuota con otros nombres de modelo en Google;
el panel los contabiliza por el nombre solicitado, no descubre esas equivalencias.

## Comportamiento

- Los límites comienzan en 0 (**sin límite local**). La opción del panel utiliza
  esos mismos valores; no requiere una nueva migración. Se sigue midiendo cada
  solicitud. No modifica cuotas de Google ni capacidad de licencia.
- RPM: ventana móvil de 60 segundos. RPD: día de `America/Los_Angeles`.
- TPM: presupuesto local **estimado de entrada** por bytes UTF-8 del contenido enviado,
  conservador y distinto del conteo oficial. No se realiza una llamada extra a countTokens.
- Una transacción bloquea la fila del modelo, revisa límites y reserva antes de enviar.
  Solicitudes concurrentes y fallidas cuentan como intentos. Una reserva sin cierre
  queda pendiente y sigue contando en su ventana: no implica tokens consumidos.
- HTTP 429: pausa compartida por modelo, sin cambiar inmediatamente al alternativo.
  Se respeta Retry-After (segundos o fecha) y RetryInfo.retryDelay. Sin indicación,
   se usa una pausa de 60 segundos; una cuota diaria puede seguir agotada al terminar.
- Si Google indica una espera menor a 60 segundos se respeta, con precisión mínima
  de un segundo. El mensaje muestra el modelo, los identificadores de cuota y valor
  reportados (si existen), y la fecha UTC antes de la cual no se reintenta. Las demás
  PCs ven esa misma pausa. No se almacena el mensaje completo del proveedor.
- La interfaz muestra desglose de entrada, respuesta, razonamiento y total informado
  por Google. El total no se vuelve a sumar a sus componentes. Sin usageMetadata,
  se indica que no hubo medición; cero no prueba que Google no haya facturado.
- El historial conserva los intentos y muestra los últimos 200, filtrables por modelo,
  fechas y fallos. No guarda prompts, claves ni cuerpos completos de errores.
- La apertura de AI Studio ocurre en un hilo propio con espera de 8 s y respaldo
  por comando externo; si el navegador no responde, el panel queda operativo y
  ofrece **Copiar enlace**. Lo mismo aplica a los enlaces de fuentes del chat.
- Los códigos con guion/barra se consultan directamente en inventario sin IA.
  Para web: «investiga …», «busca en internet …», «consulta web …».
- En modo ahorro, las respuestas estructuradas locales evitan la llamada de redacción.
  Las conversaciones generales conservan la generación de respuesta.
- Chat, web, sugerencias y análisis usan el mismo control. Si no puede reservarse en
  el servidor, no se envía una llamada a Google; las operaciones locales siguen disponibles.

## Capacidad de licencia y facturación

La pestaña de licencia conserva el historial previo y el período de capacidad Vendex.
Renovarlo no borra solicitudes, pausas ni cuotas de Google. Su costo histórico sigue
siendo una estimación global; la pestaña de cuotas ofrece tarifas por modelo para las
nuevas solicitudes. Los registros históricos no adquieren retroactivamente desglose
de razonamiento ni solicitudes fallidas.

El nivel Gratuito/Pago es una referencia administrativa: guardarlo en Vendex no cambia
el plan ni la facturación de Google. La cuota oficial restante se consulta en AI Studio.
