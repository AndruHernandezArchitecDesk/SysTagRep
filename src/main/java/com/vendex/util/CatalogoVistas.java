package com.vendex.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catálogo título de botón del sidebar → vista FXML navegable.
 * Usado por los accesos directos arrastrables (barra superior).
 * Solo incluye vistas que cargan en el contenedor central (cargarVista);
 * se excluyen modales (Cola Offline, Asistente), diálogos, Inicio/Tema y Cerrar Sesión.
 */
public final class CatalogoVistas {

    private static final Map<String, String> RUTA_POR_TITULO;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Comprobantes", "/view/NotaVentaView.fxml");
        m.put("Ingreso de Facturas", "/view/IngresoMercaderiaView.fxml");
        m.put("Compras Registradas", "/view/FacturasIngresadasView.fxml");
        m.put("Proformas", "/view/NotaVentaView.fxml");
        m.put("Facturas de Venta", "/view/FacturaView.fxml");
        m.put("Notas de Crédito", "/view/NotaCreditoView.fxml");
        m.put("Notas de Débito", "/view/NotaDebitoView.fxml");
        m.put("Guías de Remisión", "/view/GuiaRemisionView.fxml");
        m.put("Retenciones", "/view/RetencionView.fxml");
        m.put("Seguimiento SRI", "/view/SeguimientoSriView.fxml");
        m.put("Reporte de Ventas", "/view/ComprobanteVentaReporteView.fxml");
        m.put("Inventario", "/view/InventarioView.fxml");
        m.put("Ubicaciones", "/view/UbicacionPercheroView.fxml");
        m.put("Productos", "/view/InventarioView.fxml");
        m.put("Stock", "/view/GestionStockView.fxml");
        m.put("Crédito", "/view/PorCobrarView.fxml");
        m.put("Cuentas por Cobrar", "/view/PorCobrarView.fxml");
        m.put("Cuentas por Pagar", "/view/PorPagarView.fxml");
        m.put("Caja", "/view/CajaView.fxml");
        m.put("Historial", "/view/HistorialVentaView.fxml");
        m.put("Historial de Productos", "/view/HistorialProductoView.fxml");
        m.put("Historial de Ventas", "/view/HistorialVentaView.fxml");
        m.put("Historial de Compras", "/view/HistorialCompraView.fxml");
        m.put("Alertas", "/view/AlertaView.fxml");
        m.put("Administración", "/view/ClienteView.fxml");
        m.put("Clientes", "/view/ClienteView.fxml");
        m.put("Usuarios", "/view/UsuariosView.fxml");
        m.put("Roles y Permisos", "/view/GestionRolesView.fxml");
        m.put("Proveedores", "/view/ProveedorView.fxml");
        m.put("Códigos", "/view/CodigoView.fxml");
        m.put("Grupos", "/view/GrupoView.fxml");
        m.put("Marcas", "/view/MarcaView.fxml");
        m.put("Numeración", "/view/NumeracionView.fxml");
        m.put("Firma Electrónica", "/view/FirmaView.fxml");
        m.put("Correo Electrónico", "/view/ConfiguracionEmailView.fxml");
        m.put("Respaldos", "/view/BackupView.fxml");
        RUTA_POR_TITULO = Collections.unmodifiableMap(m);
    }

    private CatalogoVistas() {}

    public static String rutaDe(String titulo) {
        if (titulo == null) return null;
        return RUTA_POR_TITULO.get(titulo.trim());
    }

    public static boolean esArrastrable(String titulo) {
        return rutaDe(titulo) != null;
    }
}
