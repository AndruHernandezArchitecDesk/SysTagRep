@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

set VERSION=2.0.0
set JAR=Vendex-2.0-SNAPSHOT.jar
set NOMBRE=Vendex
set ICON=src\main\resources\img\app.ico

echo ============================================
echo  Vendex - Instalador PRODUCCION (sin base de datos)
echo ============================================
echo  Este MSI solo instala la app. La BD ya existe en el servidor.
echo  Al primer arranque pide Servidor/IP + puerto + BD + usuario + clave
echo  con boton Probar conexion. No crea tablas ni roles.
echo.

where jpackage >nul 2>nul
if errorlevel 1 (
    echo [ERROR] jpackage no encontrado.
    echo   Instala un JDK 17+ y asegurate de que su carpeta bin este en el PATH.
    exit /b 1
)

echo [0/2] Compilando el proyecto...
call mvnw.cmd clean package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion de Maven.
    exit /b 1
)

if not exist target\%JAR% (
    echo [ERROR] No se genero %JAR% en target.
    exit /b 1
)

echo [1/2] Generando instalador MSI PRODUCCION sin BD (requiere WiX Toolset 3.11+)...
jpackage --input target ^
  --name "%NOMBRE%" ^
  --app-version %VERSION% ^
  --vendor "Vendex Repuestos" ^
  --description "Vendex - cliente produccion. Se conecta a la BD existente del servidor (sin instalar PostgreSQL)." ^
  --main-jar %JAR% ^
  --main-class com.vendex.Launcher ^
  --icon "%ICON%" ^
  --type msi ^
  --win-menu --win-menu-group "Vendex Repuestos" ^
  --win-shortcut ^
  --java-options "-Xmx1024m -Xms128m" ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --dest dist
if errorlevel 1 goto fallback

echo [OK] Instalador generado: dist\%NOMBRE%-%VERSION%.msi
echo      Produccion sin BD: instalalo en cada PC cliente.
echo      Primer arranque: ingresa IP del servidor + Probar conexion + Guardar.
echo      Para cambiar de servidor luego: ejecuta Reconfigurar-Vendex-BD.bat
echo      (abre la app con el asistente de conexion).
exit /b 0

:fallback
echo [AVISO] No se genero el MSI (posiblemente falta WiX Toolset).
echo [2/2] Generando app-image portable (no requiere WiX)...
jpackage --input target ^
  --name "%NOMBRE%" ^
  --app-version %VERSION% ^
  --main-jar %JAR% ^
  --main-class com.vendex.Launcher ^
  --icon "%ICON%" ^
  --type app-image ^
  --win-menu ^
  --win-shortcut ^
  --java-options "-Xmx1024m -Xms128m" ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --dest dist
if errorlevel 1 (
    echo [ERROR] jpackage fallo en la app-image.
    exit /b 1
)
powershell -Command "Compress-Archive -Path 'dist\%NOMBRE%' -DestinationPath 'dist\%NOMBRE%-portable.zip' -Force"
echo [OK] Portable generado: dist\%NOMBRE%-portable.zip
echo      Envia el zip al cliente; el descomprime y abre %NOMBRE%\%NOMBRE%.exe
exit /b 0
