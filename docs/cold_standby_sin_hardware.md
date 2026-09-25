# Cold standby sin hardware dedicado (SPOF §1 — sin presupuesto)

> No se compra segundo equipo. El “standby” es **procedimiento** para convertir cualquier PC de la red en host temporal usando el último backup.

## Situación

- Host central es PC de uso diario (no dedicado).
- Sin UPS/RAID dedicado; RPO 24h (último cierre de caja).
- Offsite ya existe: `\\backup-pc\VendexBackups` (otra PC con backups cifrados).

## Qué tener preparado (sin comprar)

1. En **una PC candidata** (ideal la del offsite, con más espacio), pre-instalar PostgreSQL misma versión + Gpg4win, aunque quede apagado. No necesita estar corriendo.
   ```bat
   pg_dump --version
   gpg --version
   ```
2. Asegurar que `docs/RECUPERACION_DR.md` y `docs/hosts_setup.md` están fuera del host (repo privado).

## Failover — pasos (admin)

1. **Elegir PC** con más disco/RAM disponible (ej. la del offsite).
2. **Restaurar** último backup (ver `docs/RECUPERACION_DR.md` §1):
   ```bat
   gpg --decrypt \\backup-pc\VendexBackups\vendex_YYYYMMDD_HHMMSS.dump.gpg > C:\temp\restore.dump
   psql -h localhost -U postgres -c "CREATE ROLE app_vendex WITH LOGIN PASSWORD '...'"
   psql -h localhost -U postgres -c "CREATE DATABASE dbVendex OWNER app_vendex"
   pg_restore -h localhost -U postgres -d dbVendex --clean --if-exists C:\temp\restore.dump
   psql -h localhost -U postgres -d dbVendex -f src\main\resources\sql\migracion_minimo_privilegio_20260920.sql
   ```
3. **Reasignar IP/hostname:** dar a esa PC la IP del host anterior (reserva DHCP) o nueva IP, y ejecutar en **cada PC**:
   ```bat
   scripts\setup_configurar_hosts.bat NUEVA_IP
   ```
   Verificar `ping vendex-db` y `psql -h vendex-db -U app_vendex -d dbVendex -c "select 1"`.

4. **Continuar facturando** mientras se repara/reemplaza el host original. El crash de login se mantiene (sin BD no abre), pero con `vendex-db` ya apunta al temporal, vuelve a abrir.

## Limitaciones

- Sin segundo equipo encendido, no hay warm standby ni replicación. Pérdida máxima 24h (RPO).
- Si la PC temporal es de uso diario también, coordinar horarios y evitar apagarla durante jornada.

## Prueba semestral (sin comprar)

Simular apagado del host fuera de horario y medir tiempo hasta que clientes reconectan a `vendex-db` temporal. Registrar MTTR.

## Si en el futuro hay presupuesto

Ver `LINEAMIENTO_SPOF_SERVIDOR.md` §1 N2 warm standby (`streaming replication`): segundo equipo siempre encendido + `pg_basebackup` + `pg_ctl promote`. Solo si RPO <24h o MTTR <30min justifica costo.
