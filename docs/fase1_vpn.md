# Fase 1 — Postgres central + VPN (puente multisucursal)

> Objetivo: habilitar acceso remoto de sucursales a la BD central manteniendo el cliente JavaFX con JDBC. Es una solución puente para 2-3 sucursales con internet fijo/bueno.

## Alcance

- Misma base de datos central (`dbVendex` en host `vendex-db`).
- Sucursales acceden via VPN sitio a sitio (WireGuard, IPsec router, OpenVPN, etc.).
- Clientes JavaFX siguen usando JDBC + HikariCP; **no hay API REST todavía**.
- Se reutiliza todo el trabajo de Fase 0: `sucursal_id`, `punto_emision`, `secuencia_documento` por punto, datascoping por sucursal.

## Requisitos previos

- Fase 0 aplicada (migraciones V24, V27, V28, V29 ejecutadas).
- BD central accesible desde LAN y postgres escuchando en `0.0.0.0` o IP de la VPN.
- `pg_hba.conf` permite conexiones desde los rangos de la VPN (md5/scram-sha-256).
- Firewall del host central abre 5432/tcp **solo** para las IPs de la VPN (no exponer a internet público).

## Topología

```
Sucursal A (JavaFX JDBC) ─┐
Sucursal B (JavaFX JDBC) ─┼── VPN ──► Host central (vendex-db:5432/dbVendex)
Sucursal C (JavaFX JDBC) ─┘
```

## Configuración por PC

### Host central (`vendex-db`)

1. Asignar IP fija en la LAN y/o IP en la VPN (ej. `10.0.0.1/24`).
2. `postgresql.conf`:
   ```ini
   listen_addresses = '*'
   port = 5432
   ```
3. `pg_hba.conf` — permitir solo VPN:
   ```conf
   host    dbVendex    app_vendex    10.0.0.0/24    scram-sha-256
   host    dbVendex    app_vendex    192.168.1.0/24 scram-sha-256
   ```
4. Reiniciar Postgres.
5. Verificar:
   ```bash
   psql -h 10.0.0.1 -U app_vendex -d dbVendex -c "select 1"
   ```

### Sucursal (cliente)

1. Configurar cliente VPN para conectarse al túnel del host.
2. Verificar conectividad:
   ```bash
   ping vendex-db
   psql -h vendex-db -U app_vendex -d dbVendex -c "select 1"
   ```
3. En Vendex, wizard de BD:
   - URL: `jdbc:postgresql://vendex-db:5432/dbVendex`
   - Usuario: `app_vendex` (mínimo privilegio).
   - Password: guardado cifrado por PC (`~/.vendex/db.properties`).

## Connection pooling (HikariCP)

Ya activo en `DatabaseConnection`. Para WAN/VPN considerar:

| Parámetro | Valor actual | Recomendación WAN |
|---|---|---|
| `maximumPoolSize` | 6 | 4-6 (no saturar túnel) |
| `minimumIdle` | 2 | 1-2 |
| `connectionTimeout` | 10000 ms | 15000-20000 ms |
| `idleTimeout` | 300000 ms | mantener |
| `leakDetectionThreshold` | 60000 ms | mantener |

Ajustar via `DbConfig` o properties de HikariCP si se requiere tuning por cliente.

## Secretos

- `.p12` y contraseña SMTP siguen en cada PC sucursal (no ideal, pero Fase 1 no cambia esto).
- Password BD cifrado en keyring por PC (`SecureConfigStore`).
- Rotar password de `app_vendex` post-configuración VPN.

## Ventajas / Limitaciones

**A favor:**
- Casi sin cambios de código (solo Fase 0 ya aplicada).
- Rápido de habilitar.

**En contra (aceptados como puente):**
- Latencia WAN: JDBC multi-consulta se siente lento; pooling ayuda pero no elimina el problema.
- Sin internet en sucursal = no opera.
- Secretos siguen dispersos por PCs.
- Host central sigue siendo SPOF.

## Checklist de validación

- [ ] Host central: `listen_addresses='*'`, firewall abre 5432 solo para VPN, `pg_hba.conf` restringe rangos VPN.
- [ ] Cliente VPN: conecta, resuelve `vendex-db`, hace `psql -h vendex-db ...`.
- [ ] Wizard Vendex en sucursal: URL `jdbc:postgresql://vendex-db:5432/dbVendex`.
- [ ] `mvn compile` pasa.
- [ ] Login OK desde sucursal.
- [ ] Factura/NC/ND/GR/Retención OK desde sucursal.
- [ ] Caja apertura/cierre OK desde sucursal.
- [ ] Inventario lista OK desde sucursal (filtrado por sucursal_id).
- [ ] Dashboard carga métricas de sucursal correctamente.

## Migración a Fase 2

Cuando se supere 2-3 sucursales, latencia molesta o se requiera modo offline, avanzar a Fase 2 (backend API + clientes delgados). Fase 0 ya prepara el modelo; Fase 2 introduce `DAORest` y centraliza lógica/SRI/firma en servidor.
