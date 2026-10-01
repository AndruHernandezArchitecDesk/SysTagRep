# Fase 3 — Modo Offline + Sincronización

> Objetivo: permitir que una sucursal opere sin internet y sincronice al reconectar.
> Depende de Fase 2 (backend API + clientes delgados).

## Componentes implementados

### OfflineModeManager
- Monitoreo automático de conectividad hacia el backend (HTTP ping a `/api/health` cada 15s).
- Estados: `ONLINE`, `OFFLINE`, `SYNCING`.
- Cambio automático a `OFFLINE` tras 3 fallos consecutivos.
- Cambio a `SYNCING` al detectar reconexión.

### LocalOperationQueue
- Cola persistente en archivo JSON: `~/.vendex/offline_queue/operaciones.json`.
- Estados de operación: `PENDIENTE`, `ENVIADA`, `CONFLICTO`, `FALLIDA`.
- Thread-safe (sincronizado).
- Permite encolar, listar pendientes, marcar estados y limpiar enviadas.

### SecuenciaBloque (reserva de bloques)
- Modelo: `SecuenciaBloque` (id, puntoEmisionId, tipo, numeroInicio, numeroFin, usadoHasta).
- DAO: `SecuenciaBloqueDAOPostgres` y `SecuenciaBloqueDAORest`.
- Tabla BD: `secuencia_bloque` (migración V30).
- Endpoints backend:
  - `POST /api/secuencia-documento/bloque` — reservar bloque
  - `GET /api/secuencia-documento/bloque-disponible` — obtener bloque disponible
  - `GET /api/secuencia-documento/bloques` — listar por punto de emisión
  - `POST /api/secuencia-documento/bloques/liberar-expirados` — limpiar expirados

### SecuenciaLocal
- Cache en memoria de bloques por tipo de comprobante.
- `siguiente(tipo)` devuelve el siguiente secuencial sin consultar al servidor.
- Reserva bloques automáticamente al agotarse.

### SyncService
- Ejecuta sincronización cada 30 segundos cuando hay conexión.
- Procesa operaciones pendientes en orden FIFO.
- Re-ejecuta cada operación contra el API REST.
- Marca como `ENVIADA`, `CONFLICTO` o `FALLIDA` según resultado.
- Cambia modo a `ONLINE` cuando la cola se vacía.

## Uso en controllers

```java
if (OfflineHelper.debeUsarModoOffline()) {
    // Guardar localmente y encolar para sync
    guardarOffline();
} else if (ApiConfig.isModoRemoto()) {
    // Llamar API remota
    guardarRemoto();
} else {
    // Modo local JDBC
    guardarLocal();
}
```

## Próximos pasos Fase 3

1. Integrar `OfflineModeManager` en `MainApp` (iniciar monitoreo al arrancar).
2. Modificar `FacturaController`, `NotaVentaController`, `NotaCreditoController` para soportar rama offline.
3. Implementar persistencia local de facturas/notas offline (tablas espejo o SQLite).
4. Agregar UI de estado de conexión y cola de sincronización.
5. Implementar resolución de conflictos de inventario.
6. Pruebas de integración offline/online.

## Migración de BD

```sql
-- V30__secuencia_bloque.sql
CREATE TABLE secuencia_bloque (
    id SERIAL PRIMARY KEY,
    punto_emision_id INTEGER NOT NULL REFERENCES punto_emision(id),
    tipo VARCHAR(20) NOT NULL,
    numero_inicio INTEGER NOT NULL,
    numero_fin INTEGER NOT NULL,
    usado_hasta INTEGER NOT NULL,
    reservado_en TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (punto_emision_id, tipo, numero_inicio, numero_fin)
);
```
