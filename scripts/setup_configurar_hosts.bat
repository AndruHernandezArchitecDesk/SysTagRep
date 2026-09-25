@echo off
REM setup_configurar_hosts.bat — Configura vendex-db en C:\Windows\System32\drivers\etc\hosts (distribuido)
REM Uso: setup_configurar_hosts.bat [IP_DEL_HOST]
REM   Si no se pasa IP, pide interactivamente. Requiere ejecutar como Administrador.
REM Recomendado: admin ejecuta en cada PC del cliente (host y clientes) tras instalar Vendex.
REM La IP varia por cliente (ej. 192.168.1.7, 192.168.0.100, etc.). Debe coincidir con reserva DHCP del host.

setlocal enabledelayedexpansion

set IP=%~1
if "%IP%"=="" (
  echo Host central vendex-db - Configuracion hosts distribuido
  echo IP actual de vendex-db (si existe):
  findstr /i "vendex-db" "%SystemRoot%\System32\drivers\etc\hosts" 2>nul
  echo.
  set /p IP="Ingrese IP del host central (ej. 192.168.1.7): "
)
if "%IP%"=="" (
  echo ERROR: IP vacia. Uso: %~nx0 192.168.1.7
  exit /b 1
)

REM Validacion basica IPv4
echo %IP% | findstr /r "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
  echo ERROR: IP no parece IPv4 valida: %IP%
  exit /b 1
)

set HOSTS=%SystemRoot%\System32\drivers\etc\hosts
set BACKUP=%HOSTS%.vendex-bak

REM Necesita admin
net session >nul 2>&1
if errorlevel 1 (
  echo ERROR: Debe ejecutar como Administrador (click derecho - Ejecutar como administrador).
  exit /b 1
)

REM Backup
if not exist "%BACKUP%" (
  copy /Y "%HOSTS%" "%BACKUP%" >nul
  echo Backup creado: %BACKUP%
)

REM Eliminar linea previa vendex-db si existe (mantener otras)
set TMP=%TEMP%\hosts.tmp
> "%TMP%" (
  for /f "usebackq delims=" %%L in ("%HOSTS%") do (
    echo %%L | findstr /i "vendex-db" >nul
    if errorlevel 1 echo %%L
  )
)
REM Anadir nueva entrada
>> "%TMP%" echo %IP% vendex-db  # Vendex host central — admin: %date% — ver docs/hosts_setup.md

copy /Y "%TMP%" "%HOSTS%" >nul
del "%TMP%" 2>nul

echo OK: %HOSTS% actualizado -> %IP% vendex-db
echo.

REM Flush DNS y prueba
ipconfig /flushdns >nul
echo Probando resolucion...
ping -n 1 vendex-db >nul 2>&1
if errorlevel 1 (
  echo ADVERTENCIA: ping vendex-db fallo. Verifique que la IP sea alcanzable y que no haya firewall.
  ping -n 1 vendex-db
) else (
  echo vendex-db resuelve correctamente.
  ping -n 1 vendex-db | findstr /i "vendex-db"
)

echo.
echo Probando Postgres (si psql en PATH):
where psql >nul 2>&1
if %errorlevel%==0 (
  echo   psql -h vendex-db -U app_vendex -d dbVendex -c "select 1"
  psql -h vendex-db -U app_vendex -d dbVendex -c "select 1" 2>&1 | head -n 5
) else (
  echo   (psql no en PATH — probar manual: psql -h vendex-db -U app_vendex -d dbVendex -c "select 1")
)

echo.
echo Listo. En clientes, Vendex debe usar JDBC URL: jdbc:postgresql://vendex-db:5432/dbVendex
echo En host, Vendex usa: jdbc:postgresql://localhost:5432/dbVendex
echo Ver docs/hosts_setup.md para detalles por cliente (IP varia).
exit /b 0
