# Hosts distribuido — `vendex-db` (SPOF §0)

> Reemplaza IP fija por hostname para que cambiar de host sea editar 1 línea `hosts` por PC, no N `db.properties`. IP varía por cliente; usar `vendex-db` siempre.

## Concepto

- **Host central** (BD): Vendex usa `jdbc:postgresql://localhost:5432/dbVendex`
- **Clientes**: Vendex usa `jdbc:postgresql://vendex-db:5432/dbVendex`
- `vendex-db` resuelve via `C:\Windows\System32\drivers\etc\hosts` (sin DNS). Si el cliente tiene DNS local en router, puede crear `vendex-db → IP` ahí y no usar `hosts`, pero `hosts` funciona siempre.

```
# C:\Windows\System32\drivers\etc\hosts (ejemplo cliente A)
192.168.1.7 vendex-db

# Cliente B con otra red
192.168.0.100 vendex-db
```

## Instalación por cliente (admin)

1. Elegir IP fija para el host (reserva DHCP en router del local, ej. `192.168.1.7`).
2. En **cada PC** (host + clientes), ejecutar como Administrador:
   ```bat
   scripts\setup_configurar_hosts.bat 192.168.1.7
   ```
   O manual: Notepad admin → abrir `C:\Windows\System32\drivers\etc\hosts` → añadir línea → guardar → `ipconfig /flushdns`.

3. Verificar:
   ```bat
   ping vendex-db
   psql -h vendex-db -U app_vendex -d dbVendex -c "select 1"
   ```

4. Wizard Vendex:
   - Host: `jdbc:postgresql://localhost:5432/dbVendex`
   - Clientes: `jdbc:postgresql://vendex-db:5432/dbVendex` (aparece como `promptText` por defecto)

## Cambio de host (failover sin comprar hardware)

Si el host muere, admin elige cualquier PC como host temporal (ver `docs/cold_standby_sin_hardware.md`):

1. Restaurar último backup en la PC elegida (ver `docs/RECUPERACION_DR.md`).
2. Asignar nueva IP (ej. `192.168.1.55`) y ejecutar en **cada PC**:
   ```bat
   scripts\setup_configurar_hosts.bat 192.168.1.55
   ```
3. Todas las PCs reconectan a `vendex-db` sin tocar `db.properties` (si no rotó password).

## Troubleshooting

- `ping vendex-db` falla: revisar `hosts` tiene 1 sola línea `vendex-db`, sin `#` al inicio, `ipconfig /flushdns`, firewall.
- `psql -h vendex-db` timeout: PG no escucha en `0.0.0.0` o `pg_hba.conf` no permite red. Revisar `postgresql.conf` `listen_addresses='*'`.
- Cliente usa IP directa antigua: borrar `~/.vendex/db.properties` y reconfigurar wizard con `vendex-db`.

## Por qué no IP directa

IP directa obliga a tocar N `db.properties` al cambiar de equipo. Con `hosts`, el MTTR pasa de horas (ir PC por PC) a minutos (actualizar 1 línea por PC via script). RPO sigue 24h, pero RTO mejora sin costo.

## Archivos

- `src/main/java/com/vendex/config/DbConfig.java:15` y `view/DbSetupWizardView.fxml:24` ya usan `vendex-db`.
- `scripts/setup_configurar_hosts.bat` — idempotente, backup `hosts.vendex-bak`.
- `docs/RECUPERACION_DR.md` §0 y §7.
