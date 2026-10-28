package com.vendex.controller;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Ayuda local del formulario de venta; nunca envía datos del documento a IA. */
final class AyudaFacturaCatalogo {
    record Ayuda(String titulo, String descripcion, String ejemplo) {}
    private final Map<String, Ayuda> entradas = new HashMap<>();

    AyudaFacturaCatalogo() {
        agregar("Empresa emisora", "Datos de la empresa registrada. Son informativos en esta pantalla; las correcciones se hacen en la administración de la empresa.",
                "Razón social: Repuestos Ejemplo S.A.; dirección: Av. Principal y Calle 2.",
                "imgLogo", "lblRazonSocial", "lblRuc", "lblDireccion", "lblCorreo", "lblTelefono");
        agregar("Número de factura", "Consecutivo del punto de emisión. Se asigna automáticamente y se comprueba que no esté repetido al guardar. No se escribe aquí.",
                "001-001-000000123.", "lblNumFactura");
        agregar("Factura electrónica", "Este formulario registra una venta y genera su comprobante electrónico. Su autorización se consulta en Seguimiento SRI.",
                "Selecciona un cliente, agrega los productos, revisa el total y genera la factura.", "FACTURA ELECTRÓNICA");
        agregar("Seleccionar cliente", "Busca y selecciona un cliente registrado por nombre o identificación. Es obligatorio seleccionar una opción real de la lista. La selección completa los datos inferiores. Consumidor Final permite un total de hasta $50.",
                "Escribe el nombre de un cliente existente y selecciónalo en la lista.", "cmbCliente", "Seleccionar Cliente:", "Información del Cliente");
        agregar("Administrar clientes", "Abre la administración de clientes para registrar o corregir sus datos. Al cerrar se recarga la lista de selección.",
                "Registra al nuevo comprador y luego selecciónalo en la factura.", "btnAgregarCliente");
        datoCliente("Nombre del cliente", "nombre o razón social", "María Pérez", "txtNombre", "Nombre:");
        datoCliente("Identificación del cliente", "cédula, RUC o pasaporte registrado", "Selecciona un cliente con su identificación válida registrada.", "txtIdentificacion", "RUC/Cédula:");
        datoCliente("Dirección del cliente", "dirección", "Av. Central 123 y Calle Norte", "txtDireccion", "Dirección:");
        datoCliente("Correo del cliente", "correo electrónico", "cliente@ejemplo.com", "txtCorreo", "Correo:");
        datoCliente("Teléfono del cliente", "teléfono y celular", "02 234 5678 / 099 123 4567", "txtTelefono", "Teléfono:");
        agregar("Fecha mostrada", "Se inicializa con la fecha de hoy. Actualmente cambiar este selector no transmite una fecha al servicio de guardado; no lo uses para emitir con una fecha distinta.",
                "Para una venta de hoy, conserva la fecha que aparece al abrir el formulario.", "dpFechaFactura", "Fecha:");
        agregar("Buscar un producto", "Filtra el inventario de la sucursal por código, descripción, grupo o marca, sin distinguir mayúsculas. No agrega productos por sí solo.",
                "Escribe filtro o parte del código, selecciona el resultado y pulsa AÑADIR A LA FACTURA.", "txtBuscarProducto", "INVENTARIO DISPONIBLE", "tblInventarioBusqueda");
        agregar("Añadir a la factura", "Selecciona un producto del inventario y pulsa este botón. El diálogo pide una cantidad entera mayor a cero. Si ya está en el detalle, suma unidades. El sistema permite stock negativo.",
                "Selecciona un filtro e ingresa 3 para facturar tres unidades.", "btnAgregarProducto");
        agregar("Producto temporal", "Agrega un repuesto con código nuevo. Se crea en inventario al guardar la factura y queda con stock negativo por las unidades vendidas. Código obligatorio de hasta 50 caracteres, descripción hasta 300, cantidad entera positiva y precio positivo con hasta dos decimales.",
                "Código TMP-001; descripción FILTRO ESPECIAL; cantidad 3; precio sin IVA 11.50. Al guardar deja stock −3.", "btnProductoTemporal");
        agregar("Detalle de factura", "Contiene las líneas que vas a vender. Revisa código, cantidad y precio sin IVA antes de guardar. Solo el precio unitario se edita con doble clic; para cambiar cantidades puedes quitar la línea y agregarla nuevamente.",
                "3 filtros a 11.50 sin IVA producen una base de línea de 34.50.", "tblDetalle", "DETALLE DE FACTURA");
        columna("tblInventarioBusqueda", "Código", "Código del inventario", "Identifica el repuesto que puedes buscar y agregar. Es informativo.", "FIL-001");
        columna("tblInventarioBusqueda", "Descripción", "Descripción del inventario", "Nombre del repuesto registrado. Comprueba que sea el artículo solicitado.", "FILTRO DE ACEITE");
        columna("tblInventarioBusqueda", "Marca", "Marca del repuesto", "Marca registrada del producto. También puedes usarla en el filtro de búsqueda.", "Escribe una marca existente en el buscador.");
        columna("tblInventarioBusqueda", "Stock", "Stock registrado", "Muestra la cantidad registrada en inventario. El diálogo de añadir descuenta además lo que ya incluiste en esta factura para mostrar lo disponible. Se permite vender con stock negativo.", "Stock 10 y 3 unidades ya añadidas: el diálogo muestra 7 disponibles.");
        columna("tblInventarioBusqueda", "Precio Unit. (sin IVA)", "Precio de inventario sin IVA", "Precio de venta que se copia al detalle. No incluye el IVA que se calcula al final.", "11.50 por unidad, antes de IVA.");
        columna("tblInventarioBusqueda", "Ubicación", "Ubicación en percha", "Indica dónde encontrar el producto físicamente. Es informativo.", "Percha A, nivel 2.");
        columna("tblDetalle", "Código", "Código facturado", "Identifica la línea. La marca [TEMPORAL] indica un producto nuevo que se creará al guardar.", "[TEMPORAL] TMP-001");
        columna("tblDetalle", "Descripción", "Descripción facturada", "Descripción copiada del producto o ingresada al crear el producto temporal. No se edita directamente en esta columna.", "FILTRO DE ACEITE");
        columna("tblDetalle", "Cant.", "Cantidad facturada", "Unidades ingresadas al agregar. Esta columna no se edita directamente. Añadir el mismo producto suma unidades; la papelera permite quitar la línea.", "Agregas 2 y luego 1 del mismo producto: la línea queda en 3.");
        columna("tblDetalle", "P. Unit", "Precio unitario sin IVA", "Haz doble clic para editar y confirma con Enter. Debe ser mayor a cero, con hasta 2 decimales y menos de 10000000000. Un precio inválido se revierte.", "11.50. No ingreses el precio con IVA incluido.");
        columna("tblDetalle", "Total", "Base de la línea", "Importe calculado a partir de cantidad por precio unitario sin IVA. No se llena manualmente.", "3 × 11.50 = 34.50.");
        columna("tblDetalle", "Eliminar", "Quitar producto", "La papelera elimina toda la línea y recalcula los totales. En modo guía el clic solo muestra esta explicación.", "Quita una línea incorrecta y vuelve a agregar el producto con la cantidad correcta.");
        agregar("Forma de pago", "Selecciona cómo paga el comprador: Efectivo, Tarjeta de Crédito, Tarjeta de Débito, Transferencia, Depósito, Cheque o TAG Crédito. TAG Crédito muestra plazo e interés.",
                "Transferencia para una venta pagada mediante transferencia bancaria.", "cmbFormaPago", "Forma de Pago:");
        agregar("Plazo de crédito", "Disponible con TAG Crédito. Aunque el identificador interno habla de meses, el formulario presenta Días y ofrece 5, 10, 15, 20, 25 o 30. Selecciona el plazo acordado.",
                "30 para el plazo mostrado de 30 días.", "pnlCredito", "cmbMesesPlazo", "Días:");
        agregar("Interés del crédito", "Disponible con TAG Crédito. Selecciona una tasa de 0, 3, 6, 9, 12 o 15 por ciento. El resumen de totales de esta pantalla calcula base, descuento e IVA; no agrega aquí una línea de interés.",
                "0 si no se acordó interés.", "cmbInteres", "Interés %:");
        agregar("Descuento en dólares", "Es un valor fijo, no un porcentaje. Usa números y punto decimal. Se aplica antes del IVA, se redondea a dos decimales y se limita al subtotal sin IVA.",
                "Subtotal 100.00 y descuento 5.00: IVA 14.25 y total 109.25.", "txtDescuento", "cmbDescuento", "lblDescuento", "Descuento (valor fijo):", "-");
        agregar("Subtotal sin IVA", "Suma las bases de todas las líneas, antes del descuento y del IVA. Se calcula automáticamente.",
                "3 × 11.50 = 34.50 de subtotal.", "lblSubtotal", "Subtotal:");
        agregar("IVA de la venta", "Se calcula automáticamente al 15% sobre subtotal menos descuento, con redondeo a dos decimales.",
                "Base 100.00 menos descuento 5.00: 95.00 × 15% = 14.25.", "lblIva", "IVA (15%):");
        agregar("Total a pagar", "Subtotal menos descuento más IVA. Es calculado; no se escribe manualmente.",
                "100.00 − 5.00 + 14.25 = 109.25.", "lblTotal", "TOTAL A PAGAR:");
        agregar("Ambiente SRI", "Selecciona PRUEBAS para comprobantes de ensayo o PRODUCCION para la operación real. La selección se guarda como configuración de ambiente.",
                "PRUEBAS cuando estés realizando una prueba de emisión.", "cmbAmbiente", "Ambiente SRI:", "Configuración SRI");
        agregar("Firma electrónica", "La firma se configura en Administración > Firma Electrónica. No se ingresa la clave ni el archivo de firma en este formulario.",
                "Configura el certificado antes de emitir el comprobante electrónico.", "La firma electrónica se configura una sola vez en: Administración > Firma Electrónica.");
        agregar("Generar factura electrónica", "Registra la venta. Requiere cliente seleccionado, al menos un producto y datos de empresa; verifica el número único y el límite de $50 de Consumidor Final. El flujo conectado registra pendiente SRI y procesa la autorización en segundo plano. En modo offline se encola para sincronizar. Revisa el resultado y Seguimiento SRI.",
                "Selecciona cliente, agrega 3 unidades a 11.50, revisa subtotal 34.50, IVA 5.18 y total 39.68 antes de generar.", "btnGuardar");
    }

    private void datoCliente(String titulo, String dato, String ejemplo, String... claves) {
        agregar(titulo, "Muestra el " + dato + " del cliente seleccionado. Aunque puedas escribir en este cuadro, el guardado usa el cliente de la lista; los cambios aquí no actualizan su registro. Corrige los datos en Clientes y vuelve a seleccionarlo.", ejemplo, claves);
    }

    private void agregar(String titulo, String descripcion, String ejemplo, String... claves) {
        var ayuda = new Ayuda(titulo, descripcion, ejemplo);
        for (String clave : claves) entradas.put(normalizar(clave), ayuda);
    }

    private void columna(String tabla, String columna, String titulo, String descripcion, String ejemplo) {
        agregar(titulo, descripcion, ejemplo, tabla + ":" + columna);
    }

    Ayuda buscar(String clave) { return entradas.get(normalizar(clave)); }

    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll(":$", "").replaceAll("\\s+", " ");
    }
}
