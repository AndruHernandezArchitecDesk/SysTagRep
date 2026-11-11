---
name: licencia-central-cliente2
description: Use when the user asks about Cliente2 machine code, CENTRAL license issuance, registration, renewal, or reports "código inválido" or "no registrada". Follow the validated Cliente2 flow and verify every exact character before issuing.
---

# Licencia CENTRAL para Cliente2

Use este procedimiento solo para la instalación Cliente2 de Vendex, que se conecta a la base central `dbTag` en `192.168.1.4:5432`.

## Datos de referencia verificados

- El código confirmado por la salida de arranque/registro el 2026-11-11 fue `bZSOyb4VlFsQ4I/xwNEyS9RY` (`O` mayúscula y `l` minúscula en esa secuencia). Debe verificarse de nuevo en Cliente2 antes de emitir otra licencia; nunca reconstruirlo de memoria.
- El registro exitoso confirmado el 2026-11-11 11:10:52 fue modalidad `CENTRAL`, IA incluida, todos los roles, capacidad `0` (sin límite definido), vencimiento `2026-12-10`.
- La clave actualmente registrada se conserva en `/home/andru/Escritorio/Licencia_cliente2_vigente.md`. Leerla de allí si hace falta recuperarla; no volver a emitirla si sigue vigente y registrada.
- Archivos auxiliares en el Escritorio: `/home/andru/Escritorio/Registrar_licencia_cliente2.bat`, `/home/andru/Escritorio/diferencias_dbVendex_a_dbTag_20261101.sql` y `Licencia_cliente2_vigente.md`.

## Reglas que no se deben saltar

1. **Copiar el código desde Cliente2**, desde `run.bat` con el build que imprime `CODIGO DE MAQUINA PARA COPIAR` o desde la pantalla de activación. Comparar carácter por carácter. `0`/`O`, `1`/`I`/`l`, y mayúsculas/minúsculas cambian la firma.
2. La licencia v2 incluye un payload Base64 sensible a mayúsculas/minúsculas. Copiar la clave emitida exactamente; no convertirla a mayúsculas ni reescribirla manualmente.
3. Para Cliente2, no basta con generar una clave: debe registrarse el hash en `dbTag`. **Una licencia sin fila en `licencia_emitida` no sirve para el arranque remoto.**
4. El código generador debe usar el mismo secreto HMAC que las instalaciones. En este caso se validó el registro emitiendo desde Cliente2; preferir el BAT/CLI ejecutado desde ese mismo equipo y usar la clave que imprime.
5. Cliente2 no necesita instalar PostgreSQL ni `psql.exe`. El generador Java registra vía JDBC en el servidor. El script SQL se aplica una sola vez en el servidor que tiene PostgreSQL (o desde una estación administrativa que tenga `psql`).
6. No guardar contraseñas de PostgreSQL en BAT, Markdown, repositorio, comandos versionados ni historial. Introducirlas solo en el prompt oculto del BAT o mediante una variable de entorno temporal.

## Flujo para emitir/registrar o renovar

1. Verificar si ya existe una licencia central vigente registrada para el código de máquina actual. Consultar primero `Licencia_cliente2_vigente.md` y, cuando haya acceso, el registro central. No emitir otra clave solo porque el usuario no encuentre el archivo.
2. Confirmar el código de máquina literal de Cliente2 y la nueva fecha deseada en formato `YYYYMMDD`. Confirmar modalidad `CENTRAL`, IA y roles necesarios; no asumir que una fecha anterior sigue siendo la solicitada.
3. Si las tablas centrales no existen, aplicar `/home/andru/Escritorio/diferencias_dbVendex_a_dbTag_20261101.sql` conectado específicamente a `dbTag`. El SQL comprueba `current_database() = 'dbTag'` y es reejecutable. Esto requiere PostgreSQL en el servidor/estación de administración, nunca en Cliente2.
4. En Cliente2, ejecutar el BAT actualizado `/home/andru/Escritorio/Registrar_licencia_cliente2.bat` junto al JAR actualizado. Verificar que su `MACHINE_CODE` coincide exactamente con el código del paso 1 y que `DB_URL` apunta a `jdbc:postgresql://192.168.1.4:5432/dbTag`.
5. En el prompt, elegir un **usuario de PostgreSQL** con `INSERT` en `licencia_emitida` y uso de la secuencia. Usar `app_vendex` si tiene los permisos concedidos por el SQL; de lo contrario, la cuenta DBA y su contraseña real. La contraseña de PostgreSQL no es la contraseña de inicio de sesión de Vendex ni se puede asumir que sea `admin`.
6. Esperar el resultado. Solo considerar registrado si el proceso termina con código 0 y muestra `License Key (v2 CENTRAL registrada)`, `Registro: licencia_emitida` y la fecha de vencimiento solicitada.
7. Actualizar `Licencia_cliente2_vigente.md` y el TXT del Escritorio con el código literal, la clave exacta emitida, fecha, modalidad, IA, roles, capacidad y estado registrado. No sustituir la clave por una generada en otra máquina.
8. Activar en Cliente2 pegando exactamente la clave que imprimió el registro exitoso. Confirmar que la aplicación llega al login y consultar el badge/validación central.

## Diagnóstico por mensaje

- `FATAL: password authentication failed for user ...`: el registro **no ocurrió**. Corregir usuario/contraseña de PostgreSQL y volver a ejecutar el registro. No regenerar claves para resolver una contraseña incorrecta.
- `Código de licencia inválido`: el chequeo local de firma falló. Antes de hacer nada, copiar de nuevo el código mostrado en Cliente2 y la clave del registro exitoso; comprobar caja y `0/O`, `1/I/l`. Si se usó una clave emitida en otra máquina, emitir/registrar desde Cliente2 para usar su secreto HMAC y confirmar que el servidor usa el secreto compartido esperado.
- `La licencia no está registrada en el servidor central` / `NO_REGISTRADA`: la firma local puede ser válida, pero no existe el hash de esa misma clave en `licencia_emitida`. Ejecutar el flujo de registro y activar con la clave que devuelve ese proceso.
- `MODALIDAD_INCORRECTA`, `REVOCADA` o `VENCIDA`: revisar la fila central y emitir/renovar las dimensiones correspondientes; no alterar caracteres de la clave.
- Si la interfaz solo dice `Código de licencia inválido`, recopilar el código de máquina literal mostrado en Cliente2, la salida completa del BAT y el motivo central del log. No adivinar caracteres ni entregar una nueva clave antes de esa comprobación.

## Comando manual equivalente

Solo si el BAT no está disponible y el JAR actualizado se encuentra en la carpeta actual:

```bat
java -cp "Vendex-2.0-SNAPSHOT.jar" com.vendex.util.LicenseGenerator "<CODIGO_COPIADO_DE_CLIENTE2>" <YYYYMMDD> --central --ia --registrar-central --db-url=jdbc:postgresql://192.168.1.4:5432/dbTag --db-user=<USUARIO_POSTGRES>
```

Definir `DB_PASSWORD` temporalmente en el entorno antes de ejecutar el comando. No poner la contraseña directamente en el comando o en este archivo.
