package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CajaMovimientoDAO;
import com.tag.sysTagRep.dao.CajaSesionDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.UsuarioDAO;
import com.tag.sysTagRep.model.CajaMovimiento;
import com.tag.sysTagRep.model.CajaSesion;
import com.tag.sysTagRep.service.CajaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CajaController implements Initializable {

    @FXML private Label lblEstado;
    @FXML private VBox pnlApertura;
    @FXML private VBox pnlCajaAbierta;

    @FXML private TextField txtMontoInicial;
    @FXML private TextField txtObsApertura;

    @FXML private Label lblMontoInicial;
    @FXML private Label lblIngresos;
    @FXML private Label lblEgresos;
    @FXML private Label lblNeto;
    @FXML private Label lblEsperado;

    @FXML private ComboBox<String> cmbTipoMov;
    @FXML private TextField txtMontoMov;
    @FXML private TextField txtDescMov;
    @FXML private TableView<CajaMovimiento> tblMovimientos;

    @FXML private TableColumn<CajaMovimiento, LocalDateTime> colFecha;
    @FXML private TableColumn<CajaMovimiento, String> colTipo;
    @FXML private TableColumn<CajaMovimiento, String> colDescripcion;
    @FXML private TableColumn<CajaMovimiento, BigDecimal> colMonto;
    @FXML private TableColumn<CajaMovimiento, Integer> colUsuario;

    @FXML private TableView<CajaSesion> tblHistorial;
    @FXML private TableColumn<CajaSesion, Integer> colHistId;
    @FXML private TableColumn<CajaSesion, LocalDateTime> colHistApertura;
    @FXML private TableColumn<CajaSesion, LocalDateTime> colHistCierre;
    @FXML private TableColumn<CajaSesion, BigDecimal> colHistInicial;
    @FXML private TableColumn<CajaSesion, BigDecimal> colHistFisico;
    @FXML private TableColumn<CajaSesion, BigDecimal> colHistDiferencia;
    @FXML private TableColumn<CajaSesion, String> colHistEstado;
    @FXML private TableColumn<CajaSesion, Integer> colHistUsuario;

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    private final CajaService cajaService = new CajaService();
    private final CajaSesionDAO sesionDAO = new CajaSesionDAO();
    private final CajaMovimientoDAO movimientoDAO = new CajaMovimientoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final LogDAO logDAO = new LogDAO();
    private final ObservableList<CajaMovimiento> listaMovimientos = FXCollections.observableArrayList();
    private final ObservableList<CajaSesion> listaHistorial = FXCollections.observableArrayList();
    private CajaSesion sesionActual;

    private final DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logDAO.guardar("CajaController", "initialize", "INICIO CajaController - sesionActual=" + sesionActual);
        cmbTipoMov.getItems().addAll("INGRESO", "EGRESO", "RETIRO", "AJUSTE");
        cmbTipoMov.setValue("INGRESO");
        configurarTablaMovimientos();
        configurarTablaHistorial();
        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());
        cargarEstado();
        logDAO.guardar("CajaController", "initialize", "FIN CajaController - sesionActual=" + sesionActual);
    }

    private void configurarTablaMovimientos() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuarioId"));

        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.format(fmtFecha));
            }
        });
        colMonto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText("$" + item.setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        });
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    switch (item) {
                        case "INGRESO" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "EGRESO" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "RETIRO" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "AJUSTE" -> setStyle("-fx-text-fill: #9b59b6; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });
        tblMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configurarTablaHistorial() {
        colHistId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colHistApertura.setCellValueFactory(new PropertyValueFactory<>("fechaApertura"));
        colHistCierre.setCellValueFactory(new PropertyValueFactory<>("fechaCierre"));
        colHistInicial.setCellValueFactory(new PropertyValueFactory<>("montoInicial"));
        colHistFisico.setCellValueFactory(new PropertyValueFactory<>("montoFisico"));
        colHistDiferencia.setCellValueFactory(new PropertyValueFactory<>("diferencia"));
        colHistEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colHistUsuario.setCellValueFactory(new PropertyValueFactory<>("usuarioId"));

        colHistApertura.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(fmtFecha));
            }
        });
        colHistCierre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(fmtFecha));
            }
        });
        colHistInicial.setCellFactory(col -> monedaCell());
        colHistFisico.setCellFactory(col -> monedaCell());
        colHistDiferencia.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText("$" + item.setScale(2, BigDecimal.ROUND_HALF_UP));
                    if (item.compareTo(BigDecimal.ZERO) == 0) setStyle("-fx-text-fill: #2c3e50;");
                    else if (item.compareTo(BigDecimal.ZERO) > 0) setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
        colHistEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("ABIERTA".equals(item) ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
        tblHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private TableCell<CajaSesion, BigDecimal> monedaCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText("$" + item.setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        };
    }

    private void cargarEstado() {
        sesionActual = cajaService.obtenerSesionAbierta();
        if (sesionActual != null) {
            pnlApertura.setVisible(false);
            pnlApertura.setManaged(false);
            pnlCajaAbierta.setVisible(true);
            pnlCajaAbierta.setManaged(true);
            lblEstado.setText("Caja ABIERTA — Sesión #" + sesionActual.getId());
            lblEstado.setStyle("-fx-text-fill: #27ae60;");
            cargarResumen();
            cargarMovimientos();
        } else {
            pnlApertura.setVisible(true);
            pnlApertura.setManaged(true);
            pnlCajaAbierta.setVisible(false);
            pnlCajaAbierta.setManaged(false);
            lblEstado.setText("Caja CERRADA");
            lblEstado.setStyle("-fx-text-fill: #e74c3c;");
        }
        cargarHistorial();
    }

    private void cargarResumen() {
        if (sesionActual == null) return;
        lblMontoInicial.setText("$" + sesionActual.getMontoInicial().setScale(2, BigDecimal.ROUND_HALF_UP));
        var resumen = cajaService.obtenerResumen(sesionActual.getId());
        lblIngresos.setText("$" + resumen.get("INGRESO").setScale(2, BigDecimal.ROUND_HALF_UP));
        lblEgresos.setText("$" + resumen.get("TOTAL_EGRESOS").setScale(2, BigDecimal.ROUND_HALF_UP));
        lblNeto.setText("$" + resumen.get("NETO").setScale(2, BigDecimal.ROUND_HALF_UP));
        BigDecimal esperado = cajaService.calcularEsperado(sesionActual.getId());
        lblEsperado.setText("$" + esperado.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    private void cargarMovimientos() {
        if (sesionActual == null) return;
        listaMovimientos.setAll(cajaService.obtenerMovimientos(sesionActual.getId()));
        tblMovimientos.setItems(listaMovimientos);
    }

    @FXML
    private void cargarHistorial() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null) desde = LocalDate.now();
        if (hasta == null) hasta = LocalDate.now();
        List<CajaSesion> sesiones = cajaService.obtenerSesionesPorFecha(desde, hasta);
        listaHistorial.setAll(sesiones);
        tblHistorial.setItems(listaHistorial);
    }

    @FXML
    private void abrirCaja() {
        try {
            String montoStr = txtMontoInicial.getText().trim();
            if (montoStr.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese el monto inicial").showAndWait();
                return;
            }
            BigDecimal monto = new BigDecimal(montoStr);
            if (monto.compareTo(BigDecimal.ZERO) < 0) {
                new Alert(Alert.AlertType.WARNING, "El monto no puede ser negativo").showAndWait();
                return;
            }
            int usuarioId = LoginController.usuarioAutenticado != null ? LoginController.usuarioAutenticado.getId() : 1;
            int id = cajaService.abrirCaja(usuarioId, monto, txtObsApertura.getText());
            if (id > 0) {
                txtMontoInicial.clear();
                txtObsApertura.clear();
                cargarEstado();
                new Alert(Alert.AlertType.INFORMATION, "Caja abierta correctamente (Sesión #" + id + ")").showAndWait();
            }
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.WARNING, "Monto inválido").showAndWait();
        } catch (Exception ex) {
            logDAO.guardar("CajaController", "abrirCaja", ex.getMessage(), ex);
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void registrarMovimiento() {
        try {
            if (sesionActual == null) return;
            String tipo = cmbTipoMov.getValue();
            String montoStr = txtMontoMov.getText().trim();
            String desc = txtDescMov.getText().trim();
            if (montoStr.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese el monto").showAndWait();
                return;
            }
            BigDecimal monto = new BigDecimal(montoStr);
            int usuarioId = LoginController.usuarioAutenticado != null ? LoginController.usuarioAutenticado.getId() : 1;
            cajaService.registrarMovimiento(sesionActual.getId(), tipo, monto, desc, usuarioId);
            txtMontoMov.clear();
            txtDescMov.clear();
            cmbTipoMov.setValue("INGRESO");
            cargarResumen();
            cargarMovimientos();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.WARNING, "Monto inválido").showAndWait();
        } catch (Exception ex) {
            logDAO.guardar("CajaController", "registrarMovimiento", ex.getMessage(), ex);
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void abrirCierreCaja() {
        logDAO.guardar("CajaController", "abrirCierreCaja", "ENTRO al metodo - sesionActual=" + sesionActual);
        if (sesionActual == null) {
            logDAO.guardar("CajaController", "abrirCierreCaja", "sesionActual es null, saliendo");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cerrar Caja");
        dialog.setHeaderText("Sesión #" + sesionActual.getId() + " — Arqueo de caja");
        dialog.setContentText("Monto físico contado ($):");
        dialog.showAndWait().ifPresent(montoStr -> {
            try {
                String raw = montoStr.trim().replace(",", ".");
                if (raw.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Ingrese el monto físico").showAndWait();
                    return;
                }
                BigDecimal montoFisico = new BigDecimal(raw);
                if (montoFisico.compareTo(BigDecimal.ZERO) < 0) {
                    new Alert(Alert.AlertType.WARNING, "El monto no puede ser negativo").showAndWait();
                    return;
                }
                BigDecimal esperado = cajaService.calcularEsperado(sesionActual.getId());
                BigDecimal diferencia = montoFisico.subtract(esperado);
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmar cierre");
                confirm.setHeaderText("Resumen de arqueo");
                confirm.setContentText(String.format("Esperado en caja: $%s\nMonto físico: $%s\nDiferencia: $%s\n\n¿Confirmar cierre?",
                        esperado.setScale(2, BigDecimal.ROUND_HALF_UP),
                        montoFisico.setScale(2, BigDecimal.ROUND_HALF_UP),
                        diferencia.setScale(2, BigDecimal.ROUND_HALF_UP)));
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    try {
                        boolean ok = cajaService.cerrarCaja(sesionActual.getId(), montoFisico, "Arqueo automático");
                        if (ok) {
                            logDAO.guardar("CajaController", "abrirCierreCaja", "Caja cerrada OK - Sesion #" + sesionActual.getId() + " - Esperado=" + esperado + " - Fisico=" + montoFisico + " - Diferencia=" + diferencia);
                            new Alert(Alert.AlertType.INFORMATION, "Caja cerrada correctamente").showAndWait();
                            cargarEstado();
                        } else {
                            logDAO.guardar("CajaController", "abrirCierreCaja", "Fallo cerrarCaja - Sesion #" + sesionActual.getId() + " - ok=false");
                            new Alert(Alert.AlertType.ERROR, "No se pudo cerrar la caja. Ver logs.").showAndWait();
                        }
                    } catch (Exception ex) {
                        logDAO.guardar("CajaController", "abrirCierreCaja", "Excepcion cerrando sesion #" + sesionActual.getId() + ": " + ex.getMessage(), ex);
                        new Alert(Alert.AlertType.ERROR, "Error al cerrar: " + ex.getMessage()).showAndWait();
                    }
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Monto inválido").showAndWait();
            }
        });
    }
}
