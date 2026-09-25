# Permisos BD — Mínimo Privilegio `app_vendex`

> Reemplazo de `GRANT ALL` implícito (`postgres` superuser) por grants explícitos por tabla. Basado en `LINEAMIENTO_GRANT_MINIMO_PRIVILEGIO.md`.

## 1. Preparación (antes de tocar el host central `vendex-db`)

```bash
# En host central (vendex-db, ej. 192.168.1.7 en cliente X) como postgres/superuser
pg_dump -h 127.0.0.1 -U postgres -d dbVendex -Fc -f /tmp/dbVendex_pre_grant_$(date +%F).dump
# Verificar roles actuales
psql -h 127.0.0.1 -U postgres -d dbVendex -c "\du"
psql -h 127.0.0.1 -U postgres -d dbVendex -c "\z factura_registro"
psql -h 127.0.0.1 -U postgres -d dbVendex -c "SELECT grantee, table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee='app_vendex' ORDER BY table_name;"
```

No mover `config/DatabaseConnection.java:88` `ensure*Schema` aún — se tolera sin `CREATE` (ver §5).

## 2. Inventario (42 tablas)

| # | Tabla | Tipo | Grants `app_vendex` | Razón / DAO |
|---|---|---|---|---|
| 1 | `marca` | L | `SELECT,INSERT,UPDATE,DELETE` | `MarcaDAO` CRUD |
| 2 | `grupo` | L | `SELECT,INSERT,UPDATE,DELETE` | `GrupoDAO` |
| 3 | `codigo` | L | `SELECT,INSERT,UPDATE,DELETE` | `CodigoDAO` (+ `INSERT SELECT DISTINCT FROM inventario`) |
| 4 | `cliente` | L/T | `SELECT,INSERT,UPDATE,DELETE` | `ClienteDAO` |
| 5 | `proveedor` | L/T | `SELECT,INSERT,UPDATE,DELETE` | `ProveedorDAO` |
| 6 | `vendedor` | L | `SELECT,INSERT,UPDATE,DELETE` | `VendedorDAO` |
| 7 | `inventario` | T | `SELECT,INSERT,UPDATE,DELETE` | `InventarioDAO` stock `cantidad -/+`, `DELETE WHERE numero_factura` |
| 8 | `perchero` | L | `SELECT,INSERT,UPDATE,DELETE` | `PercheroDAO` |
| 9 | `ubicacion` | L/T | `SELECT,INSERT,UPDATE,DELETE` | `UbicacionDetalleDAO` |
| 10 | `ubicacion_percha` | L | `SELECT,INSERT,UPDATE,DELETE` | `UbicacionPerchaDAO` legacy |
| 11 | `factura_registro` | T | `SELECT,INSERT,UPDATE` | `FacturaRegistroDAO` (`estado_sri`) — sin `DELETE` (anulación vía NC) |
| 12 | `factura_detalle` | T | `SELECT,INSERT,UPDATE` | `FacturaDetalleDAO` — sin `DELETE` |
| 13 | `nota_venta_registro` | T | `SELECT,INSERT,UPDATE` | `NotaVentaRegistroDAO` |
| 14 | `nota_venta_detalle` | T | `SELECT,INSERT,UPDATE` | `NotaVentaDetalleDAO` batch |
| 15 | `factura_proveedor` | T | `SELECT,INSERT,UPDATE,DELETE` | `FacturaProveedorDAO` (`DELETE WHERE numero_factura`) |
| 16 | `comprobante_temp` | T | `SELECT,INSERT,UPDATE,DELETE` | `ComprobanteTempDAO` temp proforma |
| 17 | `alertas` | A | `SELECT,INSERT,UPDATE,DELETE` | `AlertaDAO` (`DELETE WHERE leida`, `DELETE FROM alertas`) |
| 18 | `caja_sesion` | T | `SELECT,INSERT,UPDATE,DELETE` | `CajaSesionDAO` (`CERRADA`) |
| 19 | `caja_movimiento` | T/A | `SELECT,INSERT` | `CajaMovimientoDAO` solo `INSERT/SELECT` |
| 20 | `historial_producto` | A | `SELECT,INSERT` | `HistorialProductoDAO` solo `INSERT/SELECT` |
| 21 | `cuentas_por_cobrar` | T | `SELECT,INSERT,UPDATE,DELETE` | `CuentaPorCobrarDAO` (`adelanto`, `Pagado`) |
| 22 | `cuentas_por_pagar` | T | `SELECT,INSERT,UPDATE,DELETE` | `CuentaPorPagarDAO` + cascada `inventario` |
| 23 | `logs` | A | `SELECT,INSERT` | `LogDAO` solo `INSERT` (nunca `UPDATE/DELETE`) |
| 24 | `comprobantes_electronicos` | A/T | `SELECT,INSERT,UPDATE` | `ComprobanteDAO` (`estado_sri`) |
| 25 | `xml_enviados` | A | `SELECT,INSERT` | `ComprobanteDAO` solo `INSERT` (ideal solo `INSERT`, se deja `SELECT` para pantalla seguimiento) |
| 26 | `secuenciales` | S | `SELECT,INSERT,UPDATE` | `ComprobanteDAO` legacy `secuencial+1` |
| 27 | `secuencia_documento` | S | `SELECT,INSERT,UPDATE` | `SecuenciaDocumentoDAO` `siguiente_numero+1` |
| 28 | `nota_credito_registro` | T | `SELECT,INSERT,UPDATE` | `NotaCreditoRegistroDAO` |
| 29 | `nota_credito_detalle` | T | `SELECT,INSERT,UPDATE` | `NotaCreditoDetalleDAO` |
| 30 | `nota_debito_registro` | T | `SELECT,INSERT,UPDATE` | `NotaDebitoRegistroDAO` |
| 31 | `nota_debito_motivo` | T | `SELECT,INSERT,UPDATE` | `NotaDebitoMotivoDAO` |
| 32 | `guia_remision_registro` | T | `SELECT,INSERT,UPDATE` | `GuiaRemisionRegistroDAO` |
| 33 | `guia_remision_destinatario` | T | `SELECT,INSERT,UPDATE` | `GuiaRemisionDestinatarioDAO` |
| 34 | `guia_remision_detalle` | T | `SELECT,INSERT,UPDATE` | `GuiaRemisionDetalleDAO` |
| 35 | `retencion_registro` | T | `SELECT,INSERT,UPDATE` | `RetencionRegistroDAO` |
| 36 | `retencion_documento_sustento` | T | `SELECT,INSERT,UPDATE` | `RetencionDocumentoSustentoDAO` |
| 37 | `retencion_detalle` | T | `SELECT,INSERT,UPDATE` | `RetencionDetalleDAO` |
| 38 | `tabla_retencion` | L | `SELECT` | `TablaRetencionDAO` solo lectura (`303/312/725…`) |
| 39 | `certificado_estado` | Se/A | `SELECT,INSERT,UPDATE` | `CertificadoEstadoDAO` |
| 40 | `configuracion_email` | Se | `SELECT,INSERT,UPDATE` | `ConfiguracionEmailDAO` (excluida de keyring, fila compartida) |
| 41 | `usuarios` | Se | `SELECT,INSERT,UPDATE,DELETE` | `UsuarioDAO` (`DELETE`, `UPDATE intentos_fallidos/bloqueado_hasta` anti fuerza bruta) |
| 42 | `empresa` | Se/L | `SELECT,UPDATE` | `EmpresaDAO` solo `SELECT/UPDATE` (sin `INSERT/DELETE`) |
| 43 | `login_intento_log` | A | `SELECT,INSERT` | `LoginIntentoLogDAO` solo `INSERT/SELECT` (nunca `UPDATE/DELETE`) |
| 44 | `configuracion` | A | `SELECT` | `PoliticaBloqueo` (`bloqueo.*`), `GRANT SELECT` a `app_vendex`; `UPDATE` solo migrador/admin |

**31 SERIAL → 31 sequences + `login_intento_log_id_seq`**: `GRANT USAGE, SELECT ON ALL SEQUENCES` necesario para `INSERT` sin `id` (`nextval`). Sin esto `INSERT` falla `permission denied for sequence`.

**Nunca otorgado**: `TRUNCATE` (solo `postgres` para `sql/reset_test_data.sql:15`), `CREATE ON SCHEMA public` (solo migrador).

## 3. Script

`src/main/resources/sql/migracion_minimo_privilegio_20260920.sql` — idempotente. Ejecutar UNA VEZ como `postgres`:

```bash
psql -h 127.0.0.1 -U postgres -d dbVendex -f src/main/resources/sql/migracion_minimo_privilegio_20260920.sql
```

Contenido: `CREATE ROLE app_vendex` si no existe, `GRANT CONNECT/USAGE`, `REVOKE CREATE/ALL`, grants por tabla/sequence, `ALTER DEFAULT PRIVILEGES` para futuras tablas/sequences.

Después:

```bash
# Verificar
psql -h 127.0.0.1 -U postgres -d dbVendex -c "SELECT grantee, table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee='app_vendex' ORDER BY table_name, privilege_type;" > /tmp/grants_post.txt
psql -h 127.0.0.1 -U app_vendex -d dbVendex -c "INSERT INTO logs (controlador,metodo,mensaje) VALUES ('perm_test','test','ok') RETURNING id;"
# debe fallar (TRUNCATE no otorgado):
psql -h 127.0.0.1 -U app_vendex -d dbVendex -c "TRUNCATE TABLE logs;" # permission denied esperado
```

## 4. Cómo obtener permisos reales sin adivinar (evitar romper en prod)

1. En **staging** (copia de `dbVendex`), activar log: `ALTER SYSTEM SET log_statement='all'; SELECT pg_reload_conf();` o `pgaudit` si disponible.
2. Recorrer flujos reales: emitir Factura, NC, ND, GR, Retención (incluyendo envío SRI), generar RIDE+mail, login/cambio pass, configurar correo, dashboard, banner `certificado_estado`.
3. Analizar log: `grep -E "SELECT|INSERT|UPDATE|DELETE" postgresql.log | cut -d' ' -f... | sort -u` por tabla.
4. Ajustar script §3 si algún DAO falló `permission denied for table X` — añadir grant puntual, no volver a `GRANT ALL`.

## 5. Despliegue seguro en host central `vendex-db` (producción)

1. **Staging**: restaurar dump en máquina pruebas, aplicar script, cambiar UNA PC a `app_vendex` vía wizard (`DbSetupWizardController` → usuario `app_vendex`, password 24 generada, Probar y Guardar cifrado keyring), checklist completo sin `permission denied`.
2. **Producción** (ventana mantenimiento):
   ```bash
   pg_dump -Fc -f /tmp/dbVendex_pre_$(date +%F).dump dbVendex
   psql -f migracion_minimo_privilegio_20260920.sql
   # Rotar passwords
   psql -c "ALTER ROLE app_vendex WITH PASSWORD '***24***';"
   psql -c "ALTER ROLE postgres WITH PASSWORD '***nueva***';"
   ```
3. **Cada PC** (todas comparten misma BD via `vendex-db` en `hosts`, IP varía por cliente): wizard → `jdbc:postgresql://vendex-db:5432/dbVendex` ( `localhost` en host) → usuario `app_vendex`, pegar misma password generada, Probar y Guardar (cifrado keyring por máquina, no portable). Validar `cat ~/.vendex/db.properties` solo `sal:iv:cifrado`.
4. **Código**: `config/DbConfig.java:34` `DEFAULT_USER` ya cambiado a `app_vendex` (esta entrega). Nuevas instalaciones usan `app_vendex` directamente.
5. **DDL futuro**: `config/DatabaseConnection.java:88` `ensure*Schema` solo debe correr como `postgres`/`vendex_migrator` (detecta `current_user` y hace `REVOKE CREATE` skip). Si corre como `app_vendex` loguea `WARNING` sin ocultar `permission denied`.
6. **Hosts**: si cambia IP del host, actualizar `C:\Windows\System32\drivers\etc\hosts` (`vendex-db` → nueva IP) en cada PC (admin, `scripts/setup_configurar_hosts.bat`), no `db.properties` si no rotó password.

## 6. Prevención a futuro

- **Code review checklist**: toda tabla nueva en `sql/*.sql` + `ensure*Schema` debe venir con su `GRANT` explícito en la migración correspondiente; nunca asumir `GRANT ALL`. `ALTER DEFAULT PRIVILEGES` ya cubre futuras `*_id_seq`, pero grants de tablas nuevas deben agregarse al script.
- **Test infra opcional** `PermisosSchemaTest`: `SELECT grantee, table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee='app_vendex'` vs lista esperada en este doc; falla si alguien reintroduce `GRANT ALL` o olvida grant.

## 7. Rollback

Mantener `postgres` superuser intacto. Si en prod un flujo falla `permission denied for table X` bajo presión, parche temporal:
```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE X TO app_vendex;
-- o solo el privilegio faltante (ej. UPDATE)
```
No volver a `GRANT ALL ON ALL TABLES` como parche rápido.

## 8. Archivos

- `src/main/resources/sql/migracion_minimo_privilegio_20260920.sql`
- `src/main/java/com/vendex/config/DbConfig.java:34` (`DEFAULT_USER=app_vendex`)
- `src/main/java/com/vendex/config/DatabaseConnection.java:88` (ensure* tolera sin CREATE)
- `controller/DbSetupWizardController.java:88` (sugiere `app_vendex`)
