@echo off
REM flyway_baseline.bat — Baseline manual por cliente (Flyway, solo host central)
REM Uso: flyway_baseline.bat [IP]
REM Requiere POSTGRES_PASSWORD env y flyway CLI en PATH o mvn flyway:baseline
REM IP varia por cliente (vendex-db via hosts), default vendex-db
setlocal
set HOST=%~1
if "%HOST%"=="" set HOST=vendex-db
if "%POSTGRES_PASSWORD%"=="" (
  echo ADVERTENCIA: POSTGRES_PASSWORD no definido.
  echo Defina: set POSTGRES_PASSWORD=su_postgres
  echo O configure en Vendex: Administracion ^> Postgres migrador
)
echo Baseline Flyway en %HOST% dbVendex V1...
REM Intenta via mvn flyway plugin si CLI no esta instalado
where flyway >nul 2>&1
if %errorlevel%==0 (
  flyway -url=jdbc:postgresql://%HOST%:5432/dbVendex -user=postgres -password=%POSTGRES_PASSWORD% -locations=filesystem:src/main/resources/db/migration baseline -baselineVersion=1 -baselineDescription="pre-flyway referencia"
) else (
  echo flyway CLI no encontrado, usando mvn:
  mvn -q -Dflyway.url=jdbc:postgresql://%HOST%:5432/dbVendex -Dflyway.user=postgres -Dflyway.password=%POSTGRES_PASSWORD% -Dflyway.locations=filesystem:src/main/resources/db/migration flyway:baseline
)
if %errorlevel%==0 (
  echo Baseline OK. Reinicie Vendex para migrar V2..Vn.
) else (
  echo Baseline fallo. Verifique POSTGRES_PASSWORD y que V1__baseline.sql exista.
)
