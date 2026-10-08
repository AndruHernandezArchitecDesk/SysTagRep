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
