@echo off
REM backup_vendex.bat — Backup Vendex Windows (pg_dump custom + gpg + copia offsite)
REM Uso: backup_vendex.bat  (ejecuta via Vendex al cierre de caja o manual con doble-click)
REM Requiere pg_dump y gpg en PATH o configurados en ~/.vendex/backup.properties
REM Destino local y offsite configurables en Vendex: Administracion -> Respaldos

setlocal enabledelayedexpansion

REM Fecha formato yyyyMMdd_HHmmss
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do set datetime=%%I
if defined datetime (
  set FECHA=%datetime:~0,8%_%datetime:~8,6%
) else (
  for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set d=%%c%%a%%b
  for /f "tokens=1-2 delims=: " %%a in ('time /t') do set t=%%a%%b
  set FECHA=%d%_%t%
  set FECHA=%FECHA: =0%
)

REM Intentar leer backup.dir de ~/.vendex/backup.properties si existe
set DESTINO=C:\Vendex\backups
set OFFSITE=
if exist "%USERPROFILE%\.vendex\backup.properties" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%USERPROFILE%\.vendex\backup.properties") do (
    if "%%A"=="backup.dir" set DESTINO=%%B
    if "%%A"=="backup.offsite.path" set OFFSITE=%%B
  )
)
REM fallback si vacio
if "%DESTINO%"=="" set DESTINO=C:\Vendex\backups

if not exist "%DESTINO%" mkdir "%DESTINO%" 2>nul

REM Resolver pg_dump path (intentar leer de backup.properties también)
set PG_DUMP=pg_dump
if exist "%USERPROFILE%\.vendex\backup.properties" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%USERPROFILE%\.vendex\backup.properties") do (
    if "%%A"=="backup.pg_dump.path" set PG_DUMP=%%B
  )
)

echo [%date% %time%] Iniciando backup Vendex -> %DESTINO%
echo Host: localhost DB: dbVendex Usuario: app_vendex (o postgres legacy)

REM pg_dump custom — requiere PGPASSWORD env (lo resuelve Vendex Java; para .bat standalone pedir)
if "%PGPASSWORD%"=="" (
  echo ADVERTENCIA: PGPASSWORD no definido. Si pg_dump pide password, defina:
  echo   set PGPASSWORD=su_password_app_vendex
  echo   %PG_DUMP% -h localhost -U app_vendex -d dbVendex --format=custom --compress=9 -f "%DESTINO%\vendex_%FECHA%.dump"
)

"%PG_DUMP%" -h localhost -U app_vendex -d dbVendex --format=custom --compress=9 -f "%DESTINO%\vendex_%FECHA%.dump"
if errorlevel 1 (
  echo ERROR: pg_dump fallo exit=%errorlevel%
  REM intentar con usuario postgres legacy
  echo Reintentando con usuario postgres...
  "%PG_DUMP%" -h localhost -U postgres -d dbVendex --format=custom --compress=9 -f "%DESTINO%\vendex_%FECHA%.dump"
  if errorlevel 1 (
    echo ERROR: pg_dump fallo con ambos usuarios. Revisar credenciales y pg_hba.conf
    exit /b 1
  )
)

REM verificacion no vacio
for %%F in ("%DESTINO%\vendex_%FECHA%.dump") do set SIZE=%%~zF
if "%SIZE%"=="0" (
  echo ERROR: backup vacio
  exit /b 1
)
echo Backup creado: %DESTINO%\vendex_%FECHA%.dump (%SIZE% bytes)

REM cifrado gpg si disponible
where gpg >nul 2>&1
if %errorlevel%==0 (
  echo Cifrando con gpg AES256...
  REM passphrase debe estar en archivo o env BACKUP_PASSPHRASE
  if "%BACKUP_PASSPHRASE%"=="" (
    echo ADVERTENCIA: BACKUP_PASSPHRASE no definido — se deja sin cifrar. Definir en Vendex: Administracion -> Respaldos genera passphrase.
  ) else (
    gpg --symmetric --cipher-algo AES256 --batch --yes --passphrase "%BACKUP_PASSPHRASE%" --output "%DESTINO%\vendex_%FECHA%.dump.gpg" "%DESTINO%\vendex_%FECHA%.dump"
    if %errorlevel%==0 (
      echo Cifrado OK: vendex_%FECHA%.dump.gpg
      del "%DESTINO%\vendex_%FECHA%.dump"
    ) else (
      echo ADVERTENCIA: gpg fallo — se conserva sin cifrar
    )
  )
) else (
  echo ADVERTENCIA: gpg no encontrado — backup sin cifrar
)

REM copia offsite a otra PC en red si configurado
if not "%OFFSITE%"=="" (
  echo Copiando offsite -> %OFFSITE%...
  if not exist "%OFFSITE%" mkdir "%OFFSITE%" 2>nul
  if exist "%DESTINO%\vendex_%FECHA%.dump.gpg" (
    copy /Y "%DESTINO%\vendex_%FECHA%.dump.gpg" "%OFFSITE%\" >nul
  ) else (
    copy /Y "%DESTINO%\vendex_%FECHA%.dump" "%OFFSITE%\" >nul
  )
  if %errorlevel%==0 (
    echo Offsite OK
  ) else (
    echo ADVERTENCIA: fallo copia offsite. Verificar que %OFFSITE% sea accesible: dir "%OFFSITE%"
  )
) else (
  echo Offsite no configurado (backup.offsite.path vacio) — configurar \\OTRA-PC\VendexBackups en backup.properties
)

echo [%date% %time%] Backup finalizado
exit /b 0
