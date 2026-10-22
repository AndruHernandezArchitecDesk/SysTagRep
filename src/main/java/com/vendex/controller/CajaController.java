package com.vendex.controller;

import com.vendex.dao.CajaMovimientoDAO;
import com.vendex.dao.CajaSesionDAO;
import com.vendex.dao.LogDAO;
import com.vendex.dao.UsuarioDAO;
import com.vendex.model.CajaMovimiento;
import com.vendex.model.CajaSesion;
import com.vendex.service.CajaService;
import com.vendex.remote.ApiConfig;
import com.vendex.offline.OfflineHelper;
import com.vendex.offline.LocalOperationQueue;
import com.vendex.offline.OperacionOffline;
import com.vendex.offline.OfflineUI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
import com.vendex.dao.CajaSesionDAOPostgres;
import com.vendex.dao.CajaMovimientoDAOPostgres;
import com.vendex.dao.UsuarioDAOPostgres;
import com.vendex.dao.LogDAOPostgres;

public class CajaController implements Initializable {

    public CajaController() {
        this(com.vendex.config.AppContext.getInstance());
    }

    public CajaController(com.vendex.config.AppContext ctx) {
        this.cajaService = ctx.cajaService;
        this.sesionDAO = ctx.cajaSesionDAO;
        this.movimientoDAO = ctx.cajaMovimientoDAO;
        this.usuarioDAO = ctx.usuarioDAO;
        this.logDAO = ctx.logDAO;
        this.prestamoService = ctx.prestamoService;
    }

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
    @FXML private TableColumn<CajaSesion, Void> colHistAcciones;

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    private final CajaService cajaService;
    private final CajaSesionDAO sesionDAO;
    private final CajaMovimientoDAO movimientoDAO;
    private final UsuarioDAO usuarioDAO;
    private final LogDAO logDAO;
    private final com.vendex.service.PrestamoService prestamoService;
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
                        case "INGRESO" -> setStyle("-fx-font-weight: bold");
                        case "EGRESO" -> setStyle("-fx-font-weight: bold");
                        case "RETIRO" -> setStyle("-fx-font-weight: bold");
                        case "AJUSTE" -> setStyle("-fx-font-weight: bold");
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
                    if (item.compareTo(BigDecimal.ZERO) == 0) setStyle("");
                    else if (item.compareTo(BigDecimal.ZERO) > 0) setStyle("-fx-font-weight: bold");
                    else setStyle("-fx-font-weight: bold");
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
                    setStyle("ABIERTA".equals(item) ? "-fx-text-fill: -accent-green; -fx-font-weight: bold;" : "-fx-text-fill: -accent-danger; -fx-font-weight: bold;");
                }
            }
        });
        colHistAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("Ver detalle");
            {
                btnVer.setOnAction(e -> {
                    CajaSesion s = getTableRow() == null ? null : getTableRow().getItem();
                    if (s != null) mostrarDetalleSesion(s);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) setGraphic(null);
                else { setGraphic(btnVer); setAlignment(Pos.CENTER); }
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

    private String nombreUsuario(int usuarioId) {
        try {
            var u = usuarioDAO.obtenerPorId(usuarioId);
            if (u != null && u.getNombre() != null && !u.getNombre().isBlank()) return u.getNombre();
        } catch (Exception ignored) {}
        return "Usuario #" + usuarioId;
    }

    private static String dinero(BigDecimal v) {
        if (v == null) return "—";
        return "$" + v.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    /** Modal solo lectura con cabecera, totales por tipo y movimientos de la sesión. */
    private void mostrarDetalleSesion(CajaSesion s) {
        try {
            List<CajaMovimiento> movs = cajaService.obtenerMovimientos(s.getId());
            BigDecimal ing = BigDecimal.ZERO, egr = BigDecimal.ZERO,
                    ret = BigDecimal.ZERO, aju = BigDecimal.ZERO;
            for (CajaMovimiento m : movs) {
                if (m == null || m.getMonto() == null) continue;
                switch (m.getTipo() != null ? m.getTipo() : "") {
                    case "INGRESO" -> ing = ing.add(m.getMonto());
                    case "EGRESO" -> egr = egr.add(m.getMonto());
                    case "RETIRO" -> ret = ret.add(m.getMonto());
                    case "AJUSTE" -> aju = aju.add(m.getMonto());
                    default -> {}
                }
            }
            BigDecimal neto = ing.subtract(egr).subtract(ret).subtract(aju);
            BigDecimal inicial = s.getMontoInicial() != null ? s.getMontoInicial() : BigDecimal.ZERO;
            BigDecimal esperado = inicial.add(neto);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Detalle Sesión #" + s.getId());
            modal.setMinWidth(720);
            modal.setMinHeight(540);

            VBox contenido = new VBox(12);
            contenido.setPadding(new Insets(20));

            Label lblTitulo = new Label("Detalle Sesión #" + s.getId());
            lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));
            contenido.getChildren().add(lblTitulo);

            GridPane info = new GridPane();
            info.setHgap(15);
            info.setVgap(8);
            info.add(etiqueta("Apertura:"), 0, 0);
            info.add(new Label(s.getFechaApertura() != null ? s.getFechaApertura().format(fmtFecha) : "—"), 1, 0);
            info.add(etiqueta("Cierre:"), 0, 1);
            info.add(new Label(s.getFechaCierre() != null ? s.getFechaCierre().format(fmtFecha) : "—"), 1, 1);
            info.add(etiqueta("Usuario:"), 2, 0);
            info.add(new Label(nombreUsuario(s.getUsuarioId())), 3, 0);
            info.add(etiqueta("Estado:"), 2, 1);
            info.add(new Label(s.getEstado() != null ? s.getEstado() : "—"), 3, 1);
            contenido.getChildren().add(info);

            GridPane totales = new GridPane();
            totales.setHgap(15);
            totales.setVgap(6);
            String[][] filas = {
                    {"Monto inicial:", dinero(inicial)},
                    {"Ingresos:", dinero(ing)},
                    {"Egresos:", dinero(egr)},
                    {"Retiros:", dinero(ret)},
                    {"Ajustes:", dinero(aju)},
                    {"Neto (ing − egr − ret − aju):", dinero(neto)},
                    {"Esperado (inicial + neto):", dinero(esperado)},
                    {"Físico:", dinero(s.getMontoFisico())},
                    {"Diferencia:", dinero(s.getDiferencia())},
            };
            for (int i = 0; i < filas.length; i++) {
                totales.add(etiqueta(filas[i][0]), 0, i);
                totales.add(new Label(filas[i][1]), 1, i);
            }
            contenido.getChildren().add(totales);

            TableView<CajaMovimiento> tbl = new TableView<>();
            TableColumn<CajaMovimiento, LocalDateTime> cFecha = new TableColumn<>("Fecha");
            cFecha.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("fecha"));
            cFecha.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.format(fmtFecha));
                }
            });
            TableColumn<CajaMovimiento, String> cTipo = new TableColumn<>("Tipo");
            cTipo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("tipo"));
            TableColumn<CajaMovimiento, String> cDesc = new TableColumn<>("Descripción");
            cDesc.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descripcion"));
            TableColumn<CajaMovimiento, BigDecimal> cMonto = new TableColumn<>("Monto");
            cMonto.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("monto"));
            cMonto.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : dinero(item));
                }
            });
            TableColumn<CajaMovimiento, Integer> cUsu = new TableColumn<>("Usuario");
            cUsu.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("usuarioId"));
            cUsu.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : nombreUsuario(item));
                }
            });
            tbl.getColumns().addAll(cFecha, cTipo, cDesc, cMonto, cUsu);
            tbl.setItems(FXCollections.observableArrayList(movs));
            tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tbl.setPrefHeight(280);
            VBox.setVgrow(tbl, javafx.scene.layout.Priority.ALWAYS);
            contenido.getChildren().add(new Label(movs.size() + " movimiento(s)"));
            contenido.getChildren().add(tbl);

            Button btnCerrar = new Button("Cerrar");
            btnCerrar.setOnAction(e -> modal.close());
            HBox pie = new HBox(btnCerrar);
            pie.setAlignment(Pos.CENTER_RIGHT);
            contenido.getChildren().add(pie);

            modal.setScene(new javafx.scene.Scene(contenido));
            modal.showAndWait();
        } catch (Exception e) {
            logDAO.guardar("CajaController", "mostrarDetalleSesion", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el detalle: " + e.getMessage()).showAndWait();
        }
    }

    private void cargarEstado() {
        sesionActual = cajaService.obtenerSesionAbierta();
        if (sesionActual != null) {
            pnlApertura.setVisible(false);
            pnlApertura.setManaged(false);
            pnlCajaAbierta.setVisible(true);
            pnlCajaAbierta.setManaged(true);
            lblEstado.setText("Caja ABIERTA — Sesión #" + sesionActual.getId());
            lblEstado.setStyle("");
            cargarResumen();
            cargarMovimientos();
        } else {
            pnlApertura.setVisible(true);
            pnlApertura.setManaged(true);
            pnlCajaAbierta.setVisible(false);
            pnlCajaAbierta.setManaged(false);
            lblEstado.setText("Caja CERRADA");
            lblEstado.setStyle("");
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
            if (ApiConfig.isModoRemoto() && OfflineHelper.debeUsarModoOffline()) {
                abrirCajaOffline();
                return;
            }

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
        if (ApiConfig.isModoRemoto() && OfflineHelper.debeUsarModoOffline()) {
            cerrarCajaOffline();
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

                // Abonos a préstamos con el efectivo disponible antes de cerrar
                BigDecimal totalAbonado = BigDecimal.ZERO;
                if (prestamoService != null && com.vendex.util.SesionActual.tienePermiso("PRESTAMO_ABONAR")) {
                    totalAbonado = ofrecerAbonosPrestamos(montoFisico);
                    if (totalAbonado.compareTo(BigDecimal.ZERO) > 0) {
                        esperado = cajaService.calcularEsperado(sesionActual.getId());
                        diferencia = montoFisico.subtract(esperado);
                    }
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmar cierre");
                confirm.setHeaderText("Resumen de arqueo");
                String infoAbono = totalAbonado.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("%nAbonos a préstamos: $%s", totalAbonado.setScale(2, BigDecimal.ROUND_HALF_UP)) : "";
                confirm.setContentText(String.format("Esperado en caja: $%s\nMonto físico: $%s\nDiferencia: $%s%s\n\n¿Confirmar cierre?",
                        esperado.setScale(2, BigDecimal.ROUND_HALF_UP),
                        montoFisico.setScale(2, BigDecimal.ROUND_HALF_UP),
                        diferencia.setScale(2, BigDecimal.ROUND_HALF_UP),
                        infoAbono));
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    try {
                        boolean ok = cajaService.cerrarCaja(sesionActual.getId(), montoFisico, "Arqueo automático");
                        if (ok) {
                            logDAO.guardar("CajaController", "abrirCierreCaja", "Caja cerrada OK - Sesion #" + sesionActual.getId() + " - Esperado=" + esperado + " - Fisico=" + montoFisico + " - Diferencia=" + diferencia);
                            new Alert(Alert.AlertType.INFORMATION, "Caja cerrada correctamente.\nSe iniciará respaldo automático en segundo plano.").showAndWait();
                            cargarEstado();
                            dispararBackupCierreCajaAsync();
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

    /**
     * Diálogo previo al cierre: sugiere cuotas de préstamos a cubrir con el efectivo
     * disponible (monto físico − monto inicial) y registra abonos (fuente CAJA) +
     * movimientos EGRESO en la sesión actual.
     * @return total efectivamente abonado (0 si se omite)
     */
    private BigDecimal ofrecerAbonosPrestamos(BigDecimal montoFisico) {
        try {
            List<Object[]> pendientes = prestamoService.listarCuotasPendientes();
            if (pendientes == null || pendientes.isEmpty()) return BigDecimal.ZERO;

            BigDecimal inicial = sesionActual.getMontoInicial() != null ? sesionActual.getMontoInicial() : BigDecimal.ZERO;
            BigDecimal disponible = montoFisico.subtract(inicial).max(BigDecimal.ZERO);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Abonar a préstamos antes de cerrar caja");

            // Fila editable
            class Fila {
                final int cuotaId; final int prestamoId; final String entidad; final int numero;
                final java.time.LocalDate vence; final BigDecimal falta;
                final javafx.beans.property.SimpleObjectProperty<BigDecimal> monto;
                Fila(Object[] f, BigDecimal sugerido) {
                    this.cuotaId = (int) f[0];
                    this.prestamoId = (int) f[1];
                    this.entidad = (String) f[2];
                    this.numero = (int) f[3];
                    this.vence = (java.time.LocalDate) f[4];
                    this.falta = (BigDecimal) f[7];
                    this.monto = new javafx.beans.property.SimpleObjectProperty<>(sugerido);
                }
            }
            List<Fila> filas = new java.util.ArrayList<>();
            BigDecimal restante = disponible;
            for (Object[] f : pendientes) {
                BigDecimal falta = (BigDecimal) f[7];
                if (falta == null || falta.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal sugerido = falta.min(restante.max(BigDecimal.ZERO));
                filas.add(new Fila(f, sugerido));
                restante = restante.subtract(sugerido);
            }
            if (filas.isEmpty()) return BigDecimal.ZERO;

            TableView<Fila> tbl = new TableView<>();
            tbl.setPrefHeight(240);
            TableColumn<Fila, String> cEnt = new TableColumn<>("Entidad");
            cEnt.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue().entidad));
            cEnt.setPrefWidth(150);
            TableColumn<Fila, Integer> cNum = new TableColumn<>("Cuota");
            cNum.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue().numero));
            cNum.setMaxWidth(60);
            TableColumn<Fila, String> cVence = new TableColumn<>("Vence");
            cVence.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                    d.getValue().vence != null ? d.getValue().vence.toString() : ""));
            cVence.setMaxWidth(90);
            TableColumn<Fila, BigDecimal> cFalta = new TableColumn<>("Falta");
            cFalta.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue().falta));
            cFalta.setMaxWidth(90);
            TableColumn<Fila, BigDecimal> cAbonar = new TableColumn<>("A abonar (editable)");
            cAbonar.setCellValueFactory(d -> d.getValue().monto);
            cAbonar.setCellFactory(col -> new TableCell<>() {
                private final TextField txt = new TextField();
                {
                    txt.setStyle("-fx-padding: 2 4");
                    txt.textProperty().addListener((o, a, b) -> {
                        Fila f = getTableView().getItems().get(getIndex());
                        try {
                            BigDecimal v = b == null || b.trim().isEmpty() ? BigDecimal.ZERO : new BigDecimal(b.trim().replace(",", "."));
                            f.monto.set(v.max(BigDecimal.ZERO));
                        } catch (NumberFormatException ignore) {
                        }
                    });
                }
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (!txt.getText().equals(item != null ? item.toPlainString() : "")) {
                            txt.setText(item != null ? item.toPlainString() : "");
                        }
                        setGraphic(txt);
                    }
                }
            });
            cAbonar.setPrefWidth(120);
            tbl.getColumns().addAll(cEnt, cNum, cVence, cFalta, cAbonar);
            tbl.getItems().setAll(filas);

            Label lblDisp = new Label(String.format("Disponible para abonar (físico − inicial): $%s", disponible.setScale(2)));
            lblDisp.setStyle("-fx-font-weight: bold");
            Label lblTotal = new Label("Total a abonar: $0.00");
            lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 13px");
            Runnable recalcTotal = () -> {
                BigDecimal t = BigDecimal.ZERO;
                for (Fila f : filas) t = t.add(f.monto.get() != null ? f.monto.get() : BigDecimal.ZERO);
                lblTotal.setText("Total a abonar: $" + t.setScale(2) + " (quedan en caja: $"
                        + montoFisico.subtract(t).setScale(2) + ")");
            };
            for (Fila f : filas) f.monto.addListener((o, a, b) -> recalcTotal.run());
            recalcTotal.run();

            Button btnAplicar = new Button("APLICAR ABONOS Y CONTINUAR");
            btnAplicar.setStyle("-fx-font-weight: bold; -fx-padding: 8 16");
            Button btnOmitir = new Button("Omitir abonos");
            HBox botones = new HBox(10, btnAplicar, btnOmitir);
            botones.setAlignment(Pos.CENTER_RIGHT);
            final BigDecimal[] resultado = {BigDecimal.ZERO};
            btnOmitir.setOnAction(e -> modal.close());
            btnAplicar.setOnAction(e -> {
                int usuarioId = com.vendex.controller.LoginController.usuarioAutenticado != null
                        ? com.vendex.controller.LoginController.usuarioAutenticado.getId() : 1;
                BigDecimal total = BigDecimal.ZERO;
                int aplicados = 0;
                for (Fila f : filas) {
                    BigDecimal m = f.monto.get();
                    if (m == null || m.compareTo(BigDecimal.ZERO) <= 0) continue;
                    try {
                        BigDecimal aplicado = prestamoService.registrarAbono(f.prestamoId, m, f.cuotaId,
                                "CAJA", sesionActual.getId(),
                                "Abono desde cierre de caja #" + sesionActual.getId());
                        if (aplicado.compareTo(BigDecimal.ZERO) > 0) {
                            cajaService.registrarMovimiento(sesionActual.getId(), "EGRESO", aplicado,
                                    "Abono préstamo " + f.entidad + " — cuota #" + f.numero, usuarioId);
                            total = total.add(aplicado);
                            aplicados++;
                        }
                    } catch (Exception ex) {
                        logDAO.guardar("CajaController", "abonoPrestamoCierre",
                                "Préstamo " + f.prestamoId + " cuota " + f.numero + ": " + ex.getMessage(), ex);
                    }
                }
                resultado[0] = total;
                modal.close();
                if (aplicados > 0) {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Se registraron " + aplicados + " abono(s) por $" + total.setScale(2)
                                    + " a préstamos.\nEstos movimientos se descuentan del esperado en caja.").showAndWait();
                    cargarEstado();
                    cargarResumen();
                    cargarMovimientos();
                }
            });
            VBox contenido = new VBox(10,
                    new Label("Hay cuotas de préstamos pendientes. Puede abonar con el efectivo disponible antes de cerrar:"),
                    lblDisp, tbl, lblTotal, botones);
            contenido.setPadding(new Insets(15));
            modal.setScene(new javafx.scene.Scene(contenido, 700, 420));
            modal.showAndWait();
            return resultado[0];
        } catch (Exception ex) {
            logDAO.guardar("CajaController", "ofrecerAbonosPrestamos", ex.getMessage(), ex);
            return BigDecimal.ZERO;
        }
    }

    private void dispararBackupCierreCajaAsync() {
        javafx.concurrent.Task<com.vendex.backup.BackupResult> task = new javafx.concurrent.Task<>() {
            @Override protected com.vendex.backup.BackupResult call() {
                try {
                    return new com.vendex.backup.BackupService().ejecutar();
                } catch (Exception e) {
                    logDAO.guardar("CajaController", "backupCierreCaja", "Error backup cierre: " + e.getMessage(), e);
                    return com.vendex.backup.BackupResult.falla(e.getMessage());
                }
            }
        };
        task.setOnSucceeded(e -> {
            com.vendex.backup.BackupResult r = task.getValue();
            if (r.exito) {
                logDAO.guardar("CajaController", "backupCierreCaja", "Backup cierre OK: " + r.mensaje + " archivo=" + (r.archivoLocal != null ? r.archivoLocal.getName() : "null"));
                // Notificar éxito solo si configurado; falla siempre notifica
            } else {
                logDAO.guardar("CajaController", "backupCierreCaja", "Backup cierre FALLÓ: " + r.mensaje);
                new com.vendex.backup.BackupNotificationService().notificarFallaConResultado(r);
                javafx.application.Platform.runLater(() ->
                    new Alert(Alert.AlertType.WARNING, "Caja cerrada pero el respaldo falló:\n" + r.mensaje + "\nRevise Administración → Respaldos.").showAndWait()
                );
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logDAO.guardar("CajaController", "backupCierreCaja", "Backup cierre excepción: " + (ex != null ? ex.getMessage() : "null"), ex instanceof Exception ? (Exception)ex : new Exception(ex));
            new com.vendex.backup.BackupNotificationService().notificarFalla("Backup al cierre de caja falló", ex);
        });
        Thread t = new Thread(task, "vendex-backup-cierre-caja");
        t.setDaemon(true);
        t.start();
    }

    private void abrirCajaOffline() {
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
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("montoInicial", monto.toString());
            body.put("observaciones", txtObsApertura.getText());
            body.put("usuarioId", usuarioId);
            OperacionOffline op = new OperacionOffline("CAJA_ABRIR", OfflineHelper.generarPayload(body));
            LocalOperationQueue queue = new LocalOperationQueue();
            queue.encolar(op);
            txtMontoInicial.clear();
            txtObsApertura.clear();
            OfflineUI.mostrarAlertaEncoladaMensaje("Modo OFFLINE\n\nApertura de caja encolada para sincronizar cuando haya conexión.");
            cargarEstado();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.WARNING, "Monto inválido").showAndWait();
        } catch (Exception ex) {
            logDAO.guardar("CajaController", "abrirCajaOffline", ex.getMessage(), ex);
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
        }
    }

    private void cerrarCajaOffline() {
        try {
            if (sesionActual == null) {
                new Alert(Alert.AlertType.WARNING, "No hay sesión abierta").showAndWait();
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Cerrar Caja (Offline)");
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
                    java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                    body.put("sesionId", sesionActual.getId());
                    body.put("montoFisico", montoFisico.toString());
                    body.put("observaciones", "Cierre offline");
                    OperacionOffline op = new OperacionOffline("CAJA_CERRAR", OfflineHelper.generarPayload(body));
                    LocalOperationQueue queue = new LocalOperationQueue();
                    queue.encolar(op);
                    OfflineUI.mostrarAlertaEncoladaMensaje("Modo OFFLINE\n\nCierre de caja encolado para sincronizar cuando haya conexión.");
                    cargarEstado();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.WARNING, "Monto inválido").showAndWait();
                } catch (Exception ex) {
                    logDAO.guardar("CajaController", "cerrarCajaOffline", ex.getMessage(), ex);
                    new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                }
            });
        } catch (Exception ex) {
            logDAO.guardar("CajaController", "cerrarCajaOffline", ex.getMessage(), ex);
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
        }
    }
}