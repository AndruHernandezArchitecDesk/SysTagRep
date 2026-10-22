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

**24 tests, 0 fallos, 0 errores, 0 omitidos.** Cubren orden/filtrado SQL, fechas
nulas, sucursal, autorización REST, actualización manual, errores de carga,
cambio de sucursal, inserción a la derecha y cierre.

El portable no se actualizó por instrucción del usuario.
