# Vendex — Links de instaladores y procedimientos

Chuleta rápida: de dónde descargar cada instalador y cómo usar `run.bat`, configurar la BD, licencias e IA. Sistema: portable Windows sin BD incluida (la BD vive en el servidor).

## 1. Links de descarga

| Para qué | Link | Notas |
|---|---|---|
| JDK 17 x64 `.msi` (máquina que **fabrica** el MSI, trae `jpackage`) | https://adoptium.net/temurin/releases/?version=17 | Debe decir **jdk**, no jre. Marcar **Add to PATH** + `JAVA_HOME`. Verificar: `java -version` y `jpackage --version` |
| JRE 17+ (PCs **clientes**, solo para correr el portable) | https://adoptium.net/temurin/releases/?version=17 (variante JRE) | Marcar Add to PATH. Verificar: `java -version` |
| WiX Toolset **3.11.2** (`wix311.exe`, el que exige `jpackage` del JDK 17) | https://github.com/wixtoolset/wix3/releases | En wixtoolset.org ya solo ofrecen v4+ (no sirve). Agregar a PATH, reabrir `cmd` |
| PostgreSQL 16+ Windows (solo máquina **servidor**) | https://www.postgresql.org/download/windows/ | Solo donde vive `dbTag` |
| Gemini API key (chatbot IA) | https://aistudio.google.com/apikey | Gratis, una por instalación (se pega por PC) |

> `.deb` es solo Ubuntu: **no abre en Windows**. Para Windows usa el ZIP portable o el `.msi`.

## 2. `run.bat` — cómo abrir la app

1. Descomprime el ZIP en `C:\Vendex`.
2. Doble clic a **`run.bat`** (nunca al `.jar` directo: Windows lo lanza sin ventana y si falla no ves nada).
3. La ventana negra **no se cierra sola**: muestra `java -version`, errores y el log de arranque. Si Vendex no abre, toma **foto de esa ventana**.

## 3. Configurar la base (wizard, primer arranque)

Campos del asistente **Conectar a Base de Datos existente**:

| Campo | Valor |
|---|---|
| Servidor (IP o nombre) | IP del PC con PostgreSQL (ej. `192.168.1.4`). **No** la IP del propio cliente |
| Puerto | `5432` |
| Base de datos | `dbTag` |
| Usuario / Contraseña | Los mismos de la instalación existente (ej. `app_vendex` + clave actual) |

Pulsa **Probar conexión**, espera el ✓ verde y luego **Guardar**. La config queda cifrada en `C:\Users\<tu-usuario>\.vendex\db.properties` (línea `db.url` = IP a reutilizar en las demás PCs).

**Cambiar de servidor después:** doble clic a `Reconfigurar-Vendex-BD.bat` (reabre el wizard sin reinstalar).

## 4. Abrir PostgreSQL a la red (solo servidor)

1. `postgresql.conf` (ej. `C:\Program Files\PostgreSQL\16\data\`): `listen_addresses = '*'`
2. `pg_hba.conf` (misma carpeta, al final): `host    dbTag    all    192.168.1.0/24    scram-sha-256`
3. Servicios → `postgresql-x64-16` → **Reiniciar**.
4. Firewall → regla de entrada TCP puerto `5432` (nombre: `Postgres Vendex`).
5. Desde el cliente, en `cmd`: `ping <IP-servidor>` y `powershell -Command "Test-NetConnection <IP-servidor> -Port 5432"` (debe salir `True`).

## 5. Licencia y chatbot IA

* **Licencia:** solo la pide el **servidor** (`localhost`). Clientes remotos entran directo. Pantalla Activación → pegar clave con guiones (`XXXXX-XXXXX-XXXXX-XXXXX-YYYYMMDD`, vence en la fecha final).
* **IA:** menú lateral → **Configurar IA** → Gemini → pegar key → Guardar. Repetir **en cada PC** (la key es local por máquina); el badge pasa a `● IA Gemini`. Sin key, el chatbot sigue buscando en modo local.

## 6. Fabricar los artefactos (máquina de desarrollo)

* **MSI Windows:** en PC Windows con JDK 17 + WiX → `empaquetar_instalador.bat` → `dist\Vendex-2.0.0.msi` (muestra progreso Maven; tiene `pause` en cada salida para leer errores).
* **Portable Windows (puente sin MSI):** `dist\Vendex-portable-win.zip` (JAR + `run.bat` + `Reconfigurar-Vendex-BD.bat` + LEEME). Requiere JRE 17 en destino.
* **Ubuntu:** `./empaquetar_instalador.sh` → `dist/vendex_2.0.0_amd64.deb` (`sudo apt install ./vendex_*.deb`).
* **Updates:** instalar/reemplazar encima; `db.properties` y licencia se conservan. Nunca desinstalar marcando borrar datos.

## 7. Mensajes de Gemini y Java

* **Gemini HTTP 503:** indisponibilidad temporal del proveedor. El chat prueba los
  modelos de respaldo y omite durante 60 segundos los modelos que acaban de fallar.
  Si ninguno responde, conserva la búsqueda local. No implica por sí solo una clave
  inválida ni cuota agotada (se informan como 401/403 y 429, respectivamente).
* **`sun.misc.Unsafe::allocateMemory will be removed in a future release`:** advertencia
  de una API usada por alguna dependencia al ejecutar con Java reciente; no es la
  causa del HTTP 503. Para este paquete con JavaFX 21, se recomienda ejecutar con
  Java 17 o 21 LTS. En la validación visual se reprodujo desde
  `com.sun.marlin.OffHeapArray`, el renderizador incluido con JavaFX, usando Java 25.
  La línea `has been called by ...` permite confirmar el origen en cada instalación.

## 8. Vetxi capibara

Vetxi aparece como capibara flotante en la ventana principal. Arrástralo
para colocarlo donde prefieras; recordará su posición. Pulsa para abrir el chat
o recuperar las sugerencias indicadas por el contador.

Las sugerencias de Factura y Proforma aparecen en globos no modales, con acciones
para ver detalles, agregar una unidad o descartar. Se ocultan automáticamente sin
perder los pendientes. Desde el chat, **Verificar documento** revisa el formulario
activo; también funciona en Nota de Crédito y sin conexión al proveedor IA.

En Factura y Proforma se pone lentes y toma notas en su libreta. En reposo se
rasca ocasionalmente. Después de 60 segundos sin actividad puede pasear por la
ventana; se detiene al mover el mouse o pulsar una tecla. El paseo se pausa si
estás leyendo el chat, hay sugerencias pendientes o la ventana está minimizada
o sin foco. Los movimientos automáticos no cambian la posición manual guardada.

Detalle técnico: `docs/fase9_vendexi_capibara_flotante.md`.

## 9. Historial personal de Vetxi

El chat recupera la conversación más reciente del usuario y sucursal actuales.
Usa **Conversaciones anteriores** para elegir otra y **Nueva** para comenzar una
conversación sin borrar las previas. Se guardan preguntas, respuestas, roles y
fuentes web desde esta versión, tanto en acceso directo a BD como por API REST.

El esquema se incorpora con la migración **V40** y el fallback habitual de arranque.
El historial no consume tokens al recuperarse. Si el almacenamiento no está
disponible, el estado del chat indica que los mensajes no pudieron guardarse.

Detalle técnico: `docs/fase10_vetxi_historial_conversaciones.md`.

## 10. Producto temporal en Factura de Venta

Usa **Producto temporal** junto al buscador para capturar código nuevo, descripción,
cantidad y precio unitario **sin IVA** (sección 13). Al guardar la factura, el producto queda
registrado en el inventario de la sucursal y su stock se descuenta: vender 3 unidades
de una referencia nueva deja **stock −3**. Cancelar o quitar la línea antes de guardar
no crea el producto. Si el código ya existe en la sucursal, debe agregarse desde el
buscador normal o utilizarse otro código.

En modo offline, el alta se realiza al sincronizar y registrar la factura en el servidor.
Detalle técnico: `docs/factura_productos_temporales.md`.

## 11. Stock negativo al ingresar facturas de proveedor

En **Ingreso de Facturas** revisa el panel derecho **Stock negativo**. Presenta
los productos activos con saldo bajo cero de la sucursal actual, ordenados por
fecha de alta descendente. Tras registrar un ingreso pulsa **Actualizar** si el
listado no se ha actualizado todavía. En edición de producto/factura el panel se
oculta para dejar espacio al formulario.

El botón **«Stock negativo»** junto al título **Ingreso de Factura** oculta y
vuelve a mostrar ese panel derecho cuando necesitas más ancho para el formulario.
En la pantalla **Préstamos Financieros**, el botón **«Ingresos recientes»** junto
al título oculta y muestra el panel de ingresos. En ambos casos la preferencia
dura mientras la pantalla permanece abierta.

Detalle técnico: `docs/ingreso_panel_stock_negativo.md`.

## 12. Editar precio unitario con doble clic

En **Factura de Venta** y en **Proforma (Nota de Venta)**, el **precio unitario** de
la tabla de detalle se edita con **doble clic** sobre la celda: se abre un campo de
texto, se escribe el nuevo precio (acepta coma o punto decimal) y se confirma con
**Enter**. La línea, el subtotal, el IVA, el descuento y el total se recalculan al
instante; Vetxi recibe el documento actualizado.

Los precios inválidos —cero, negativos o con más de 2 decimales— **no se aplican**:
la celda vuelve silenciosamente al valor anterior. Las demás columnas (cantidad,
código, descripción) no se editan de esta forma.

En la factura electrónica no cambia nada del XML: la pantalla y el XML construido
usan las mismas fórmulas sobre el precio unitario, por lo que los totales del
comprobante siguen coincidiendo exactamente con lo que se muestra.

Detalle técnico: `docs/factura_productos_temporales.md`.

## 13. Factura de Venta: precio sin IVA

En **Factura de Venta** el precio unitario se captura y se muestra **sin IVA**; el
IVA del 15 % se suma automáticamente sobre la base menos el descuento. Ejemplo:
3 × 11.50 ⇒ **Subtotal 34.50, IVA 5.18, Total 39.68**. Las fórmulas SRI no cambian;
solo deja de dividirse el precio entre 1.15.

Aplica a la pantalla y al XML al mismo tiempo, por lo que los totales del comprobante
siguen coincidiendo con lo que se muestra. **Proforma y Nota de Crédito no cambian**
(Notas de Crédito siguen leyendo el precio tal cual; sobre facturas antiguas —que se
emitieron con precio con IVA incluido— acreditan ~15 % menos). Los RIDE regenerados de
facturas antiguas mantienen sus líneas originales.

Cliente y servidor (ApiServer `/api/facturas` y modo offline) deben correr el mismo
build, ya que ambos usan la misma fórmula de totales.

Detalle técnico: `docs/convencion_precio_factura.md`.
