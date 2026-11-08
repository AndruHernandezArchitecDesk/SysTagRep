# Panel Stock negativo en Ingreso de Facturas

## Ubicación y contenido

La pantalla **Ingreso de Facturas** presenta el panel a la derecha del formulario,
en un divisor redimensionable. Muestra código, descripción, stock actual y fecha
de alta; el orden es fecha descendente y el desempate, ID descendente. Los registros
sin fecha quedan al final. Si no hay negativos, lo indica dentro de la tabla.

El listado se filtra a la sucursal de la sesión actual. No incluye inactivos,
positivos ni productos de otras sucursales.

## Actualización y ciclo de vida

- Carga automáticamente al abrir la pantalla y tiene un botón **Actualizar**.
- La consulta se hace fuera del hilo FX y tiene timeout de 15 segundos en PostgreSQL
  y REST.
- Después de guardar un ingreso se vuelve a cargar, de modo que un producto repuesto
  deja de aparecer cuando su stock ya no es negativo.
- El saldo real se confirma **solo al Guardar**. Agregar, quitar o cancelar líneas
  del formulario no cambia existencias.
- Al ocultar o cerrar la ventana se cancela el trabajo y se retiran las filas para
  no conservar vistas de sucursal/usuario anterior.
- En edición de un producto o una factura de proveedor, y en modales que cierran al
  guardar, se oculta el panel. Volver al formulario principal lo restaura.
- El panel es de consulta; seleccionar un negativo no altera el formulario de compra.

## Botón ocultar/mostrar

El encabezado **Ingreso de Factura** incluye un botón rotulado **«Stock negativo»**
(`btnTogglePanelStock`, icono de flecha) junto al título que oculta y vuelve a
mostrar el panel derecho:

- Ocultar retira el panel del divisor y cancela la carga en curso; el formulario
  principal ocupa todo el ancho.
- Mostrar lo reinserta a la derecha con el divisor en 0.68 y recarga de inmediato.
- El estado se combina con la visibilidad por modo: en edición de producto/factura
  el panel permanece oculto aunque el usuario lo haya marcado como visible, y el
  icono del botón refleja el estado efectivo (`chevron-right` visible,
  `chevron-left` oculto); el tooltip también alterna entre «Ocultar…» y «Mostrar…».
- Tras guardar, el refresco solo se ejecuta si el panel está realmente visible.
- El estado es por sesión: se reinicia al reabrir la pantalla.

El mismo patrón se aplica en **Préstamos Financieros** con el botón rotulado
**«Ingresos recientes»** (`btnToggleIngresos`), que oculta/muestra el panel
«Ingresos recientes» (`paneIngresos`); al ocultarlo el bloque izquierdo toma todo
el ancho gracias a `HBox.hgrow="ALWAYS"`.

## Convención FXML de tooltips

`Labeled` (base de `Button`) declara `@DefaultProperty("text")`. Un hijo
`<Tooltip .../>` en FXML inicia con mayúscula, por lo que **no** se interpreta como
la propiedad `tooltip` y FXMLLoader lo asigna a `text` mediante
`String.valueOf(...)`: el botón termina mostrando `javafx.scene.control.Tooltip@…`
(si no tiene atributo `text`) o pierde el tooltip (si el atributo lo sobrescribe).
Por eso en este repo los tooltips **siempre** van envueltos en minúscula:

```xml
<Button text="BOTÓN">
    <tooltip><Tooltip text="Ayuda"/></tooltip>
</Button>
```

Esta corrección unificó los 55 usos de la app (`MainView`, `PrestamoView`,
`IngresoMercaderiaView`, `ConsumoIaView`, `ChatWidget`), eliminando el texto
`javafx.scene.control.Tooltip@…` visible en los botones icono de Alertas/Inicio y
restaurando los tooltips que nunca llegaron a instalarse.

## API REST y autorización

`GET /api/inventario/stock-negativo` devuelve una proyección ligera que incluye
`fecha_ingreso`. La sucursal procede de la sesión autenticada; el endpoint rechaza
una sucursal solicitada distinta para impedir acceso cruzado. El DAO local ejecuta
la misma consulta con `WHERE estado=true AND cantidad<0 AND sucursal_id=?` y
`ORDER BY fecha_ingreso DESC NULLS LAST, id DESC`.

## Verificación

```bash
./mvnw -Dtest=StockNegativoPaneTest,FxmlSmokeTest,InventarioStockNegativoTest,InventarioStockNegativoApiTest test
```

Estas pruebas cubren orden/filtrado SQL, fechas
nulas, sucursal, autorización REST, actualización manual, errores de carga,
cambio de sucursal, inserción a la derecha y cierre.

## Reposición por código y sucursal

El guardado utiliza `IngresoStockService`: busca el **código del producto**
(`inventario.codigo`, que el formulario llama código manual), normalizando espacios
externos y mayúsculas. No compara descripción, proveedor ni el TAG generado para
la etiqueta. La sucursal corresponde a la sesión; nunca compensa otra sucursal.
Los guiones y demás caracteres internos siguen siendo parte del código.

| Stock antes | Compra | Stock confirmado | Panel negativo |
|---:|---:|---:|---|
| −5 | 3 | −2 | Sigue visible con −2 |
| −5 | 5 | 0 | Desaparece |
| −5 | 8 | 3 | Desaparece |

Se mantiene el ID y el TAG del producto existente, incluyendo temporales vendidos
antes de recibir la mercadería. Los ingresos posteriores del mismo código suman
sobre ese producto. Un código nuevo crea un producto en la sucursal correcta.
Los servicios logísticos no se mezclan con existencias de productos.

Si hay varios lotes antiguos negativos del mismo código, se compensan primero todos
ellos y solo después se deja excedente positivo. Los movimientos se enlazan a esos
lotes en `ingreso_stock_movimiento`, conservando cada línea original de la factura
con su cantidad e importe, sin diferencias por redondeo al repartir existencias.

La factura, el incremento y los créditos al proveedor se confirman en una sola
transacción. Si una línea falla, todo se revierte. La edición de ingresos enlazados
resta el aporte anterior y aplica el nuevo: conserva ventas posteriores y evita
sumar dos veces. Las facturas históricas sin enlace mantienen las restricciones
de edición si sus productos ya tienen ventas o ingresos posteriores.

`factura_proveedor` conserva la cantidad **comprada**, aunque el inventario aún sea
negativo. La migración **V42** agrega el enlace al producto, la sucursal, el crédito
y los datos de compra para edición. `ensureIngresoStockSchema` incorpora el mismo
esquema al arrancar un cliente directo con permisos de DDL; con un usuario limitado,
el administrador debe aplicar V42 en el servidor. Las etiquetas usan la cantidad
comprada, no el saldo resultante.

`InventarioEventos` notifica después del commit al panel y a las vistas de inventario
abiertas en el mismo proceso. La sincronización offline emite el aviso solo cuando
el servidor confirma el ingreso. El endpoint `/api/ingreso-mercaderia` comparte
el mismo servicio y usa la sucursal autenticada, no una sucursal enviada en el JSON.
En modo API hay que actualizar también el backend antes de usar este flujo.

### Verificación adicional

```bash
xvfb-run -a ./mvnw -q -Dnet.bytebuddy.experimental=true \
  -Dtest=IngresoStockServiceTest,StockNegativoPaneTest,InventarioStockNegativoTest,InventarioStockNegativoApiTest,FacturaTemporalTransaccionTest,FacturaTemporalApiTest test
```

`IngresoStockServiceTest` también admite PostgreSQL aislado mediante
`-Dingreso.test.pg.url=jdbc:postgresql://127.0.0.1:PUERTO/postgres`, con usuario
`vendex_test`. Crea y elimina un esquema único por prueba; incluye concurrencia de
dos ingresos y rollback de la edición. Esa prueba concurrente solo se ejecuta con
PostgreSQL; los demás escenarios también corren en HSQLDB.
