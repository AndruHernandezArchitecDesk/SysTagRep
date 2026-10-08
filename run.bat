@echo off
title Vendex
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java no encontrado. Instala JRE/JDK 17+ de https://adoptium.net ^(marca Add to PATH^).
  pause
  exit /b 1
)
java -version 2>&1
echo.
if not exist "Vendex-2.0-SNAPSHOT.jar" (
  echo [ERROR] No se encontro Vendex-2.0-SNAPSHOT.jar junto a run.bat.
  pause
  exit /b 1
)
echo Lanzando Vendex...
java --enable-native-access=ALL-UNNAMED -Xmx1024m -Xms128m -jar "Vendex-2.0-SNAPSHOT.jar"
echo.
echo La app termino ^(codigo %errorlevel%^). Si no viste la ventana de Vendex, toma foto de esta pantalla.
pause
