# Contingencia SRI — Cola persistente (24h) + entrega inmediata

> `LINEAMIENTO_CONTINGENCIA_SRI.md` implementado. RPO/operación desacoplada: el mostrador entrega PDF al firmar, no al autorizar.

## Qué hace

- **Todo a cola:** al emitir FACTURA/NC/ND/GR/RETención se genera PDF **provisional PENDIENTE** y se encola en `comprobante_pendiente_sri` inmediatamente. No hay modal "Consultando al SRI 60s". El cliente recibe el comprobante (clave de acceso válida firmada) en el mostrador.
- **Job de fondo** `ServicioReintentoSri` (`ScheduledExecutorService` cada 2min, daemon) consulta `SRIWebService.consultarAutorizacion(clave)` solo para pendientes con `proximo_intento <= now()`.
- **Backoff creciente hasta AGOTADA 24h (~30 intentos):**
  | Intentos | Espera |
  |---|---|
  | 1–5 | 2 min |
  | 6–15 | 15 min |
  | 16–30 | 1 h |
  | >30 | `AGOTADA` (stop) → email `~/.vendex/contingencia.properties` `sri.contingencia.email` |
- **Distingue rechazo vs caída:** `AUTORIZADO` → actualiza `comprobantes_electronicos` + registro específico + `xml_enviados` + regenera RIDE + (correo si aplica) y marca `AUTORIZADO`. `RECHAZADA/DEVUELTA/NO AUTORIZADO` → **no reencola**, marca `RECHAZADA`, notifica inmediato, requiere corrección manual. `RECIBIDA/PENDIENTE/ERROR/timeout` → reintenta con backoff.
- **Modo contingencia:** si `sri.contingencia.modo=true` (default), `NotaCredito/NotaDebito/GuiaRemision` permiten sustento `PENDIENTE/RECIBIDA/ERROR` además de `AUTORIZADO` (ver `NotaCreditoService.java:83` etc.). Permite emitir NC sobre factura aún no autorizada durante caída prolongada.
- **Banner amarillo** `MainView.fxml` `bannerSri` (estilo certificado) muestra "X comprobantes esperando autorización — servicio con demoras, se reintenta cada 2min" (`MainController.java:iniciarBannerSriPendientes` Timeline 5s/120s). Visible sin abrir `Seguimiento SRI`.
- **Seguimiento SRI** `SeguimientoSriController.java:172` ahora es **vista** de la cola (`comprobante_pendiente_sri`) + botón "Consultar SRI" que fuerza `proximo_intento=NOW` y ejecuta `ciclo()` inmediato. No ejecuta lógica inline de 5 DAOs.
- **Email parametrizado por cliente:** archivo separado `~/.vendex/contingencia.properties` (`sri.contingencia.email`, `sri.contingencia.modo`), no `backup.properties`. UI: `Administración → SRI Contingencia (email)` en `MainController.irConfigurarContingenciaSRI`. Fallback a `configuracion_email.emailRemitente` si vacío. Alertas `RECHAZADA` inmediata y `AGOTADA 24h`.

## Archivos

- `util/SRIContingenciaConfig.java` — `~/.vendex/contingencia.properties`
- `model/ComprobantePendienteSri.java` + `dao/ComprobantePendienteSriDAO.java` (`encolar`, `listarParaReintentar`, `contarPendientes`, `forzarReintentoAhora`)
- `service/ServicioReintentoSri.java` — job, backoff, regenerarRide, notificaciones via `ConfiguracionEmailDAO`
- `config/DatabaseConnection.java:ensureComprobantePendienteSriSchema` + `util/AppConstants.java:ESTADO_AGOTADA`
- `service/FacturaService.java:215` + controllers `Factura/NotaCredito/NotaDebito/GuiaRemision/Retencion` → provisional + encolar
- `controller/MainController.java + view/MainView.fxml` banner SRI

## Operación SRI caído horas

1. Cajeros siguen facturando con normalidad (PDF provisional entregado). Banner amarillo indica demora.
2. Al volver SRI, cola autoriza sola sin reproceso manual; RIDE se regenera en escritorio y correo se enviará (si `debeEnviarNotificacion`).
3. Si pasan 24h sin autorizar, email `AGOTADA` al admin parametrizado (revisar manualmente). Si `RECHAZADA`, email inmediato.

## Verificación

- Simular SRI caído mockeando `SRIWebService` → emitir factura → PDF provisional entregado <1s, encolado `SELECT * FROM comprobante_pendiente_sri`, banner muestra 1 pendiente, tras mock OK se autoriza sola y RIDE regenerado.
- Probar `RECHAZADA` mock → no reencola, email inmediata.
- Test backoff: `ServicioReintentoSri.calcularProximoIntento(6) → +15min`.
- `psql -c "SELECT clave_acceso, estado, intentos, proximo_intento FROM comprobante_pendiente_sri ORDER BY proximo_intento"`
- Seguimiento SRI → "Consultar SRI" fuerza ciclo inmediato.

## Plazo legal

`AGOTADA` 24h alineada a plazo confirmado. Si normativa cambia, ajustar umbral `>30` en `ServicioReintentoSri.java:procesarUno`.
