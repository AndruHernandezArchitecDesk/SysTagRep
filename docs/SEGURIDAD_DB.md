# Seguridad BD — Contraseña `~/.vendex/db.properties`

## Resumen
- `db.password` se guarda **cifrada AES/GCM** (`sal:iv:cifrado`) via `SecureConfigStore`/`Cifrado` con master key del SO. Formato `Base64(sal):Base64(iv):Base64(cifrado)` HKDF HmacSHA256 (keyring) + fallback PBKDF2 legacy.
- **Fase 1 completa implementada**: `security/MasterKeyManager.java:22` resuelve master key 32B con orden **keyring OS (DPAPI Windows / Keychain macOS / Secret Service Linux via `java-keyring` 1.0.1 + JNA 5.14)** → fallback `~/.vendex/.master.key` (0600) + backup cifrado `~/.vendex/.master.key.bak` (ENC:). `Cifrado.java:27` cifra con `MasterKeyManager`, descifra dual (master→legacy) para migración transparente. `SecureConfigStore.java:28` migra `claro→cifrado` y `legacy→keyring` vía `migrarTodoSiEsNecesario()`. `ConfigFirma.java:24` migrado a `SecureConfigStore` (hereda keyring). `configuracion_email` queda fuera de keyring (decisión 1) por ser fila compartida.
- Prioridad: `DB_PASSWORD` env > `-Ddb.password` > `~/.vendex/db.properties` (cifrado keyring, no portable) > legacy claro/legacy cifrado (migrado lazy) > `admin` solo en memoria (no se autocrea).
- **No se autocrea** `db.properties` con `admin` al primer arranque. El wizard integrado `src/main/java/com/vendex/MainApp.java:25` + `view/DbSetupWizardView.fxml` lo genera.
- Validación débil: `util/PasswordDebilValidator.java` (`admin`, `postgres`, `password`, `123456`, etc., <8 chars) → solo **warning+log** (`DatabaseConnection.java:56`), no bloquea arranque.
- Flag `-Dvendex.keyring.disabled=true` fuerza fallback file (CI/headless). Timeout keyring 5s no bloquea `MainApp.start()`.

## Primer arranque

### Instalación nueva (primera PC del local)
1. Borra `~/.vendex/db.properties` si existe con `admin` (o instala limpio).
2. Ejecuta Vendex → wizard “Configuración de Base de Datos” aparece antes del login.
3. Selecciona **Instalación nueva — generar contraseña fuerte** → click “Regenerar” genera 24 chars (`A-Z,a-z,0-9,-_!@#$%&*`, `SecureRandom` en `DbSetupWizardController.java:19`).
4. Copia con “Copiar” y **guárdala en gestor de contraseñas** (se muestra una sola vez, como API keys cloud).
5. Completa `JDBC URL` (`jdbc:postgresql://localhost:5432/dbVendex` en host `192.168.1.7`, `jdbc:postgresql://192.168.1.7:5432/dbVendex` en clientes) y usuario (`app_vendex` mínimo privilegio — `postgres` solo legacy).
6. Si es primera vez con `app_vendex`, ejecuta **como postgres** el script de privilegios:
   ```bash
   psql -h 127.0.0.1 -U postgres -d dbVendex -f src/main/resources/sql/migracion_minimo_privilegio_20260920.sql
   # ver docs/permisos_bd.md §1-3
   ```
7. Click **Probar conexión** → valida `DriverManager.getConnection()` sin exponer password en logs.
8. **Guardar (cifrado keyring)** → escribe `~/.vendex/db.properties` con `db.password=cifrado` (no portable). Actualiza `DatabaseConnection` en memoria.
9. **En Postgres** (una sola vez, superuser):
   ```sql
   ALTER ROLE app_vendex WITH PASSWORD 'la-generada-24-chars';
   -- rotar también postgres después de migrar
   ALTER ROLE postgres WITH PASSWORD 'nueva-superuser-24';
   ```

### PC adicional (mismo local, BD compartida)
1. Instala Vendex en nueva PC → wizard aparece.
2. Selecciona **PC adicional — pegar contraseña existente** → pega la que generaste en la primera PC.
3. Ajusta `JDBC URL` a remota (`192.168.1.7`) y usuario.
4. Probar + Guardar → se cifra localmente con clave de esa máquina (archivo no portable).

## Migración instalaciones existentes con `admin` en claro
- Al arrancar con `db.password=admin` en claro, `DbConfig.java:47` detecta y `SecureConfigStore.migrarSiEsNecesario()` lo re-cifra automáticamente en disco (lazy, siguiente `cargar()` ya lee cifrado). No requiere acción manual para cifrado.
- Además `DatabaseConnection.java:56` loguea `WARNING` + `MainApp.java:30` muestra `Alert WARNING` no bloqueante: “Contraseña insegura… Contacta soporte”. Continúa conectando para no romper operación.
- **Rotación recomendada**: ver abajo. No es bloqueante por decisión (punto 1).

## Rotación de contraseña (todas las PCs)
> BD es secreto compartido — hay que rotar en **cada PC** del local + Postgres. Rol por defecto `app_vendex` (ver `docs/permisos_bd.md`).

1. En **una PC** (cualquiera con acceso superuser), genera nueva: borra `~/.vendex/db.properties` o usa wizard → Regenerar → Copiar nueva.
2. En Postgres (como `postgres`):
   ```sql
   ALTER ROLE app_vendex WITH PASSWORD 'nueva-24-chars';
   -- opcional rotar superuser
   -- ALTER ROLE postgres WITH PASSWORD 'nueva-24-super';
   ```
3. Guarda en esa PC (wizard → usuario `app_vendex`, Probar y Guardar cifrado keyring). Verifica login.
4. **En cada otra PC**:
   - Borra o edita `~/.vendex/db.properties` o simplemente reinicia Vendex y deja que wizard pida nueva → pega la misma `nueva-24-chars` → Probar → Guardar.
   - Alternativa sin wizard: `DB_PASSWORD=nueva-24-chars` env no persiste; mejor usar wizard para cifrar en disco.
5. **Valida** que `cat ~/.vendex/db.properties` no contiene la nueva en claro (solo `sal:iv:cifrado`).
6. **Documenta** quién y cuándo rotó; comunica a soporte.

> Env override: `DB_PASSWORD` o `-Ddb.password` tiene prioridad sobre archivo, útil para Docker/tests sin archivo, pero no reemplaza rotación en archivo.

## Pruebas manuales del lineamiento
- Copiar `~/.vendex/db.properties` cifrado con keyring a otro equipo → **no descifra** (`AEADBadTagException`, verificado 2026-09-20 con dos `~/.vendex/.master.key` distintos). Con Fase 1 legacy sí descifraba — corregido.
- Flujo PC adicional: pegar compartida → cifra correctamente en nuevo equipo con su propia master key y conecta.

## Archivos clave
- `config/DbConfig.java:33-143` — descifra/cifra keyring, migra claro+legacy, env fallback, no autocrea admin
- `config/DatabaseConnection.java:8-69` — warning débil, no log secreto
- `util/PasswordDebilValidator.java` — lista débiles
- `controller/DbSetupWizardController.java` + `view/DbSetupWizardView.fxml` — wizard integrado `MainApp.java:25`
- `util/SecureConfigStore.java` + `util/Cifrado.java` — cifrado keyring + dual-read legacy
- `security/MasterKeyManager.java` + `security/KeyringSecretProvider.java` + `security/FallbackFileProvider.java` — provider keyring DPAPI/Keychain/libsecret (5s timeout) → fallback `~/.vendex/.master.key` (0600) + backup `~/.vendex/.master.key.bak` (ENC: legacy, decisión 4)
- `config/GeminiConfig.java` / `NvidiaConfig.java` — migrados a `migrarTodoSiEsNecesario()` (heredan keyring)
- `util/ConfigFirma.java` — migrado a `SecureConfigStore` (hereda keyring)

## Backup y recuperación (decisión 4)
- Al generar fallback master key se crea `~/.vendex/.master.key.bak` con contenido `ENC:<cifrado legacy>` + comentario. Guardar `master.key` y `.bak` en lugar seguro. Si reinstalas OS/cambias usuario Windows (DPAPI pierde SID), restaura `.master.key` o re-ingresa password en wizard (re-cifra con nueva master key). `configuracion_email` excluida de keyring por ser compartida.

## Fase 1 completa — completada 2026-09-20
- `pom.xml` añade `jna 5.14.0`, `jna-platform`, `java-keyring 1.0.1`. `Cifrado.SECRETO` queda solo para `encriptarLegacy`/`desencriptarLegacy` y backup, no para nuevas escrituras.
