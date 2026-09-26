# API REST Vendex V1 (Javalin, misma BD dbVendex)

> **HikariCP** `maxPool=6` por PC/API (misma BD `vendex-db:5432/dbVendex`). Multi-sucursal `sucursal_id` DEFAULT 1.

## Arranque

```bash
# Deshabilitada por defecto
java -jar Vendex-2.0-SNAPSHOT.jar

# Habilitar API en puerto 7070
java -Dapi.enabled=true -Dapi.port=7070 -jar Vendex-2.0-SNAPSHOT.jar
# o
API_ENABLED=true API_PORT=7070 java -jar Vendex-2.0-SNAPSHOT.jar
```

Log: `API REST Vendex iniciada en http://localhost:7070/api/health`

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/health` | `{"status":"UP","sucursales":2}` |
| GET | `/api/sucursales` | Lista `Sucursal` |
| GET | `/api/sucursales/{id}` | Por id |
| POST | `/api/sucursales` | `{"codigo":"002","nombre":"Sur","direccion":"Av Sur"}` → 201 |
| GET | `/api/inventario?sucursalId=1&q=tornillo&page=1&size=25` | `listarPorSucursal` + filtro `q` |
| GET | `/api/inventario?vehMarca=TOYOTA&vehModelo=HILUX&vehAnio=2019` | Filtro por compatibilidad de vehículo (`vehMarca`/`vehModelo`/`vehAnio`, combinable con `q`, `sucursalId`, `page`, `size`) |
| GET | `/api/inventario/{id}` | Por id |
| POST | `/api/transferencias` | `{"inventarioIdOrigen":1,"destinoSucursalId":2,"cantidad":4,"motivo":"Reposicion"}` → 201 (transaccional `HikariCP`) |
| GET | `/api/transferencias?inventarioId=1` / `?sucursalId=2` | Listar |
| GET | `/api/vehiculos?marca=&modelo=&anio=&q=` | Catálogo `vehiculo` (búsqueda o listado) |
| GET | `/api/vehiculos/{id}` | Por id |
| POST | `/api/vehiculos` | `{"marca":"TOYOTA","modelo":"HILUX","anioDesde":2015,"anioHasta":2024,"motor":"2.8"}` → 201 |
| GET | `/api/vehiculos/vin/{vin17}` | Lookup VIN (fallback import NHTSA vPIC si no existe) |
| POST | `/api/vehiculos/import?make=TOYOTA&limit=5` | Importa modelos desde API pública NHTSA vPIC |
| GET | `/api/inventario/{id}/compatibilidades` | Vehículos asociados a un repuesto |
| POST | `/api/inventario/{id}/compatibilidades` | `{"vehiculoIds":[1,2]}` → asocia/reemplaza compatibilidades |

## Ejemplo curl

```bash
curl http://localhost:7070/api/sucursales | jq
curl "http://localhost:7070/api/inventario?sucursalId=1" | jq
curl "http://localhost:7070/api/inventario?vehMarca=TOYOTA&vehModelo=HILUX&vehAnio=2019" | jq
curl "http://localhost:7070/api/vehiculos?marca=TOYOTA" | jq
curl -X POST http://localhost:7070/api/transferencias -H "Content-Type: application/json" -d '{"inventarioIdOrigen":1,"destinoSucursalId":2,"cantidad":2,"motivo":"test"}' | jq
```

## Notas

* Reusa `AppContext` DAOs `*Postgres` y `TransferenciaInventarioService` (transacción `origen - cantidad`, `destino + cantidad`, `transferencia_inventario`).
* Sin auth V1 (mismo `app_vendex` de la BD). V2: JWT + `SesionActual` / `Rol`.
* `SucursalActual` UI (`MainView.fxml` combo) no afecta API (API recibe `sucursalId` explícito por query).
* Test: `src/test/java/com/vendex/api/ApiIntegrationTest.java` (Javalin `Testcontainers` `postgres:15-alpine`, `Flyway` `V1..V25`, `java.net.http.HttpClient`, `disabledWithoutDocker=true`).
