# Advertencias del portable en clientes remotos

## Empaquetado multi-release

El JAR sombreado conserva `META-INF/versions/` y declara `Multi-Release: true`.
Se excluyen los descriptores `module-info.class`, no las implementaciones versionadas.
Esto permite que Log4j use su `StackLocator` para Java 9+ y elimina el aviso sobre
location-based features. La verificación del artefacto incluye el proveedor Bouncy Castle.

## Flyway

Un cliente cuya BD es remota no ejecuta Flyway al arrancar, incluso si conserva una
contraseña antigua de `postgres` en el keyring o en `postgres.properties`. La migración
se mantiene en el host central con conexión local y su contraseña de migrador.
Los mecanismos `ensure*` existentes mantienen su comportamiento.

No hace falta cambiar la contraseña de la conexión normal ni borrar configuración
para corregir el intento de migración desde un cliente.

## Correo SMTP compartido

La contraseña SMTP se guarda en la BD, pero su clave de instalación se importa en
cada PC. Actualizar el JAR no permite recuperar una clave que falta.

En cliente2:

1. Obtén del administrador la `vendex-master-key` existente de la PC donde se
   configuró el correo. Puede obtenerse desde el asistente de BD de esa PC o de su
   respaldo de clave de instalación.
2. Abre **Administración → Correo Electrónico → Importar clave de instalación**.
3. Pega esa clave Base64. El programa comprueba que descifre la contraseña SMTP
   antes de reemplazar la clave local; si no coincide, no la modifica.
4. Pulsa **Probar conexión** y luego **Enviar correo de prueba**.

No se debe generar una clave nueva para intentar descifrar una contraseña anterior.
Si la contraseña antigua se cifró con una clave exclusiva de otra máquina y no con
la clave compartida, hay que volver a guardar la contraseña SMTP desde una PC con
la clave de instalación correcta e importarla en los clientes.

Se conserva la lectura de SMTP cifrado con formatos locales/legacy cuando sea
posible descifrarlos. Los nuevos guardados requieren la clave de instalación,
evitando crear una contraseña compartida que solo pueda leer una PC.

## Apariencia

La vista previa espera a que el árbol JavaFX termine de enlazarse a la escena.
El cambio de hojas de estilo es atómico y se resuelve la paleta desde la raíz antes
de abrir otros desplegables. Se comprueba navegar repetidamente por Apariencia y
alternar los cuatro temas con desplegables abiertos, sin advertencias de lookups
ni conversiones de `String` a `Paint`/`Color`.

## Verificación

```bash
xvfb-run -a ./mvnw -q -Dnet.bytebuddy.experimental=true \
  -Dtest=AparienciaCssTest,FlywayClienteTest,ConfiguracionEmailCifradoTest,LicenseActivatorInputTest,LicenseManagerTest,EmailServiceTest,CifradoTest test
./mvnw -q package -DskipTests
```

`net.bytebuddy.experimental` es necesario para la versión de Mockito del proyecto
al ejecutar estas pruebas con Java 25; no es una opción del portable.
