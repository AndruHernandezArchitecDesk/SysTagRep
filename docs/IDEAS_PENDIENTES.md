# Ideas pendientes para SysTagRep

> Módulo 2 (Alertas) ya implementado. A continuación las ideas restantes organizadas por prioridad.

---

## 1. Módulo de Vehículos y Compatibilidad (Alta prioridad)

Este es el **hueco más crítico** para tu rubro. Hoy el ERP maneja repuestos sin saber a qué vehículo pertenecen.

| Funcionalidad | Descripción | Valor |
|---|---|---|
| **Catálogo de vehículos** | Modelo, marca, año, motor, tipo combustible, código de chasis | Base de datos propia o importación desde una API pública |
| **Relación repuesto ↔ vehículo** | Tabla intermedia: qué repuesto sirve para qué vehículo (y por qué) | Es la knowledge base que el chatbot usará para recomendar |
| **Compatibilidad jerárquica** | Motor → Variante → Año → Repuesto (filtros en cascada) | UX rápida al vender |
| **VIN Scanner** | Leer VIN (17 caracteres) y autocompletar vehículo | Reduce errores humanos |

---

## 3. Módulo de Garantías y Devoluciones (Media-Alta)

Hoy no hay trazabilidad de qué repuesto fue instalado en qué vehículo y cuándo.

| Funcionalidad | Descripción |
|---|---|
| **Registro de instalación** | Al vender un repuesto, asociarlo a un vehículo + cliente + fecha |
| **Garantías por proveedor** | Cada proveedor tiene meses de garantía; se calcula automáticamente la fecha de vencimiento |
| **RMA / Devoluciones** | Flujo: cliente devuelve → se genera nota crédito → ingresa a stock (con estado "garantía") |
| **Historial de garantías por cliente** | Ver todos los repuestos garantizados de un cliente específico |

---

## 4. Módulo de Compras y Proveedores (Media-Alta)

Hoy el ingreso de mercadería es un formulario suelto. Falta el ciclo completo de compras.

| Funcionalidad | Descripción |
|---|---|
| **Órdenes de Compra** | Generar OC → enviar por email al proveedor → recepcionar mercadería |
| **Comparación de precios** | Historial de costo por proveedor → sugerir el más económico |
| **Evaluación de proveedores** | Rating por: tiempo de entrega, calidad (devoluciones), precio |
| **Pedidos automáticos** | Si stock < umbral, generar propuesta de pedido al proveedor habitual |

---

## 5. Módulo de Cotizaciones y Presupuestos (Media)

Tu dashboard muestra "Proforma", lo que sugiere que ya hay algo, pero podría formalizarse.

| Funcionalidad | Descripción |
|---|---|
| **Cotizaciones formateadas** | Con validez de días, descuentos, términos |
| **Conversión a Venta/Factura** | Un clic: cotización → nota de venta |
| **Comparador de cotizaciones** | Mismo repuesto, diferentes proveedores, elegir el mejor |

---

## 6. Módulo de Caja y Arqueo (Media)

Muy útil para saber el flujo real del día.

| Funcionalidad | Descripción |
|---|---|
| **Apertura/cierre de caja** | Con usuario, fecha, monto inicial |
| **Movimientos de caja** | Ventas (efectivo), gastos, retiros |
| **Arqueo de caja** | Comparar sistema vs. físico con diferencias |
| **Reporte de caja diario/mensual** | Resumen por usuario, por método de pago |

---

## 7. Módulo de Taller/Servicio (Media-Alta)

Los repuestos suelen venderse con servicio de instalación.

| Funcionalidad | Descripción |
|---|---|
| **Órdenes de Servicio** | Vehículo ingresa → se registran repuestos usados + mano de obra |
| **Mano de obra** | Catálogo de servicios con precio (cambio de aceite, alineación, etc.) |
| **Historial de servicio por vehículo** | Ver todos los servicios realizados a un VIN |
| **Agenda de taller** | Calendarizar próximos mantenimientos (cambio de aceite cada 5k km) |

---

## 8. Reportes Avanzados (Media)

Tu dashboard tiene gráficos básicos. Podrías ampliar mucho.

| Reporte | Descripción | Valor |
|---|---|---|
| **Rotación de inventario** | Índice de rotación por producto, grupo, marca | Identificar cuellos de botella |
| **Análisis ABC** | Clasificar productos por valor de ventas (A=20% productos = 80% ingresos) | Enfocar esfuerzo comercial |
| **Rentabilidad por producto** | Precio venta - costo - descuentos - comisiones | Saber qué realmente da ganancia |
| **Flujo de caja proyectado** | Cuentas por cobrar + por pagar próximas | Planificación financiera |
| **Ventas por vendedor** | Ranking, comisiones calculadas automáticamente | Gestión comercial |
| **Clientes top / morosos** | Segmentación automática | Marketing y cobranza |

---

## 9. Fidelización y Clientes (Media)

| Funcionalidad | Descripción |
|---|---|
| **Sistema de puntos** | Por cada dólar gastado, X puntos; canjeables por repuestos o descuentos |
| **Cumpleaños/recordatorios** | Enviar descuento automático por email/SMS |
| **Historial completo por cliente** | Vehículos, servicios, repuestos comprados, garantías activas |
| **Segmentación** | Clientes VIP, morosos, nuevos, inactivos |

---

## 10. Mejoras Técnicas y UX (Media-Alta)

| Funcionalidad | Descripción | Valor |
|---|---|---|
| **Escáner de códigos de barras** | Entrada/salida de mercadería escaneando (no tipeando) | Reduce errores, acelera operación |
| **Impresión de etiquetas mejorada** | Etiquetas con código de barras, precio, ubicación, vehículos compatibles | Mejor trazabilidad |
| **Búsqueda avanzada** | Por VIN, por rango de precios, por compatibilidad, por ubicación | UX más rápida |
| **Sincronización/backup en la nube** | Backup automático a Google Drive/Dropbox/S3 | Seguridad de datos |
| **Multi-sucursal** | Inventario centralizado por sucursal, transferencias entre sucursales | Escalabilidad |
| **API REST** | Exponer endpoints para integración con tienda web, app móvil | Canal de ventas adicional |
| **App móvil para vendedores** | Consultar stock, precios, generar ventas desde celular | Movilidad en el taller/tienda |

---

## 11. Seguridad y Auditoría (Media)

| Funcionalidad | Descripción |
|---|---|
| **Bitácora detallada (ya tienes LogDAO, ampliar)** | Quién eliminó/modificó/precio de venta cambió |
| **Aprobaciones** | Cambios de precio, descuentos > X% requieren aprobación de supervisor |
| **Roles granulares** | Vendedor: solo ventas; Admin: todo; Cajero: solo caja; Bodeguero: solo inventario |

---

## 12. Integración con el Chatbot IA (Alta - ya planeado)

| Funcionalidad | Descripción |
|---|---|
| **Recomendación de repuestos** | "Tengo un Toyota Corolla 2015 motor 1.8, ¿qué bujías necesita?" |
| **Diagnóstico por síntomas** | "El auto hace ruido al frenar" → sugiere pastillas, discos, etc. |
| **Consulta de stock en lenguaje natural** | "¿Tienen amortiguadores para Hilux 2020?" |
| **Explicación de precios** | "¿Por qué cuesta $X este repuesto?" → margen, proveedor, costo |
