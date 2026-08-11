package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CuentaPorCobrarDAO;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.UpperCaseTextFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class PorCobrarController implements Initializable {

    @FXML private TableView<Object[]> tblCreditosActivos;
    @FXML private TableColumn<Object[], String> colCliente, colFactura;
    @FXML private TableColumn<Object[], Void> colProgreso, colVerDetalle;

    @FXML private TextField txtFiltroCliente;
    @FXML private TableView<Object[]> tblDetalleCredito;
    @FXML private TableColumn<Object[], String> colDetCliente, colDetNota, colDetFecha, colDetArticulos, colDetTiempoPago;
    @FXML private TableColumn<Object[], BigDecimal> colDetTotal, colDetAdelanto, colDetPendiente;
    @FXML private TableColumn<Object[], Void> colDetAccion;

    private final CuentaPorCobrarDAO dao = new CuentaPorCobrarDAO();
    private ObservableList<Object[]> listaCreditos = FXCollections.observableArrayList();
    private ObservableList<Object[]> listaDetalle = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablaCreditos();
        configurarTablaDetalle();
        SortTable.agregarBotones(tblCreditosActivos);
        SortTable.agregarBotones(tblDetalleCredito);
        cargarCreditos();
    }

    private void configurarTablaCreditos() {
        colCliente.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>((String) data.getValue()[3]));
        colFactura.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>((String) data.getValue()[5]));

        colProgreso.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Object[] fila = getTableView().getItems().get(getIndex());
                    int dias = (int) fila[8];
                    Timestamp fechaReg = (Timestamp) fila[13];
                    if (fechaReg == null) {
                        setGraphic(new Label("Sin fecha"));
                        return;
                    }
                    LocalDateTime inicio = fechaReg.toLocalDateTime();
                    LocalDateTime fin = inicio.plusDays(dias);
                    long totalDias = ChronoUnit.DAYS.between(inicio, fin);
                    long diasTranscurridos = ChronoUnit.DAYS.between(inicio, LocalDateTime.now());
                    double progreso = totalDias > 0 ? (double) diasTranscurridos / totalDias : 1.0;
                    int diasRestantes = (int) (totalDias - diasTranscurridos);

                    ProgressBar bar = new ProgressBar(Math.min(progreso, 1.0));
                    bar.setPrefWidth(160);
                    bar.setPrefHeight(18);
                    if (progreso >= 1.0) {
                        bar.setStyle("-fx-accent: red;");
                    } else if (progreso > 0.5) {
                        bar.setStyle("-fx-accent: orange;");
                    } else {
                        bar.setStyle("-fx-accent: limegreen;");
                    }

                    String texto = String.format("%.0f%%", progreso * 100);
                    if (diasRestantes <= 0) {
                        texto += "  (VENCIDO)";
                    } else {
                        texto += "  (" + diasRestantes + " días)";
                    }
                    Label lblInfo = new Label(texto);
                    lblInfo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

                    VBox box = new VBox(3, bar, lblInfo);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });
        colProgreso.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));

        colVerDetalle.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button();
            {
                FontIcon icon = new FontIcon("fas-eye");
                icon.setIconColor(javafx.scene.paint.Color.WHITE);
                btnVer.setGraphic(icon);
                btnVer.setStyle("-fx-background-color: #3498db; -fx-cursor: hand; -fx-padding: 4 8;");
                btnVer.setTooltip(new Tooltip("Ver detalle de proforma"));
                btnVer.setOnAction(e -> {
                    Object[] fila = getTableView().getItems().get(getIndex());
                    mostrarDetalleNota(fila);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnVer);
            }
        });
        colVerDetalle.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));

        tblCreditosActivos.setItems(listaCreditos);
    }

    private void configurarTablaDetalle() {
        colDetCliente.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>((String) data.getValue()[3]));
        colDetNota.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>((String) data.getValue()[5]));
        colDetFecha.setCellValueFactory(data -> {
            Timestamp ts = (Timestamp) data.getValue()[6];
            return new ReadOnlyObjectWrapper<>(ts != null ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        });
        colDetArticulos.setCellValueFactory(data -> {
            int notaVentaId = (int) data.getValue()[1];
            List<String[]> detalles = dao.obtenerDetallesVenta(notaVentaId);
            StringBuilder sb = new StringBuilder();
            for (String[] d : detalles) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(d[0]).append(" x").append(d[1]);
            }
            return new ReadOnlyObjectWrapper<>(sb.toString());
        });
        colDetTiempoPago.setCellValueFactory(data -> {
            int dias = (int) data.getValue()[8];
            return new ReadOnlyObjectWrapper<>(dias + " días");
        });
        colDetTotal.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>((BigDecimal) data.getValue()[7]));
        colDetAdelanto.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>((BigDecimal) data.getValue()[11]));
        colDetPendiente.setCellValueFactory(data -> {
            Object[] fila = data.getValue();
            BigDecimal total = (BigDecimal) fila[7];
            BigDecimal adelanto = (BigDecimal) fila[11];
            return new ReadOnlyObjectWrapper<>(total.subtract(adelanto != null ? adelanto : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        });

        colDetAccion.setCellFactory(param -> new TableCell<>() {
            private final TextField txtAdelanto = new TextField();
            private final Button btnGuardar = new Button();
            {
                UpperCaseTextFormatter.apply(txtAdelanto);
                txtAdelanto.setPromptText("$");
                txtAdelanto.setPrefWidth(80);
                FontIcon icon = new FontIcon("fas-save");
                icon.setIconColor(javafx.scene.paint.Color.WHITE);
                btnGuardar.setGraphic(icon);
                btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-cursor: hand; -fx-padding: 5 8;");
                btnGuardar.setTooltip(new Tooltip("Registrar adelanto"));
                btnGuardar.setOnAction(e -> {
                    Object[] fila = getTableView().getItems().get(getIndex());
                    int cpcId = (int) fila[0];
                    BigDecimal total = (BigDecimal) fila[7];
                    BigDecimal adelantoActual = (BigDecimal) fila[11];
                    if (adelantoActual == null) adelantoActual = BigDecimal.ZERO;

                    try {
                        BigDecimal monto = new BigDecimal(txtAdelanto.getText().trim());
                        if (monto.compareTo(BigDecimal.ZERO) <= 0) return;

                        BigDecimal nuevoAdelanto = adelantoActual.add(monto);
                        if (nuevoAdelanto.compareTo(total) > 0) {
                            new Alert(Alert.AlertType.WARNING, "El adelanto excede el total pendiente.").showAndWait();
                            return;
                        }

                        dao.registrarAdelanto(cpcId, nuevoAdelanto);
                        fila[11] = nuevoAdelanto;

                        if (nuevoAdelanto.compareTo(total) >= 0) {
                            dao.marcarPagado(cpcId);
                            fila[12] = "Pagado";
                        }

                        txtAdelanto.clear();
                        tblDetalleCredito.refresh();
                        cargarCreditos();
                    } catch (NumberFormatException ex) {
                        new Alert(Alert.AlertType.WARNING, "Ingrese un monto válido.").showAndWait();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Object[] fila = getTableView().getItems().get(getIndex());
                    String estado = (String) fila[12];
                    if ("Pagado".equals(estado)) {
                        Label lblPagado = new Label("PAGADO");
                        lblPagado.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        setGraphic(lblPagado);
                    } else {
                        HBox box = new HBox(5, txtAdelanto, btnGuardar);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                    }
                }
            }
        });
        colDetAccion.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
        tblDetalleCredito.setItems(listaDetalle);
    }

    private void mostrarDetalleNota(Object[] fila) {
        int notaVentaId = (int) fila[1];
        String cliente = (String) fila[3];
        String factura = (String) fila[5];
        Timestamp fecha = (Timestamp) fila[6];
        BigDecimal total = (BigDecimal) fila[7];
        int dias = (int) fila[8];
        BigDecimal interes = (BigDecimal) fila[9];
        BigDecimal cuotaMensual = (BigDecimal) fila[10];
        BigDecimal adelanto = (BigDecimal) fila[11];
        if (adelanto == null) adelanto = BigDecimal.ZERO;

        List<String[]> detalles = dao.obtenerDetallesVenta(notaVentaId);

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Detalle Proforma " + factura);
        modal.setResizable(true);

        VBox contenido = new VBox(12);
        contenido.setPadding(new Insets(20));
        contenido.setStyle("-fx-background-color: white;");

        Label lblTitulo = new Label("Detalle de Proforma");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane info = new GridPane();
        info.setHgap(15);
        info.setVgap(8);

        info.add(crearLabel("Cliente:"), 0, 0);
        info.add(crearLabelBold(cliente), 1, 0);
        info.add(crearLabel("Factura:"), 0, 1);
        info.add(crearLabelBold(factura), 1, 1);
        info.add(crearLabel("Fecha:"), 0, 2);
        info.add(crearLabelBold(fecha != null ? fecha.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""), 1, 2);
        info.add(crearLabel("Plazo:"), 2, 0);
        info.add(crearLabelBold(dias + " días"), 3, 0);
        info.add(crearLabel("Interés:"), 2, 1);
        info.add(crearLabelBold(interes + "%"), 3, 1);
        info.add(crearLabel("Cuota:"), 2, 2);
        info.add(crearLabelBold("$" + cuotaMensual.setScale(2, RoundingMode.HALF_UP)), 3, 2);

        TableView<String[]> tblDetalles = new TableView<>();
        TableColumn<String[], String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()[0]));
        colDesc.setPrefWidth(300);
        TableColumn<String[], String> colCant = new TableColumn<>("Cantidad");
        colCant.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()[1]));
        colCant.setPrefWidth(80);
        TableColumn<String[], String> colPrecio = new TableColumn<>("P. Unitario");
        colPrecio.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()[2]));
        colPrecio.setPrefWidth(100);
        TableColumn<String[], String> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()[3]));
        colSub.setPrefWidth(100);

        tblDetalles.getColumns().addAll(colDesc, colCant, colPrecio, colSub);
        tblDetalles.getItems().addAll(detalles);
        tblDetalles.setFixedCellSize(30);
        tblDetalles.setMinHeight(35);

        HBox totales = new HBox(30);
        totales.setAlignment(Pos.CENTER_RIGHT);
        totales.getChildren().addAll(
                crearLabel("Total: $" + total.setScale(2, RoundingMode.HALF_UP)),
                crearLabel("Pagado: $" + adelanto.setScale(2, RoundingMode.HALF_UP)),
                crearLabelBold("Pendiente: $" + total.subtract(adelanto).setScale(2, RoundingMode.HALF_UP))
        );

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnCerrar.setOnAction(e -> modal.close());

        contenido.getChildren().addAll(lblTitulo, new Separator(), info, new Separator(), tblDetalles, totales, btnCerrar);

        Scene escena = new Scene(contenido, 750, 550);
        modal.setScene(escena);
        modal.showAndWait();
    }

    private Label crearLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 13px;");
        return lbl;
    }

    private Label crearLabelBold(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        return lbl;
    }

    private void cargarCreditos() {
        List<Object[]> creditos = dao.listarCreditosActivos();
        listaCreditos.setAll(creditos);
        listaDetalle.setAll(creditos);
    }

    @FXML
    private void filtrarPorCliente() {
        String filtro = txtFiltroCliente.getText().trim().toLowerCase();
        if (filtro.isEmpty()) {
            cargarCreditos();
            return;
        }
        List<Object[]> todos = dao.listarCreditosActivos();
        ObservableList<Object[]> filtrados = FXCollections.observableArrayList();
        for (Object[] fila : todos) {
            String nombre = ((String) fila[3]).toLowerCase();
            String ident = ((String) fila[4]).toLowerCase();
            if (nombre.contains(filtro) || ident.contains(filtro)) {
                filtrados.add(fila);
            }
        }
        listaCreditos.setAll(filtrados);
        listaDetalle.setAll(filtrados);
    }

    @FXML
    private void limpiarFiltro() {
        txtFiltroCliente.clear();
        cargarCreditos();
    }
}
