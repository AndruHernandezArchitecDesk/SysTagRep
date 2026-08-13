package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.service.AlertaService;
import com.tag.sysTagRep.util.AboutDialog;
import com.tag.sysTagRep.util.ThemeManager;
import com.tag.sysTagRep.util.UpperCaseTextFormatter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private StackPane contenedor;

    @FXML
    private MenuItem lblUsuarioSesion;

    @FXML
    private MenuItem menuModoTema;

    @FXML
    private MenuItem menuAlertas;

    private final LogDAO logDAO = new LogDAO();
    private final AlertaService alertaService = new AlertaService();

    private boolean mostrandoDashboard2 = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mostrarUsuarioSesion();
        actualizarBadgeAlertas();
        actualizarTextoModoTema();
        abrirDashboard1();
        if (contenedor.getScene() != null) {
            ThemeManager.aplicarTemaGuardado(contenedor.getScene());
        }
    }

    public void abrirDashboard1() {
        cargarVista("/view/DashboardView.fxml");
        mostrandoDashboard2 = false;
    }

    public void abrirDashboard2() {
        cargarVista("/view/Dashboard2View.fxml");
        mostrandoDashboard2 = true;
    }

    public void abrirAlertas() {
        cargarVista("/view/AlertaView.fxml");
        actualizarBadgeAlertas();
    }

    private void mostrarUsuarioSesion() {
        if (LoginController.usuarioAutenticado == null) return;
        String apellido = LoginController.usuarioAutenticado.getApellido();
        String nombre = LoginController.usuarioAutenticado.getNombre();
        String nombreCompleto = (apellido != null ? apellido.trim() : "")
                + (nombre != null && !nombre.trim().isEmpty() ? " " + nombre.trim() : "");
        lblUsuarioSesion.setText("Bienvenido " + nombreCompleto);
    }

    private void actualizarBadgeAlertas() {
        if (menuAlertas == null) return;
        int noLeidas = alertaService.obtenerCantidadNoLeidas();
        if (noLeidas > 0) {
            menuAlertas.setText("Ver Alertas (" + noLeidas + ")");
        } else {
            menuAlertas.setText("Ver Alertas");
        }
    }

    @FXML
    private void irVendedores() {
        cargarVista("/view/VendedorView.fxml");
    }

    @FXML
    private void irAlertas() {
        abrirAlertas();
    }

    @FXML
    private void irCaja() {
        cargarVista("/view/CajaView.fxml");
    }

    @FXML
    private void irIngresoFactura() {
        cargarVista("/view/IngresoMercaderiaView.fxml");
    }

    @FXML
    private void irInventario() {
        cargarVista("/view/InventarioView.fxml");
    }

    @FXML
    private void irNotaVenta() {
        cargarVista("/view/NotaVentaView.fxml");
    }

    @FXML
    private void irFactura() {
        cargarVista("/view/FacturaView.fxml");
    }

    @FXML
    private void irUsuarios() {
        cargarVista("/view/UsuariosView.fxml");
    }

    @FXML
    private void irProveedores() {
        cargarVista("/view/ProveedorView.fxml");
    }

    @FXML
    private void irClientes() {
        cargarVista("/view/ClienteView.fxml");
    }

    @FXML
    private void irCodigos() {
        cargarVista("/view/CodigoView.fxml");
    }

    @FXML
    private void irPorCobrar() {
        cargarVista("/view/PorCobrarView.fxml");
    }

    @FXML
    private void irPorPagar() {
        cargarVista("/view/PorPagarView.fxml");
    }

    @FXML
    private void irGrupos() {
        cargarVista("/view/GrupoView.fxml");
    }

    @FXML
    private void irMarcas() {
        cargarVista("/view/MarcaView.fxml");
    }

    @FXML
    private void irUbicaciones() {
        cargarVista("/view/UbicacionView.fxml");
    }

    @FXML
    private void irUbicacionPerchero() {
        cargarVista("/view/UbicacionPercheroView.fxml");
    }

    @FXML
    private void irGestionStock() {
        cargarVista("/view/GestionStockView.fxml");
    }

    @FXML
    private void irHistorialProductos() {
        cargarVista("/view/HistorialProductoView.fxml");
    }

    @FXML
    private void irHistorialVentas() {
        cargarVista("/view/HistorialVentaView.fxml");
    }

    @FXML
    private void irHistorialCompras() {
        cargarVista("/view/HistorialCompraView.fxml");
    }

    @FXML
    private void irSeguimientoSri() {
        cargarVista("/view/SeguimientoSriView.fxml");
    }

    @FXML
    private void irComprobanteVentaReporte() {
        cargarVista("/view/ComprobanteVentaReporteView.fxml");
    }

    @FXML
    private void irFacturasIngresadas() {
        cargarVista("/view/FacturasIngresadasView.fxml");
    }

    @FXML
    private void irNumeracion() {
        cargarVista("/view/NumeracionView.fxml");
    }

    @FXML
    private void irFirma() {
        cargarVista("/view/FirmaView.fxml");
    }

    @FXML
    private void acercaDe() {
        AboutDialog.show(contenedor.getScene().getWindow());
    }

    @FXML
    private void cerrarSesion() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Cerrar sesión?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        LoginController.usuarioAutenticado = null;
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) contenedor.getScene().getWindow();
            stage.setTitle("SysTag - Inicio de Sesión");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/inventario.png")));
            Scene scene = new Scene(login);
            ThemeManager.aplicarTemaGuardado(scene);
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            logDAO.guardar("MainController", "cerrarSesion", e.getMessage(), e);
        }
    }

    @FXML
    private void cambiarTema() {
        ThemeManager.alternarTema(contenedor.getScene());
        actualizarTextoModoTema();
        if (mostrandoDashboard2) {
            abrirDashboard2();
        } else {
            abrirDashboard1();
        }
    }

    @FXML
    private void irHome() {
        try {
            Parent home = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
            Stage stage = (Stage) contenedor.getScene().getWindow();
            stage.setScene(new Scene(home));
            ThemeManager.aplicarTemaGuardado(stage.getScene());
        } catch (IOException e) {
            logDAO.guardar("MainController", "irHome", e.getMessage(), e);
        }
    }

    private void actualizarTextoModoTema() {
        if (menuModoTema == null) return;
        menuModoTema.setText(ThemeManager.esDarkMode() ? "Modo Light" : "Modo Dark");
    }

    private void cargarVista(String ruta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent vista = loader.load();
            if (loader.getController() instanceof DashboardController dashboardController) {
                dashboardController.setMainController(this);
            } else if (loader.getController() instanceof Dashboard2Controller dashboard2Controller) {
                dashboard2Controller.setMainController(this);
            }
            if (!ruta.contains("ClienteView.fxml")) {
                aplicarMayusculas(vista);
            }
            contenedor.getChildren().setAll(vista);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String detalle = sw.toString();
            String rutaFinal = ruta;
            logDAO.guardar("MainController", "cargarVista", detalle, e);
            String rutaArchivo;
            try {
                String base = System.getProperty("user.dir");
                if (base == null || base.isBlank()) {
                    base = System.getProperty("user.home");
                }
                java.nio.file.Path dir = java.nio.file.Paths.get(base, "systagrep-errors");
                java.nio.file.Files.createDirectories(dir);
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                rutaArchivo = dir.resolve("error-" + timestamp + ".txt").toString();
                java.nio.file.Files.writeString(java.nio.file.Paths.get(rutaArchivo), detalle);
            } catch (Exception ex) {
                rutaArchivo = null;
            }
            String finalRutaArchivo = rutaArchivo;
            javafx.application.Platform.runLater(() -> {
                String mensaje = "No se pudo abrir: " + rutaFinal;
                if (finalRutaArchivo != null) {
                    mensaje += "\n\nDetalle guardado en:\n" + finalRutaArchivo;
                }
                 javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                 alert.setTitle("Error al cargar vista");
                 alert.setHeaderText(mensaje);
                 alert.setContentText("Revisa el archivo de detalle para copiar el error completo.");
                 alert.showAndWait();
             });
         }
     }

     private void aplicarMayusculas(Node nodo) {
        if (nodo instanceof Parent parent) {
            for (Node n : parent.lookupAll(".text-field")) {
                UpperCaseTextFormatter.apply((TextField) n);
            }
        }
    }
}
