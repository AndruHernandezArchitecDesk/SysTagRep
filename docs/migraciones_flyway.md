# Migraciones Flyway — Versionado (LINEAMIENTO_MIGRACIONES_FLYWAY)

> `db/migration/V1__baseline.sql` desde `pg_dump --schema-only` referencia (Opción A). Validación **solo local**. Flyway corre con `postgres` migrador, baseline manual por cliente.

## Estructura

```
src/main/resources/db/migration/
 V1__baseline.sql                    # placeholder — reemplazar con pg_dump real antes de prod
 V2__caja.sql ... V15__certificado_estado.sql  # artesanales tal cual (IF NOT EXISTS)
 V16__grant_minimo_privilegio.sql
 V17__login_intento_bloqueo.sql
 V18__roles_permisos_auditoria.sql
 V19__comprobante_pendiente_sri.sql
 V20__fix_multipc_numeracion.sql ... V23__numeracion.sql
```

`V1` es baseline consolidado (42 tablas). Nunca editar `V*` aplicada — checksum; corregir con `V24__corrige_...sql`.

## Arranque

`MainApp.java:30` `DatabaseConnection.initFromConfig()` → `DatabaseConnection.migrateWithFlywayIfAvailable()` (usa `PostgresConfig.getPassword()` env `POSTGRES_PASSWORD` > `~/.vendex/postgres.properties` cifrado > keyring `postgres.password`). Si no hay password (clientes), se omite y `ensure*` fallback idempotente sigue. Si hay password (host central), `Flyway.configure().dataSource(vendex-db, postgres, pgPass).locations("classpath:db/migration").baselineOnMigrate(false).validateOnMigrate(true).load().migrate()` con lock PG `flyway_schema_history` (multi-PC safe).

Configurar password postgres **solo en host central** (no en clientes):
- UI: `Administración → Postgres migrador (Flyway)` → guarda cifrado `~/.vendex/postgres.properties` + keyring.
- O env: `POSTGRES_PASSWORD=xxx` para baseline manual.

## Baseline manual por cliente (una vez)

En cada instalación existente (tiene datos):
```bat
REM En host central como postgres, con Vendex cerrado
set POSTGRES_PASSWORD=su_postgres_superuser
flyway -url=jdbc:postgresql://vendex-db:5432/dbVendex -user=postgres -password=%POSTGRES_PASSWORD% -locations=filesystem:src/main/resources/db/migration baseline -baselineVersion=1 -baselineDescription="pre-flyway referencia"
```
Esto marca `V1` como ya aplicado sin re-ejecutar. De ahí `flyway migrate` aplicará `V2..Vn`. Nuevas instalaciones sin baseline corren `V1..Vn` desde cero.

Si Flyway al arrancar loguea `already exists` → ejecutar baseline y reiniciar.

## Solo local

Sin Docker CI. Probar local: `psql -c "DROP DATABASE vendex_test; CREATE DATABASE vendex_test"` → `flyway -url=.../vendex_test migrate` debe terminar sin error. HSQLDB tests (`ClienteDAOTest`) siguen para unit; migraciones solo contra PG real local.

## Si falla migración

- Solo esquema: `V24__corrige_V3.sql` hacia adelante.
- Datos: restaurar `pg_dump -Fc` pre-migración (`RECUPERACION_DR.md`) + `V*` corregida. Hacer backup manual antes de `V*` riesgosa.

## Archivos

- `pom.xml: org.flywaydb:flyway-core 10.17.0 + flyway-database-postgresql`
- `config/PostgresConfig.java` — env > keyring > `postgres.properties` cifrado
- `config/DatabaseConnection.java:migrateWithFlywayIfAvailable()` + 8 `ensure*` (deprecados progresivo)
- `MainApp.java:30` orden
- `scripts/flyway_baseline.bat` helper
