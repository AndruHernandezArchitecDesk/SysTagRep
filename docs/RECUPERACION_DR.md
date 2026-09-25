# Runbook — Recuperación ante Desastre (PostgreSQL dbVendex)

> **Guardar copia de este documento FUERA de 192.168.1.7** (repo privado, Drive, impresión). Si el host central se pierde y este runbook solo está ahí, es inútil.

**Última revisión:** 2026-09-23 — RPO 24h (backup diario `pg_dump` custom + GPG en cierre de caja).  
**Host central:** `192.168.1.7` — BD `dbVendex` — Rol app `app_vendex` (ver `docs/permisos_bd.md` y `sql/migracion_minimo_privilegio_20260920.sql`).  
**Offsite:** otra PC en red `\\OTRA-PC\VendexBackups` (configurable en `~/.vendex/backup.properties` `backup.offsite.path`).  
**Notificación falla:** `andresrockfull@gmail.com` ( `~/.vendex/backup.properties` `backup.email.destino` ).  
**Cifrado backup:** `gpg --symmetric AES256`, passphrase en `SecureConfigStore` Tier1 `backup.passphrase` (`~/.vendex/secrets/backup.passphrase.enc` + keyring DPAPI).  
**Archivo local:** `C:\Vendex\backups\vendex_YYYYMMDD_HHMMSS.dump.gpg` — retención GFS 7 diarios / 4 semanales / 12 mensuales.

---

## 1. Pérdida total de 192.168.1.7 (disco dañado / robo / borrado)

### Paso 1 — Nuevo equipo Windows
1. Instalar PostgreSQL **misma versión** que producción (verificar `SELECT version();` previo — si no se sabe, usar 17 LTS). Durante instalación, definir password `postgres` superuser y anotarla.
2. Verificar `pg_dump --version` y `psql --version` en PATH. Si no están, añadir `C:\Program Files\PostgreSQL\17\bin` al PATH.
3. Instalar Gpg4win (https://www.gpg4win.org) y verificar `gpg --version`.

### Paso 2 — Ubicar último backup
- Local (si disco sobrevivió): `C:\Vendex\backups`
- Offsite (otra PC): `\\OTRA-PC\VendexBackups` — elegir el más reciente `vendex_YYYYMMDD_HHMMSS.dump.gpg`. Verificar tamaño no sea 0.

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

### Paso 7 — Reconfigurar cada PC cliente
1. En **cada PC** (incluida la nueva 192.168.1.7): borrar `~/.vendex/db.properties` o abrir Vendex → Wizard BD.
2. Configurar:
   - Host central: `jdbc:postgresql://192.168.1.7:5432/dbVendex` (en host usar `localhost`)
   - Usuario: `app_vendex`
   - Password: la nueva de 24 chars (pegar misma en todas las PCs → “PC adicional — pegar existente”)
   - Probar conexión → Guardar cifrado.
3. Si cambió IP del nuevo host, actualizar `db.url` en cada PC.

### Paso 8 — Restaurar otros archivos críticos (no viven en BD)
| Archivo | Ubicación original | Respaldar aparte |
|---|---|---|
| Certificado firma `.p12` | `~/.vendex/firma.properties` → `rutaP12` | Copiar el `.p12` a offsite también |
| `vendex-master-key` Tier2 | `~/.vendex/.install-master.key` + `.bak` | Guardado fuera de PCs (wizard export) — sin ella no descifra `configuracion_email` |
| `backup.passphrase` Tier1 | `~/.vendex/secrets/backup.passphrase.enc` | No portable entre PCs — regenerar si se pierde (backups viejos cifrados se pierden) |
| `configuracion_email` fila | Dentro de BD (se restaura con dump) | — |
| `backup.properties` | `~/.vendex/backup.properties` | Reconfigurar ruta offsite y email |

### Paso 9 — Verificación
1. Login Vendex en host y en una PC cliente.
2. Emitir Factura de prueba → SRI PRUEBAS → verificar PDF/RIDE.
3. Caja → Abrir/Cerrar → verificar que se dispara backup nuevo en `C:\Vendex\backups` y `\\OTRA-PC\VendexBackups`.
4. Revisar `Vendex → Administración → Respaldos` lista de últimos backups.

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
- **Pérdida <24h aceptable:** RPO actual es hasta 24h (último cierre de caja). Si necesita PITR segundos, ver `docs/WAL_FASE2.md` (no implementado).

---

## 3. Procedimiento de prueba mensual (recomendado)

1. Tomar último `vendex_*.dump.gpg` de offsite.
2. En PC de prueba (no 192.168.1.7) restaurar a `dbVendex_test` (pasos 3-5).
3. `SELECT count(*) FROM factura_registro` debe estar en rango esperado (comparar con producción `SELECT max(id) FROM factura_registro`).
4. Registrar resultado. Si falla, alertar a `andresrockfull@gmail.com`.

---

## 4. Contactos y accesos

- **Responsable backup:** andresrockfull@gmail.com
- **Host BD:** 192.168.1.7 — usuario `postgres` / `app_vendex` — passwords en gestor de contraseñas (NO en este repo).
- **Offsite:** `\\OTRA-PC\VendexBackups` — credenciales red Windows (usuario con permisos escritura).
- **Repo privado runbook:** (este archivo) — mantener sincronizado con cada cambio de IP, nueva tabla `sql/*.sql`, o rotación password.

---

## 5. Checklist tras cualquier cambio infra

- [ ] ¿Cambió IP host? → actualizar `db.properties` en cada PC y este runbook.
- [ ] ¿Nueva tabla en `sql/*.sql`? → añadir `GRANT` en `migracion_minimo_privilegio*.sql`.
- [ ] ¿Rotó password `app_vendex`? → actualizar en cada PC via wizard.
- [ ] ¿Nuevo certificado `.p12`? → copiar a offsite.
- [ ] ¿Cambió ruta backups? → actualizar `backup.properties` y `scripts/backup/backup_vendex.bat`.

---

## Dependencias

- `pg_dump` / `pg_restore` (PostgreSQL bin)
- `gpg` (Gpg4win)
- Acceso a `\\OTRA-PC\VendexBackups` desde 192.168.1.7
- `ConfiguracionEmail` activa para alertas (sino fallback no notifica)
