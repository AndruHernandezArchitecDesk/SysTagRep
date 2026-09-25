# Runbook — Recuperación ante Desastre (PostgreSQL dbVendex)

> **Guardar copia de este documento FUERA del host central** (repo privado, Drive, impresión). Si el host se pierde y este runbook solo está ahí, es inútil. La IP del host varía por cliente; usar siempre `vendex-db` (ver `docs/hosts_setup.md`).

**Última revisión:** 2026-09-24 — RPO 24h (backup diario `pg_dump` custom + GPG en cierre de caja).  
**Host central:** `vendex-db` → BD `dbVendex` — Rol app `app_vendex` (ver `docs/permisos_bd.md` y `sql/migracion_minimo_privilegio_20260920.sql`). IP real por cliente ej. `192.168.1.7` (configurar via `hosts` + reserva DHCP).  
**Offsite:** otra PC en red `\\backup-pc\VendexBackups` o `\\OTRA-PC\VendexBackups` (configurable en `~/.vendex/backup.properties` `backup.offsite.path`).  
**Notificación falla:** `andresrockfull@gmail.com` ( `~/.vendex/backup.properties` `backup.email.destino` ).  
**Cifrado backup:** `gpg --symmetric AES256`, passphrase en `SecureConfigStore` Tier1 `backup.passphrase` (`~/.vendex/secrets/backup.passphrase.enc` + keyring DPAPI).  
**Archivo local:** `C:\Vendex\backups\vendex_YYYYMMDD_HHMMSS.dump.gpg` — retención GFS 7 diarios / 4 semanales / 12 mensuales.

---

## 0. Pre-requisito: resolución `vendex-db` (hosts distribuido)

Todas las PCs deben resolver `vendex-db` a la IP del host central via `C:\Windows\System32\drivers\etc\hosts` (ver `docs/hosts_setup.md` y `scripts/setup_configurar_hosts.bat`).

```
# C:\Windows\System32\drivers\etc\hosts (ejecutar Notepad como admin)
192.168.1.7 vendex-db   # IP real del host en ESTE cliente
```

- Host: `jdbc:postgresql://localhost:5432/dbVendex`
- Clientes: `jdbc:postgresql://vendex-db:5432/dbVendex`

Cambiar de host = editar 1 línea `hosts` por PC (admin), no N `db.properties`.

---

## 1. Pérdida total del host (disco dañado / robo / borrado)

### Paso 1 — Nuevo equipo Windows (puede ser cualquier PC de la red — sin comprar si no hay presupuesto)
1. Instalar PostgreSQL **misma versión** que producción (verificar `SELECT version();` previo — si no se sabe, usar 17 LTS). Durante instalación, definir password `postgres` superuser y anotarla.
2. Verificar `pg_dump --version` y `psql --version` en PATH. Si no están, añadir `C:\Program Files\PostgreSQL\17\bin` al PATH.
3. Instalar Gpg4win (https://www.gpg4win.org) y verificar `gpg --version`.

### Paso 2 — Ubicar último backup
- Local (si disco sobrevivió): `C:\Vendex\backups`
- Offsite (otra PC): `\\backup-pc\VendexBackups` — elegir el más reciente `vendex_YYYYMMDD_HHMMSS.dump.gpg`. Verificar tamaño no sea 0.

### Paso 3 — Desencriptar (si tiene .gpg)
```bat
REM passphrase: está en antigua PC en ~/.vendex/secrets/backup.passphrase.enc (keyring) o respaldada manual.
REM Si no tiene respaldo de passphrase, el .gpg es irrecuperable — usar .dump sin cifrar si existe.
gpg --decrypt C:\VendexBackups\vendex_20260923_190000.dump.gpg > C:\temp\restore.dump
```
> **Lección:** Respaldar `backup.passphrase` (exportar desde Vendex: Administración → Respaldos → Generar/Ver estado) en gestor de contraseñas. Sin ella, backups cifrados no sirven.

### Paso 4 — Crear rol y BD
```bat
psql -h localhost -U postgres -c "CREATE ROLE app_vendex WITH LOGIN PASSWORD 'NUEVA_24_CHARS';"
psql -h localhost -U postgres -c "CREATE DATABASE dbVendex OWNER app_vendex;"
REM o si la BD ya existe vacía:
psql -h localhost -U postgres -c "DROP DATABASE dbVendex;"
psql -h localhost -U postgres -c "CREATE DATABASE dbVendex OWNER app_vendex;"
```

### Paso 5 — Restaurar dump custom
```bat
pg_restore -h localhost -U postgres -d dbVendex --clean --if-exists C:\temp\restore.dump
REM Verificar
psql -h localhost -U postgres -d dbVendex -c "SELECT count(*) FROM factura_registro; SELECT count(*) FROM usuarios; SELECT count(*) FROM inventario;"
```
Si restaura tabla individual (borrado accidental):
```bat
pg_restore -h localhost -U postgres -d dbVendex -t factura_registro C:\temp\restore.dump
```

### Paso 6 — Re-aplicar privilegios mínimo privilegio
```bat
psql -h localhost -U postgres -d dbVendex -f src\main\resources\sql\migracion_minimo_privilegio_20260920.sql
psql -h localhost -U postgres -d dbVendex -f src\main\resources\sql\migracion_permisos_granulares_20260922.sql
psql -h localhost -U postgres -d dbVendex -f src\main\resources\sql\migracion_antifuerza_bruta_20260920.sql
```

### Paso 7 — Actualizar `hosts` y reconfigurar solo si cambia IP
1. Asignar al nuevo host una IP fija (reserva DHCP router, ej. `192.168.1.7` u otra disponible).
2. En **cada PC** (incluida la nueva) editar `C:\Windows\System32\drivers\etc\hosts`:
   ```
   192.168.1.TU_NUEVA_IP vendex-db
   ```
   O ejecutar como admin:
   ```bat
   scripts\setup_configurar_hosts.bat 192.168.1.TU_NUEVA_IP
   ```
   Verificar: `ping vendex-db` y `psql -h vendex-db -U app_vendex -d dbVendex -c "select 1"`
3. Solo si hubo rotación de password, en cada PC borrar `~/.vendex/db.properties` o abrir Vendex → Wizard BD:
   - Host: `jdbc:postgresql://localhost:5432/dbVendex` (en host) / `jdbc:postgresql://vendex-db:5432/dbVendex` (clientes)
   - Usuario: `app_vendex`
   - Password: la nueva de 24 chars (pegar misma en todas las PCs → “PC adicional — pegar existente”)
   - Probar conexión → Guardar cifrado.

> **Ventaja `hosts`:** si solo cambió IP y no password, no hace falta tocar `db.properties` en N PCs — basta actualizar `hosts`.

### Paso 8 — Restaurar otros archivos críticos (no viven en BD)
| Archivo | Ubicación original | Respaldar aparte |
|---|---|---|
| Certificado firma `.p12` | `~/.vendex/firma.properties` → `rutaP12` | Copiar el `.p12` a offsite también |
| `vendex-master-key` Tier2 | `~/.vendex/.install-master.key` + `.bak` | Guardado fuera de PCs (wizard export) — sin ella no descifra `configuracion_email` |
| `backup.passphrase` Tier1 | `~/.vendex/secrets/backup.passphrase.enc` | No portable entre PCs — regenerar si se pierde (backups viejos cifrados se pierden) |
| `configuracion_email` fila | Dentro de BD (se restaura con dump) | — |
| `backup.properties` | `~/.vendex/backup.properties` | Reconfigurar ruta offsite y email |
| `db.properties` por PC | `~/.vendex/db.properties` (`vendex-db` si no rotó) | Solo si rotó password |

### Paso 9 — Verificación
1. `ping vendex-db` desde host y cliente OK.
2. Login Vendex en host y en una PC cliente.
3. Emitir Factura de prueba → SRI PRUEBAS → verificar PDF/RIDE.
4. Caja → Abrir/Cerrar → verificar que se dispara backup nuevo en `C:\Vendex\backups` y `\\backup-pc\VendexBackups`.
5. Revisar `Vendex → Administración → Respaldos` lista de últimos backups.

---

## 2. Borrado accidental (tabla o dato)

- **Tabla completa borrada:** `pg_restore -t nombre_tabla ultimo.dump` (no necesita tirar BD).
- **Dato reciente borrado hace minutos:** Restaurar último backup a BD temporal y copiar dato:
  ```bat
  createdb -h localhost -U postgres dbVendex_temp
  pg_restore -h localhost -U postgres -d dbVendex_temp C:\temp\restore.dump
  psql -h localhost -U postgres -d dbVendex_temp -c "COPY (SELECT * FROM cliente WHERE id=123) TO 'C:\temp\cliente_123.csv' CSV HEADER"
  psql -h localhost -U postgres -d dbVendex -c "COPY cliente FROM 'C:\temp\cliente_123.csv' CSV HEADER"
  ```
- **Pérdida <24h aceptable:** RPO actual es hasta 24h (último cierre de caja). Cold standby sin hardware es reutilizar cualquier PC (sin comprar).

---

## 3. Procedimiento de prueba mensual (recomendado)

1. Tomar último `vendex_*.dump.gpg` de offsite.
2. En PC de prueba (no el host central) restaurar a `dbVendex_test` (pasos 3-5).
3. `SELECT count(*) FROM factura_registro` debe estar en rango esperado (comparar con producción `SELECT max(id) FROM factura_registro`).
4. Registrar resultado. Si falla, alertar a `andresrockfull@gmail.com`.

---

## 4. Contactos y accesos

- **Responsable backup/hosts:** admin (andresrockfull@gmail.com) — mantiene `hosts` y reserva DHCP.
- **Host BD:** `vendex-db` (`C:\Windows\System32\drivers\etc\hosts` → IP real por cliente) — usuario `postgres` / `app_vendex` — passwords en gestor de contraseñas (NO en este repo).
- **Offsite:** `\\backup-pc\VendexBackups` — credenciales red Windows (usuario con permisos escritura).
- **Repo privado runbook:** (este archivo) — mantener sincronizado con cada cambio de IP (`hosts`), nueva tabla `sql/*.sql`, o rotación password.

---

## 5. Checklist tras cualquier cambio infra

- [ ] ¿Cambió IP host? → actualizar `hosts` (`vendex-db`) en cada PC y reserva DHCP (no tocar `db.properties` si no rotó password).
- [ ] ¿Nueva tabla en `sql/*.sql`? → añadir `GRANT` en `migracion_minimo_privilegio*.sql`.
- [ ] ¿Rotó password `app_vendex`? → actualizar en cada PC via wizard.
- [ ] ¿Nuevo certificado `.p12`? → copiar a offsite.
- [ ] ¿Cambió ruta backups? → actualizar `backup.properties` y `scripts/backup/backup_vendex.bat`.

---

## 6. Cold standby sin presupuesto (reutilizar cualquier PC)

No se compra equipo dedicado. El “standby” es el procedimiento §1 con cualquier PC de la red que tenga PG instalado al momento de la falla. La otra PC del offsite (`\\backup-pc`) es candidata natural si tiene espacio. Ver `docs/cold_standby_sin_hardware.md`.

---

## Dependencias

- `pg_dump` / `pg_restore` (PostgreSQL bin)
- `gpg` (Gpg4win)
- Acceso a `\\backup-pc\VendexBackups` desde `vendex-db`
- `ConfiguracionEmail` activa para alertas
- Resolución `vendex-db` via `hosts` (o DNS router si el cliente lo tiene)
