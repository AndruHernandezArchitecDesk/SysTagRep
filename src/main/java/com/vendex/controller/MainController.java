package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.config.GeminiConfig;
import com.vendex.dao.LogDAO;
import com.vendex.dao.SucursalDAO;
import com.vendex.model.Sucursal;
import com.vendex.service.AlertaService;
import com.vendex.util.AboutDialog;
import com.vendex.util.SucursalActual;
import com.vendex.util.ThemeManager;
import com.vendex.util.UpperCaseTextFormatter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import com.vendex.dao.LogDAOPostgres;

public class MainController implements Initializable {

    @FXML
    private StackPane contenedor;

    @FXML
    private MenuItem lblUsuarioSesion;

    @FXML
    private MenuItem menuModoTema;

    @FXML
    private Button btnToggleTema;

    @FXML
    private FontIcon iconToggleTema;

    @FXML
    private MenuItem menuAlertas;

    @FXML
    private ComboBox<Sucursal> cmbSucursal;

    @FXML
    private HBox bannerCertificado;

    @FXML
    private Label lblBannerCertificado;

    @FXML
    private HBox bannerSri;

    @FXML
    private Label lblBannerSri;

    private final LogDAO logDAO = new LogDAOPostgres();
    private final AlertaService alertaService = new AlertaService();

    private boolean mostrandoDashboard2 = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mostrarUsuarioSesion();
        cargarSucursales();
        actualizarBadgeAlertas();
        actualizarTextoModoTema();
        abrirDashboard1();
        if (contenedor.getScene() != null) {
            ThemeManager.aplicarTemaGuardado(contenedor.getScene());
        }
        verificarCertificadoAsync();
        iniciarBannerSriPendientes();
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

    private void cargarSucursales() {
        if (cmbSucursal == null) return;
        try {
            SucursalDAO dao = AppContext.getInstance().sucursalDAO;
            var lista = dao.listarActivas();
            if (lista.isEmpty()) {
                cmbSucursal.setVisible(false);
                cmbSucursal.setManaged(false);
                return;
            }
            cmbSucursal.getItems().setAll(lista);
            cmbSucursal.setConverter(new javafx.util.StringConverter<Sucursal>() {
                @Override public String toString(Sucursal s) { return s == null ? "" : s.getCodigo() + " - " + s.getNombre(); }
                @Override public Sucursal fromString(String string) { return null; }
            });
            int idPref = SucursalActual.getIdPersistido();
            Sucursal sel = lista.stream().filter(s -> s.getId() == idPref).findFirst()
                    .orElse(lista.stream().filter(Sucursal::isEsCentral).findFirst().orElse(lista.get(0)));
            cmbSucursal.setValue(sel);
            SucursalActual.set(sel);
            cmbSucursal.valueProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    SucursalActual.set(val);
                    logDAO.guardar("MainController", "cambiarSucursal", "Sucursal cambiada a " + val.getCodigo() + " - " + val.getNombre());
                }
            });
            boolean multi = lista.size() > 1;
            cmbSucursal.setVisible(multi);
            cmbSucursal.setManaged(multi);
        } catch (Exception e) {
            logDAO.guardar("MainController", "cargarSucursales", e.getMessage(), e);
            cmbSucursal.setVisible(false);
            cmbSucursal.setManaged(false);
        }
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
    private void irNotaCredito() {
        cargarVista("/view/NotaCreditoView.fxml");
    }

    @FXML
    private void irNotaDebito() {
        cargarVista("/view/NotaDebitoView.fxml");
    }

    @FXML
    private void irGuiaRemision() {
        cargarVista("/view/GuiaRemisionView.fxml");
    }

    @FXML
    private void irRetencion() {
        cargarVista("/view/RetencionView.fxml");
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
    private void irConfiguracionEmail() {
        cargarVista("/view/ConfiguracionEmailView.fxml");
    }

    @FXML
    private void irRespaldos() {
        cargarVista("/view/BackupView.fxml");
    }

    @FXML
    private void irAsistenteRepuestos() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/ChatWidget.fxml"));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof ChatWidgetController cwc) {
                javafx.application.Platform.runLater(cwc::abrir);
            }
            Stage stage = new Stage();
            stage.setTitle("Asistente de Repuestos - Vendex");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(contenedor.getScene().getWindow());
            Scene scene = new Scene(root, 400, 500);
            ThemeManager.aplicarTemaGuardado(scene);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            logDAO.guardar("MainController", "irAsistenteRepuestos", e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo abrir el asistente: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    private void irConfigurarIA() {
        javafx.scene.control.ChoiceDialog<String> tipoDlg = new javafx.scene.control.ChoiceDialog<>("Gemini", "Gemini", "NVIDIA");
        tipoDlg.setTitle("Configurar IA");
        tipoDlg.setHeaderText("Seleccione proveedor");
        tipoDlg.setContentText("Proveedor:");
        var tipoRes = tipoDlg.showAndWait();
        if (tipoRes.isEmpty()) return;
        boolean esNvidia = "NVIDIA".equals(tipoRes.get());
        String keyActual = esNvidia ? com.vendex.config.NvidiaConfig.obtenerApiKey() : GeminiConfig.obtenerApiKey();
        javafx.scene.control.TextInputDialog dlg = new javafx.scene.control.TextInputDialog(keyActual != null ? keyActual : "");
        dlg.setTitle("Configurar IA - " + tipoRes.get());
        dlg.setHeaderText(tipoRes.get() + " API Key");
        dlg.setContentText("API Key" + (esNvidia ? " (https://build.nvidia.com):" : " (https://aistudio.google.com/apikey):"));
        var result = dlg.showAndWait();
        if (result.isPresent()) {
            String key = result.get().trim();
            if (esNvidia) {
                if (key.isEmpty()) {
                    com.vendex.config.NvidiaConfig.borrarApiKey();
                    new Alert(Alert.AlertType.INFORMATION, "NVIDIA API key eliminada.", ButtonType.OK).showAndWait();
                } else {
                    com.vendex.config.NvidiaConfig.guardarApiKey(key);
                    new Alert(Alert.AlertType.INFORMATION, "NVIDIA API key guardada cifrada en ~/.vendex/nvidia.properties", ButtonType.OK).showAndWait();
                }
            } else {
                if (key.isEmpty()) {
                    GeminiConfig.borrarApiKey();
                    new Alert(Alert.AlertType.INFORMATION, "API key eliminada. El chat usará modo local sin IA.", ButtonType.OK).showAndWait();
                } else {
                    GeminiConfig.guardarApiKey(key);
                    new Alert(Alert.AlertType.INFORMATION, "API key guardada cifrada en ~/.vendex/gemini.properties\nBadge mostrará ● IA Gemini", ButtonType.OK).showAndWait();
                }
            }
        }
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
        com.vendex.util.SesionActual.cerrar();
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) contenedor.getScene().getWindow();
            stage.setTitle("Vendex - Inicio de Sesión");
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
        boolean dark = ThemeManager.esDarkMode();
        if (menuModoTema != null) menuModoTema.setText(dark ? "Modo Light" : "Modo Dark");
        if (iconToggleTema != null) iconToggleTema.setIconLiteral(dark ? "fas-sun" : "fas-moon");
        if (btnToggleTema != null) btnToggleTema.setText(dark ? "Light" : "Dark");
    }

    private void verificarCertificadoAsync() {
        Task<com.vendex.service.CertificadoAlertaService.ResultadoAlerta> task = new Task<>() {
            @Override protected com.vendex.service.CertificadoAlertaService.ResultadoAlerta call() {
                try { return new com.vendex.service.CertificadoAlertaService().verificarEstadoCertificado(); } catch (Exception e) { return null; }
            }
        };
        task.setOnSucceeded(e -> {
            var res = task.getValue();
            if (res == null) return;
            if (res.debeMostrarBanner) mostrarBannerCertificado(res);
            if (res.debeMostrarModal) mostrarModalCertificado(res);
        });
        new Thread(task, "Hilo-Verifica-Certificado").start();
    }

    private void mostrarBannerCertificado(com.vendex.service.CertificadoAlertaService.ResultadoAlerta res) {
        if (bannerCertificado == null || lblBannerCertificado == null) return;
        String nivel = res.info.getNivel().name();
        String colorFondo = switch (nivel) {
            case "AVISO" -> "#fff3cd";
            case "ADVERTENCIA" -> "#ffe0b2";
            case "CRITICO" -> "#f8d7da";
            case "EXPIRADO" -> "#f5c6cb";
            default -> "#fff3cd";
        };
        String colorTexto = switch (nivel) {
            case "AVISO" -> "#856404";
            case "ADVERTENCIA" -> "#7a3e00";
            case "CRITICO", "EXPIRADO" -> "#721c24";
            default -> "#856404";
        };
        bannerCertificado.setStyle("-fx-background-color: " + colorFondo + "; -fx-border-color: #ffc107; -fx-border-width: 0 0 1 0; -fx-padding: 8 12;");
        lblBannerCertificado.setStyle("-fx-text-fill: " + colorTexto + "; -fx-font-weight: bold;");
        lblBannerCertificado.setText(res.mensajeBanner + "  (Titular: " + res.info.getTitular().split(",")[0] + " | Expira: " + res.info.getFechaExpiracion() + " | " + res.info.getDiasRestantes() + " días)");
        bannerCertificado.setVisible(true);
        bannerCertificado.setManaged(true);
    }

    @FXML
    private void cerrarBannerCertificado() {
        if (bannerCertificado != null) { bannerCertificado.setVisible(false); bannerCertificado.setManaged(false); }
    }

    @FXML
    private void cerrarBannerSri() {
        if (bannerSri != null) { bannerSri.setVisible(false); bannerSri.setManaged(false); }
    }

    private void iniciarBannerSriPendientes() {
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> actualizarBannerSriPendientes()),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(120), e -> actualizarBannerSriPendientes())
        );
        tl.setCycleCount(javafx.animation.Animation.INDEFINITE);
        tl.play();
        // también actualizar al inicio en hilo fondo
        actualizarBannerSriPendientes();
    }

    private void actualizarBannerSriPendientes() {
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                try { return new com.vendex.dao.ComprobantePendienteSriDAOPostgres().contarPendientes(); } catch (Exception e) { return 0; }
            }
        };
        task.setOnSucceeded(e -> {
            int c = task.getValue() != null ? task.getValue() : 0;
            if (bannerSri == null || lblBannerSri == null) return;
            if (c > 0) {
                lblBannerSri.setText(c + " comprobante(s) esperando autorización del SRI — servicio con demoras, se reintenta cada 2min automáticamente");
                bannerSri.setVisible(true);
                bannerSri.setManaged(true);
            } else {
                bannerSri.setVisible(false);
                bannerSri.setManaged(false);
            }
        });
        new Thread(task, "Hilo-Banner-SRI").start();
    }

    @FXML
    private void irConfigurarContingenciaSRI() {
        String actual = com.vendex.util.SRIContingenciaConfig.getEmailDestino();
        javafx.scene.control.TextInputDialog dlg = new javafx.scene.control.TextInputDialog(actual != null ? actual : "");
        dlg.setTitle("SRI Contingencia — Email notificaciones");
        dlg.setHeaderText("Email parametrizado por cliente (RECHAZADA / AGOTADA 24h)");
        dlg.setContentText("Email destino:");
        var res = dlg.showAndWait();
        if (res.isPresent()) {
            String email = res.get().trim();
            if (!email.isEmpty() && !email.contains("@")) {
                new Alert(Alert.AlertType.WARNING, "Email inválido").showAndWait();
                return;
            }
            com.vendex.util.SRIContingenciaConfig.setEmailDestino(email);
            new Alert(Alert.AlertType.INFORMATION, email.isEmpty() ? "Email de contingencia eliminado." : "Email guardado: " + email + "\n~/.vendex/contingencia.properties").showAndWait();
        }
    }

    @FXML
    private void irConfigurarPostgres() {
        String actual = "";
        // no mostrar password actual por seguridad, solo indicar si existe
        boolean tiene = com.vendex.config.PostgresConfig.tienePassword();
        javafx.scene.control.TextInputDialog dlg = new javafx.scene.control.TextInputDialog("");
        dlg.setTitle("Flyway — Postgres migrador");
        dlg.setHeaderText(tiene ? "Postgres password ya configurado (cifrado)" : "Configurar postgres superuser para Flyway");
        dlg.setContentText("Password postgres (se guardará cifrado en ~/.vendex/postgres.properties solo en host):");
        var res = dlg.showAndWait();
        if (res.isPresent()) {
            String pwd = res.get().trim();
            if (pwd.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Password vacío, no se guardó.").showAndWait();
                return;
            }
            try {
                com.vendex.config.PostgresConfig.guardarPassword(pwd);
                new Alert(Alert.AlertType.INFORMATION, "Password postgres guardado cifrado.\nArchivo: " + com.vendex.config.PostgresConfig.getArchivo().getAbsolutePath() + "\nReinicie para migrar.").showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Error guardando: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void mostrarModalCertificado(com.vendex.service.CertificadoAlertaService.ResultadoAlerta res) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(res.info.getNivel() == com.vendex.util.CertificadoDigitalInfo.Nivel.EXPIRADO ? Alert.AlertType.ERROR : Alert.AlertType.WARNING);
            alert.setTitle("Certificado de Firma - " + res.info.getNivel().name());
            alert.setHeaderText(res.mensajeModal != null ? res.mensajeModal.split("\n")[0] : "Certificado por expirar");
            TextArea ta = new TextArea(res.mensajeModal != null ? res.mensajeModal : res.mensajeBanner);
            ta.setWrapText(true); ta.setEditable(false); ta.setPrefHeight(220); ta.setPrefWidth(520);
            VBox box = new VBox(ta); VBox.setVgrow(ta, Priority.ALWAYS);
            alert.getDialogPane().setContent(box);
            alert.getDialogPane().setPrefSize(580, 340);
            alert.setResizable(true);
            alert.showAndWait();
        });
    }

    private boolean tienePermisoParaRuta(String ruta) {
        // mapeo ruta → permiso (si no está mapeado, permitir por compat)
        String perm = switch (ruta) {
            case "/view/FacturaView.fxml" -> "FACTURA_EMITIR";
            case "/view/NotaCreditoView.fxml" -> "NOTA_CREDITO_EMITIR";
            case "/view/NotaDebitoView.fxml" -> "NOTA_DEBITO_EMITIR";
            case "/view/GuiaRemisionView.fxml" -> "GUIA_REMISION_EMITIR";
            case "/view/RetencionView.fxml" -> "RETENCION_EMITIR";
            case "/view/CajaView.fxml" -> "CAJA_ABRIR";
            case "/view/InventarioView.fxml", "/view/GestionStockView.fxml", "/view/UbicacionPercheroView.fxml" -> "INVENTARIO_AJUSTAR";
            case "/view/UsuariosView.fxml", "/view/GestionRolesView.fxml" -> "USUARIO_GESTIONAR";
            case "/view/ConfiguracionEmailView.fxml", "/view/FirmaView.fxml", "/view/NumeracionView.fxml", "/view/BackupView.fxml" -> "CONFIGURACION_EMAIL_EDITAR";
            case "/view/DashboardView.fxml", "/view/Dashboard2View.fxml" -> "REPORTE_VER";
            default -> null;
        };
        if (perm == null) return true;
        return com.vendex.util.SesionActual.tienePermiso(perm);
    }

    @FXML private void irGestionRoles() { cargarVista("/view/GestionRolesView.fxml"); }

    private void cargarVista(String ruta) {
        if (!tienePermisoParaRuta(ruta)) {
            new Alert(Alert.AlertType.ERROR, "No tienes permiso para esta acción: " + ruta + "\nSolicita a un ADMINISTRADOR que te asigne el permiso.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent vista = loader.load();
            if (loader.getController() instanceof DashboardController dashboardController) {
                dashboardController.setMainController(this);
            } else if (loader.getController() instanceof Dashboard2Controller dashboard2Controller) {
                dashboard2Controller.setMainController(this);
            }
            aplicarMayusculas(vista);
            contenedor.getChildren().setAll(vista);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String detalle = sw.toString();
            String rutaFinal = ruta;
            logDAO.guardar("MainController", "cargarVista", detalle, e);
            String rutaArchivo;
            try {
                java.nio.file.Path dir = obtenerDirectorioBase().resolve("vendex-errors");
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

     private java.nio.file.Path obtenerDirectorioBase() {
         try {
             java.security.CodeSource cs = MainController.class.getProtectionDomain().getCodeSource();
             if (cs != null && cs.getLocation() != null) {
                 java.nio.file.Path loc = java.nio.file.Paths.get(cs.getLocation().toURI());
                 if (java.nio.file.Files.isRegularFile(loc)) {
                     java.nio.file.Path padre = loc.getParent();
                     if (padre != null && "app".equals(padre.getFileName().toString())) {
                         return padre.getParent();
                     }
                     return padre;
                 }
             }
         } catch (Exception ignored) {
         }
         String base = System.getProperty("user.dir");
         if (base == null || base.isBlank()) {
             base = System.getProperty("user.home");
         }
         return java.nio.file.Paths.get(base);
     }

       private void aplicarMayusculas(Node nodo) {
          if (nodo instanceof Parent parent) {
              for (Node n : parent.lookupAll(".text-field")) {
                  if (n instanceof TextField tf) {
                      if (tf.getStyleClass().contains("no-uppercase")) continue;
                      UpperCaseTextFormatter.apply(tf);
                  }
              }
          }
      }
}