# Rediseño del Menú — Vendex 2.0

## Estado actual

- Menú superior horizontal (`MenuBar`) en `MainView.fxml`.
- Accesos organizados por categorías: Comprobantes, Inventario, Crédito, Caja, Historial, Alertas, Administración, Ayuda, Cuenta.
- Sin iconos en los ítems.
- Sin descripciones funcionales visibles para el usuario.

## Objetivo

- Mover el menú a la **barra lateral izquierda**.
- Agregar **iconos** a cada acceso.
- Renombrar ítems con nombres más apropiados a su funcionalidad real.
- Agregar descripción funcional de cada pantalla.
- Mantener acceso a: sucursal, tema claro/oscuro y usuario en la parte superior.

---

## Catálogo completo de pantallas

### Comprobantes

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Comprobantes > Ingreso > Facturas | `IngresoProductoView.fxml` / `#irIngresoFactura` | Registro de facturas de compra/ingreso de mercadería desde proveedores. | Ingreso de Facturas |
| Comprobantes > Ingreso > Facturas Ingresadas | `FacturasIngresadasView.fxml` / `#irFacturasIngresadas` | Listado y consulta de facturas de compra ya registradas en el sistema. | Facturas Registradas |
| Comprobantes > Emisión > Proforma | `NotaVentaView.fxml` / `#irNotaVenta` | Emisión de proformas/comprobantes de venta a clientes. | Proformas |
| Comprobantes > Emisión > Factura | `FacturaView.fxml` / `#irFactura` | Emisión de facturas electrónicas de venta. | Facturas de Venta |
| Comprobantes > Emisión > Nota de Crédito | `NotaCreditoView.fxml` / `#irNotaCredito` | Emisión de notas de crédito para devoluciones o ajustes. | Notas de Crédito |
| Comprobantes > Emisión > Nota de Débito | `NotaDebitoView.fxml` / `#irNotaDebito` | Emisión de notas de débito para recargos o ajustes. | Notas de Débito |
| Comprobantes > Emisión > Guía de Remisión | `GuiaRemisionView.fxml` / `#irGuiaRemision` | Generación y gestión de guías de remisión de transporte. | Guías de Remisión |
| Comprobantes > Emisión > Retención | `RetencionView.fxml` / `#irRetencion` | Emisión y gestión de retenciones de impuestos. | Retenciones |
| Comprobantes > Emisión > Seguimiento SRI | `SeguimientoSriView.fxml` / `#irSeguimientoSri` | Consulta del estado de autorización de comprobantes en el SRI. | Seguimiento SRI |
| Comprobantes > Emisión > Facturas Emitidas | `ComprobanteVentaReporteView.fxml` / `#irComprobanteVentaReporte` | Reporte y listado de comprobantes de venta emitidos. | Comprobantes Emitidos |

### Inventario

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Inventario > Ubicación Percha | `UbicacionPercheroView.fxml` / `#irUbicacionPerchero` | Gestión física de ubicaciones en percheros/estanterías del depósito. | Ubicaciones |
| Inventario > Gestión de Inventario | `InventarioView.fxml` / `#irInventario` | Ingreso, edición, baja y consulta de productos en inventario. | Productos |
| Inventario > Gestion de Stock | `GestionStockView.fxml` / `#irGestionStock` | Control de stock mínimo, alertas de reposición y movimientos. | Stock |

### Crédito

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Crédito > Cuentas por Cobrar | `PorCobrarView.fxml` / `#irPorCobrar` | Gestión de cuentas corrientes de clientes y seguimiento de pagos. | Cuentas por Cobrar |
| Crédito > Cuentas por Pagar | `PorPagarView.fxml` / `#irPorPagar` | Gestión de cuentas corrientes con proveedores y fechas de pago. | Cuentas por Pagar |

### Caja

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Caja > Ver Caja | `CajaView.fxml` / `#irCaja` | Apertura, cierre y movimiento de caja diaria. | Caja |

### Historial

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Historial > de Productos | `HistorialProductoView.fxml` / `#irHistorialProductos` | Historial de movimientos, precios y modificaciones de productos. | Historial de Productos |
| Historial > de Ventas | `HistorialVentaView.fxml` / `#irHistorialVentas` | Registro histórico de ventas y comprobantes emitidos. | Historial de Ventas |
| Historial > de Compras | `HistorialCompraView.fxml` / `#irHistorialCompras` | Registro histórico de compras y facturas de ingreso. | Historial de Compras |

### Alertas

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Alertas > Ver Alertas | `AlertaView.fxml` / `#irAlertas` | Visualización de alertas activas del sistema (stock, certificados, SRI). | Alertas |

### Administración

#### Gestión de Usuarios

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Administración > Gestión de Usuarios > Clientes | `ClienteView.fxml` / `#irClientes` | Alta, baja, modificación y consulta de clientes. | Clientes |
| Administración > Gestión de Usuarios > Usuarios | `UsuariosView.fxml` / `#irUsuarios` | Administración de usuarios del sistema y credenciales. | Usuarios |
| Administración > Gestión de Usuarios > Roles y Permisos | `GestionRolesView.fxml` / `#irGestionRoles` | Definición de roles, permisos y accesos por perfil. | Roles y Permisos |
| Administración > Gestión de Usuarios > Proveedores | `ProveedorView.fxml` / `#irProveedores` | Alta, baja, modificación y consulta de proveedores. | Proveedores |

#### Catálogos

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Administración > Catálogos > Códigos | `CodigoView.fxml` / `#irCodigos` | Administración de códigos auxiliares del sistema. | Códigos |
| Administración > Catálogos > Grupos | `GrupoView.fxml` / `#irGrupos` | Administración de grupos de productos. | Grupos |
| Administración > Catálogos > Marcas | `MarcaView.fxml` / `#irMarcas` | Administración de marcas de productos. | Marcas |

#### Configuración y Mantenimiento

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Administración > Numeración de Documentos | `NumeracionView.fxml` / `#irNumeracion` | Configuración de secuencias y numeración de comprobantes. | Numeración |
| Administración > Firma Electrónica | `FirmaView.fxml` / `#irFirma` | Administración del certificado de firma electrónica. | Firma Electrónica |
| Administración > Correo Electrónico | `ConfiguracionEmailView.fxml` / `#irConfiguracionEmail` | Configuración de cuenta SMTP para envío de correos. | Correo Electrónico |
| Administración > Respaldos | `BackupView.fxml` / `#irRespaldos` | Generación y administración de respaldos de base de datos. | Respaldos |
| Administración > Cola offline | `ColaOfflineView.fxml` / `#irColaOffline` | Visualización y gestión de la cola offline de operaciones. | Cola Offline |
| Administración > SRI Contingencia (email) | — / `#irConfigurarContingenciaSRI` | Configuración de envío de comprobantes por email en contingencia SRI. | Contingencia SRI |
| Administración > Postgres migrador (Flyway) | — / `#irConfigurarPostgres` | Ejecución y control de migraciones Flyway de base de datos. | Migraciones DB |

### Ayuda

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Ayuda > Asistente de Repuestos | Chat / `#irAsistenteRepuestos` | Chatbot/Asistente IA para consulta de repuestos. | Asistente IA |
| Ayuda > Configurar IA (Gemini) | — / `#irConfigurarIA` | Configuración del modelo y API de inteligencia artificial. | Configurar IA |
| Ayuda > Acerca de | — / `#acercaDe` | Información de la aplicación, versión y créditos. | Acerca de |

### Cuenta

| Nombre actual | Ruta (FXML / acción) | Funcionalidad | Nombre propuesto |
|---|---|---|---|
| Cuenta > Bienvenido (usuario) | Dinámico / `#irHome` | Muestra el usuariologueado y acceso a perfil/inicio. | Mi Cuenta |
| Cuenta > Home | — / `#irHome` | Regreso al dashboard principal. | Inicio |
| Cuenta > Cerrar Sesión | — / `#cerrarSesion` | Cierra la sesión actual del usuario. | Cerrar Sesión |
| Cuenta > Modo Dark / Modo Light | — / `#cambiarTema` | Alterna entre tema claro y oscuro. | Tema |

---

## Estructura propuesta del menú lateral

### Encabezado superior
- Logo de Vendex 2.0 (icono o imagen pequeña).
- Nombre de la app: **Vendex 2.0**.
- Selector de sucursal.
- Botón de cambio de tema claro/oscuro con icono.

### Items del menú lateral (con icono + etiqueta + tooltip descriptivo)

#### 1. Dashboard
- Icono: `fas-chart-line` o `fas-home`
- Acción: Abre el dashboard principal.
- Descripción tooltip: Panel principal con métricas y resumen del negocio.

#### 2. Comprobantes
- Icono: `fas-file-invoice`
- Subitems:
  - Ingreso de Facturas `fas-arrow-down`
  - Facturas Registradas `fas-list`
  - Proformas `fas-file-alt`
  - Facturas de Venta `fas-file-invoice-dollar`
  - Notas de Crédito `fas-undo`
  - Notas de Débito `fas-redo`
  - Guías de Remisión `fas-truck`
  - Retenciones `fas-percentage`
  - Seguimiento SRI `fas-satellite-dish`
  - Comprobantes Emitidos `fas-clipboard-list`

#### 3. Inventario
- Icono: `fas-boxes` o `fa-boxes`
- Subitems:
  - Ubicaciones `fa-map-marker-alt`
  - Productos `fa-box`
  - Stock `fa-chart-bar`

#### 4. Crédito
- Icono: `fas-hand-holding-usd`
- Subitems:
  - Cuentas por Cobrar `fa-users`
  - Cuentas por Pagar `fa-store`

#### 5. Caja
- Icono: `fas-cash-register` o `fas-money-bill-wave`
- Acción directa: Caja

#### 6. Historial
- Icono: `fas-history`
- Subitems:
  - Historial de Productos `fa-box-open`
  - Historial de Ventas `fa-shopping-cart`
  - Historial de Compras `fa-truck-loading`

#### 7. Alertas
- Icono: `fas-bell`
- Acción directa: Alertas

#### 8. Administración
- Icono: `fas-cogs`
- Subitems:
  - Clientes `fa-user-friends`
  - Usuarios `fa-user-shield`
  - Roles y Permisos `fa-key`
  - Proveedores `fa-truck`
  - Códigos `fa-barcode`
  - Grupos `fa-layer-group`
  - Marcas `fa-tag`
  - Numeración `fa-sort-numeric-up`
  - Firma Electrónica `fa-certificate`
  - Correo Electrónico `fa-envelope`
  - Respaldos `fa-database`
  - Cola Offline `fa-wifi` (con variación en tooltip)
  - Contingencia SRI `fa-envelope-open-text`
  - Migraciones DB `fa-download`

#### 9. Asistente IA
- Icono: `fa-robot`
- Acción directa: Abre el chatbot/Asistente de Repuestos.

#### 10. Configuración
- Icono: `fa-sliders-h`
- Subitems:
  - Configurar IA `fa-brain`
  - Tema claro/oscuro `fa-moon / fa-sun`
  - Acerca de `fa-info-circle`

### Pie del menú lateral
- Usuario logueado.
- Botón Cerrar Sesión.

---

## Consideraciones técnicas

1. `MainView.fxml` debe pasar de `BorderPane` con menú superior a:
   - `BorderPane` o `HBox` general.
   - `left`: menú lateral (`VBox` con items clickeables).
   - `top`: encabezado con logo, sucursal y tema.
   - `center`: contenedor dinámico de pantallas.
   - `bottom`: footer.

2. Los iconos pueden ser `FontIcon` de Ikonli con literales `fas-*`.

3. Las descripciones funcionales pueden ir como `Tooltip` en cada item del menú.

4. Los nombres propuestos buscan consistencia: sustantivos cortos, verbos en infinitivo para acciones, sin repeticiones.

5. Las pantallas internas (`FXML`) no necesitan cambios de nombre; solo cambia el label del menú.
