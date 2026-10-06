@echo off
REM Reconfigurar-Vendex-BD.bat — Reabre el asistente de conexion a la BD existente (produccion, sin tocar la BD).
REM Uso: doble clic. Pide Servidor/IP + puerto + BD + usuario + clave, con Probar conexion.
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if exist "target\Vendex-2.0-SNAPSHOT.jar" (
  java --enable-native-access=ALL-UNNAMED -Xmx1024m -Dvendex.configDb=true -jar "target\Vendex-2.0-SNAPSHOT.jar"
) else if exist "Vendex-2.0-SNAPSHOT.jar" (
  java --enable-native-access=ALL-UNNAMED -Xmx1024m -Dvendex.configDb=true -jar "Vendex-2.0-SNAPSHOT.jar"
) else (
  echo [ERROR] No se encontro Vendex-2.0-SNAPSHOT.jar junto a este .bat ni en target\.
  echo Copia este .bat a la carpeta donde esta el JAR e intenta de nuevo.
  pause
  exit /b 1
)
