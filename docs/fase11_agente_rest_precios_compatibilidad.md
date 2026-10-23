# Fase 11 — Vendexi: agente REST, precios y compatibilidad por vehículo

**Anterior:** `docs/fase8_agente_herramientas_roles.md` · **Fecha:** 23/10/2026

## Implementado

### Herramientas remotas

`HerramientasAgenteDAORest` completa el contrato de `HerramientasAgente` mediante
lecturas autenticadas en `/api/vetxi/herramientas/*`: productos, complementos,
clientes, ventas, saldo, más vendidos, margen y compatibilidades. El backend toma
la sucursal de `SesionActual` (token API) y construye el DAO de consultas limitado a
esa sucursal; no confía en una sucursal enviada por el cliente. Las fallas REST
degradan a la búsqueda habitual de inventario.

### Explicación del precio

La respuesta de producto transmite al redactor los datos ya registrados en el ERP:
precio, costo sin IVA, proveedor y margen calculado como
`(precio de venta - costo sin IVA) / precio de venta`. Cuando el costo está vacío,
Vetxi lo indica y no atribuye razones inventadas. El monto precio-costo se explica
como diferencia bruta registrada, no como utilidad neta; no se agregan impuestos,
descuentos ni gastos que no formen parte de los datos.

### Compatibilidad vehicular

Se consulta la relación estructurada `inventario_vehiculo` ↔ `vehiculo`, filtrando
por marca, modelo, intervalo de años, motor y descripción de pieza disponibles.
Solo una relación explícita se comunica como asociación del catálogo Vendex. Una
coincidencia textual se etiqueta como no verificada y se indica confirmar con el
fabricante. Si no hay una relación registrada, no se inventa compatibilidad.

## Pruebas

```bash
mvn test -Dtest=HerramientasAgenteDAORestTest,HerramientasAgenteApiControllerTest,HerramientasAgenteVehiculoTest,GeminiChatbotServiceHerramientasTest,HerramientasAgenteComplementariosTest
```

Las pruebas cubren deserialización REST, codificación de parámetros, autenticación,
contexto de sucursal, proveedor/costo/margen y exclusión de compatibilidades no
registradas. No requieren llamadas al proveedor de IA.

## Pendiente

- Completar una fuente técnica oficial para confirmar las asociaciones del catálogo
  y explicar precios con descuentos, impuestos u otros componentes si el ERP los
  almacena de forma estructurada.
- Actualizar precios de costo de tokens de IA cuando el usuario proporcione los
  valores reales del proveedor.
