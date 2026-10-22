package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.dao.CajaMovimientoDAO;
import com.vendex.dao.FacturaRegistroDAO;
import com.vendex.model.CajaMovimiento;
import com.vendex.model.EntidadFinanciera;
import com.vendex.model.FacturaRegistro;
import com.vendex.model.Prestamo;
import com.vendex.model.PrestamoCuota;
import com.vendex.model.PrestamoPago;
import com.vendex.service.PrestamoService;
import com.vendex.util.SortTable;
import com.vendex.util.ThemeManager;
import javafx.application.Platform;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PrestamoController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_HM_CORTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final int LIMITE_INGRESOS = 50;
    private static final String[] PERIODICIDADES = {"SEMANAL", "MENSUAL"};
    private static final String[] META_PERIODICIDADES = {"NINGUNA", "DIARIA", "SEMANAL"};

    private final PrestamoService prestamoService;
    private final FacturaRegistroDAO facturaRegistroDAO;
    private final CajaMovimientoDAO cajaMovimientoDAO;

    public PrestamoController() {
        this(AppContext.getInstance());
    }

    public PrestamoController(AppContext ctx) {
        this.prestamoService = ctx.prestamoService;
        this.facturaRegistroDAO = ctx.facturaRegistroDAO;
        this.cajaMovimientoDAO = ctx.cajaMovimientoDAO;
    }

    @FXML private TableView<Object[]> tblPrestamos;
    @FXML private TableColumn<Object[], String> colEntidad, colTasa, colCuotas, colEstado;
    @FXML private TableColumn<Object[], BigDecimal> colPrincipal, colCuota, colSaldo;
    @FXML private TableColumn<Object[], String> colProxVenc, colMeta;
    @FXML private TableColumn<Object[], Void> colProgreso;

    @FXML private Label lblResumen;
    @FXML private TableView<PrestamoCuota> tblCuotas;
    @FXML private TableColumn<PrestamoCuota, Integer> colCuoNum;
    @FXML private TableColumn<PrestamoCuota, String> colCuoVence, colCuoFechaPago, colCuoEstado;
    @FXML private TableColumn<PrestamoCuota, BigDecimal> colCuoMonto, colCuoInteres, colCuoCapital, colCuoSaldoAnt, colCuoSaldoRest, colCuoPagado;

    @FXML private TableView<PrestamoPago> tblPagos;
    @FXML private TableColumn<PrestamoPago, String> colPagFecha, colPagFuente, colPagNotas;
    @FXML private TableColumn<PrestamoPago, Integer> colPagCuota;
    @FXML private TableColumn<PrestamoPago, BigDecimal> colPagMonto, colPagInteres, colPagCapital;

    @FXML private TableView<Object[]> tblIngresos;
    @FXML private TableColumn<Object[], String> colIngFecha, colIngOrigen, colIngRef, colIngForma;
    @FXML private TableColumn<Object[], BigDecimal> colIngMonto;
    @FXML private TitledPane paneIngresos;
    @FXML private Button btnToggleIngresos;
    private boolean ingresosOcultos;

    private final ObservableList<Object[]> listaPrestamos = FXCollections.observableArrayList();
    private final ObservableList<Object[]> listaIngresos = FXCollections.observableArrayList();
    private Prestamo prestamoSeleccionado;
    private Stage modalAbierto;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTablaPrestamos();
        configurarTablaCuotas();
        configurarTablaPagos();
        configurarTablaIngresos();
        SortTable.agregarBotones(tblPrestamos);
        SortTable.agregarBotones(tblCuotas);
        SortTable.agregarBotones(tblPagos);
        tblPrestamos.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) cargarDetalle((int) sel[0]);
        });
        refrescar();
    }

    @FXML
    private void refrescar() {
        try {
            listaPrestamos.setAll(prestamoService.listarPrestamos());
            tblPrestamos.refresh();
            if (prestamoSeleccionado != null) {
                cargarDetalle(prestamoSeleccionado.getId());
            }
        } catch (Exception ex) {
            mostrarError("Error al cargar préstamos", ex);
        }
        cargarIngresos();
    }

    @FXML
    private void toggleIngresos() {
        ingresosOcultos = !ingresosOcultos;
        if (paneIngresos != null) {
            paneIngresos.setVisible(!ingresosOcultos);
            paneIngresos.setManaged(!ingresosOcultos);
        }
        if (btnToggleIngresos != null && btnToggleIngresos.getGraphic() instanceof FontIcon icono)
            icono.setIconLiteral(ingresosOcultos ? "fas-chevron-left" : "fas-chevron-right");
        if (btnToggleIngresos != null) {
            String texto = ingresosOcultos ? "Mostrar el panel de ingresos recientes"
                    : "Ocultar el panel de ingresos recientes";
            if (btnToggleIngresos.getTooltip() == null) btnToggleIngresos.setTooltip(new Tooltip(texto));
            else btnToggleIngresos.getTooltip().setText(texto);
        }
    }

    // ==================== Tabla de préstamos ====================

    private void configurarTablaPrestamos() {
        colEntidad.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((String) d.getValue()[2]));
        colPrincipal.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((BigDecimal) d.getValue()[3]));
        colTasa.setCellValueFactory(d -> {
            BigDecimal tasa = (BigDecimal) d.getValue()[4];
            String per = (String) d.getValue()[5];
            return new ReadOnlyObjectWrapper<>(tasa != null ? tasa.setScale(2, RoundingMode.HALF_UP) + "% / " + ("S".equals(per) ? "sem" : "mes") : "");
        });
        colCuota.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((BigDecimal) d.getValue()[7]));
        colCuotas.setCellValueFactory(d -> {
            int pag = (int) d.getValue()[16];
            int total = (int) d.getValue()[6];
            return new ReadOnlyObjectWrapper<>(pag + "/" + total);
        });
        colSaldo.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((BigDecimal) d.getValue()[15]));
        colProxVenc.setCellValueFactory(d -> {
            LocalDate v = (LocalDate) d.getValue()[17];
            return new ReadOnlyObjectWrapper<>(v != null ? v.format(FMT) : "—");
        });
        colMeta.setCellValueFactory(d -> {
            String per = (String) d.getValue()[12];
            BigDecimal monto = (BigDecimal) d.getValue()[13];
            if (per == null || monto == null) return new ReadOnlyObjectWrapper<>("—");
            return new ReadOnlyObjectWrapper<>(monto.setScale(2, RoundingMode.HALF_UP) + " / " + per.toLowerCase());
        });
        colEstado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((String) d.getValue()[11]));
        colProgreso.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().size() <= getIndex()) {
                    setGraphic(null);
                    return;
                }
                Object[] fila = getTableView().getItems().get(getIndex());
                int pagadas = (int) fila[16];
                int total = (int) fila[6];
                double prog = total > 0 ? (double) pagadas / total : 0;
                ProgressBar bar = new ProgressBar(prog);
                bar.setPrefWidth(120);
                bar.setPrefHeight(16);
                Label lbl = new Label(pagadas + "/" + total + " (" + String.format("%.0f%%", prog * 100) + ")");
                lbl.setStyle("-fx-font-size: 11px");
                VBox box = new VBox(2, bar, lbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        colProgreso.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(null));
        tblPrestamos.setItems(listaPrestamos);
    }

    // ==================== Tabla de cuotas ====================

    private void configurarTablaCuotas() {
        colCuoNum.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getNumero()));
        colCuoVence.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue().getFechaVencimiento() != null ? d.getValue().getFechaVencimiento().format(FMT) : ""));
        colCuoMonto.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMonto()));
        colCuoInteres.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getInteres()));
        colCuoCapital.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCapital()));
        colCuoSaldoAnt.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getSaldoAnterior()));
        colCuoSaldoRest.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getSaldoRestante()));
        colCuoPagado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMontoPagado()));
        colCuoEstado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getEstado()));
        colCuoFechaPago.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue().getFechaPago() != null ? d.getValue().getFechaPago().format(FMT) : ""));
        colCuoEstado.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PAGADA".equalsIgnoreCase(item)) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32");
                    } else if ("PARCIAL".equalsIgnoreCase(item)) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #ef6c00");
                    } else {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #c62828");
                    }
                }
            }
        });
    }

    // ==================== Tabla de pagos ====================

    private void configurarTablaPagos() {
        colPagFecha.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue().getFecha() != null ? d.getValue().getFecha().format(FMT_HM) : ""));
        colPagCuota.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCuotaId()));
        colPagMonto.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMonto()));
        colPagInteres.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getInteresParte()));
        colPagCapital.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCapitalParte()));
        colPagFuente.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getFuente()));
        colPagNotas.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getNotas()));
    }

    // ==================== Panel de ingresos recientes ====================

    private void configurarTablaIngresos() {
        tblIngresos.setTooltip(new Tooltip("Últimos ingresos: ventas de contado y movimientos de caja (tipo INGRESO)"));
        colIngFecha.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue()[0] instanceof LocalDateTime f ? f.format(FMT_HM_CORTO) : ""));
        colIngOrigen.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((String) d.getValue()[1]));
        colIngRef.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue()[2] != null ? String.valueOf(d.getValue()[2]) : ""));
        colIngForma.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue()[3] != null ? String.valueOf(d.getValue()[3]) : ""));
        colIngMonto.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>((BigDecimal) d.getValue()[4]));
        colIngMonto.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.setScale(2, RoundingMode.HALF_UP).toPlainString());
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });
        colIngOrigen.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });
        tblIngresos.setItems(listaIngresos);
    }

    private void cargarIngresos() {
        try {
            List<FacturaRegistro> facturas = facturaRegistroDAO.listarContadoReciente(LIMITE_INGRESOS);
            List<CajaMovimiento> movimientos = cajaMovimientoDAO.listarIngresosRecientes(LIMITE_INGRESOS);
            listaIngresos.setAll(mezclarIngresos(facturas, movimientos, LIMITE_INGRESOS));
        } catch (Exception ex) {
            listaIngresos.clear();
        }
    }

    /**
     * Intercala ventas de contado y movimientos de caja de tipo INGRESO,
     * ordenados por fecha descendente y recortados al límite indicado.
     * Fila: [0]=LocalDateTime, [1]=origen, [2]=referencia, [3]=forma/tipo, [4]=monto.
     */
    static List<Object[]> mezclarIngresos(List<FacturaRegistro> facturas, List<CajaMovimiento> movimientos, int limite) {
        List<Object[]> filas = new ArrayList<>();
        if (facturas != null) {
            for (FacturaRegistro f : facturas) {
                LocalDateTime fecha = f.getFecha() != null ? f.getFecha() : f.getFechaRegistro();
                filas.add(new Object[]{fecha, "VENTA", f.getCodigo(), f.getFormaPago(), f.getTotal()});
            }
        }
        if (movimientos != null) {
            for (CajaMovimiento m : movimientos) {
                filas.add(new Object[]{m.getFecha(), "CAJA", m.getDescripcion(), m.getTipo(), m.getMonto()});
            }
        }
        filas.sort((a, b) -> {
            LocalDateTime fa = (LocalDateTime) a[0];
            LocalDateTime fb = (LocalDateTime) b[0];
            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fb.compareTo(fa);
        });
        if (filas.size() > limite) return new ArrayList<>(filas.subList(0, limite));
        return filas;
    }

    // ==================== Detalle ====================

    private void cargarDetalle(int prestamoId) {
        try {
            prestamoSeleccionado = prestamoService.obtenerPrestamo(prestamoId);
            if (prestamoSeleccionado == null) return;
            List<PrestamoCuota> cuotas = prestamoService.listarCuotas(prestamoId);
            tblCuotas.setItems(FXCollections.observableArrayList(cuotas));
            tblPagos.setItems(FXCollections.observableArrayList(prestamoService.listarPagos(prestamoId)));
            BigDecimal saldo = prestamoService.saldoPendiente(prestamoId);
            int pagadas = 0;
            for (PrestamoCuota c : cuotas) if (c.estaPagada()) pagadas++;
            PrestamoCuota prox = prestamoService.proximaCuota(prestamoId);
            String meta = prestamoSeleccionado.getMetaMonto() != null
                    ? " | Meta " + prestamoSeleccionado.getMetaPeriodicidad().toLowerCase() + ": $" + prestamoSeleccionado.getMetaMonto().setScale(2)
                    : "";
            lblResumen.setText(String.format("%s — Principal $%s — Cuota $%s — Pagadas %d/%d — Saldo $%s — Próx. cuota #%d %s%s",
                    prestamoSeleccionado.getEntidadNombre(),
                    prestamoSeleccionado.getMontoPrincipal().setScale(2),
                    prestamoSeleccionado.getCuotaMonto().setScale(2),
                    pagadas, cuotas.size(),
                    saldo.setScale(2),
                    prox != null ? prox.getNumero() : 0,
                    prox != null && prox.getFechaVencimiento() != null ? prox.getFechaVencimiento().format(FMT) : "—",
                    meta));
        } catch (Exception ex) {
            mostrarError("Error al cargar detalle", ex);
        }
    }

    // ==================== Nuevo préstamo ====================

    @FXML
    private void nuevoPrestamo() {
        try {
            List<EntidadFinanciera> entidades = prestamoService.listarEntidades(true);
            if (entidades.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "Primero registre una entidad financiera (banco o particular).");
                gestionarEntidades();
                return;
            }
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nuevo Préstamo");
            modal.setResizable(true);

            GridPane form = new GridPane();
            form.setHgap(12);
            form.setVgap(10);
            form.setPadding(new Insets(5));

            ComboBox<EntidadFinanciera> cmbEntidad = new ComboBox<>(FXCollections.observableArrayList(entidades));
            cmbEntidad.setPrefWidth(220);
            cmbEntidad.setValue(entidades.get(0));
            TextField txtMonto = new TextField();
            txtMonto.setPromptText("0.00");
            TextField txtTasa = new TextField();
            txtTasa.setPromptText("5.00 (% por período)");
            ComboBox<String> cmbPeriodicidad = new ComboBox<>(FXCollections.observableArrayList(PERIODICIDADES));
            cmbPeriodicidad.setValue("MENSUAL");
            TextField txtCuotas = new TextField();
            txtCuotas.setPromptText("6");
            DatePicker dpInicio = new DatePicker(LocalDate.now());
            ComboBox<String> cmbMetaPer = new ComboBox<>(FXCollections.observableArrayList(META_PERIODICIDADES));
            cmbMetaPer.setValue("NINGUNA");
            TextField txtMetaMonto = new TextField();
            txtMetaMonto.setPromptText("0.00");
            txtMetaMonto.setDisable(true);
            Button btnSugerirMeta = new Button("SUGERIR");
            btnSugerirMeta.setTooltip(new Tooltip("Sugerir una meta de ventas según la cuota calculada"));
            btnSugerirMeta.setDisable(true);
            TextArea txtNotas = new TextArea();
            txtNotas.setPrefRowCount(2);
            txtNotas.setPromptText("Observaciones (opcional)");

            cmbMetaPer.valueProperty().addListener((o, a, b) -> {
                boolean conMeta = b != null && !"NINGUNA".equals(b);
                txtMetaMonto.setDisable(!conMeta);
                btnSugerirMeta.setDisable(!conMeta);
            });
            btnSugerirMeta.setOnAction(e -> {
                try {
                    BigDecimal cuota = calcularCuotaPreview(txtMonto, txtTasa, txtCuotas);
                    if (cuota == null) return;
                    BigDecimal sugerida = PrestamoService.calcularMetaAutomatica(cuota,
                            "SEMANAL".equals(cmbPeriodicidad.getValue()) ? "S" : "M", cmbMetaPer.getValue());
                    txtMetaMonto.setText(sugerida.toPlainString());
                } catch (Exception ex) {
                    mostrarError("Error calculando meta sugerida", ex);
                }
            });

            form.add(crearLabel("Entidad financiera:"), 0, 0);
            form.add(cmbEntidad, 1, 0);
            form.add(crearLabel("Monto principal ($):"), 0, 1);
            form.add(txtMonto, 1, 1);
            form.add(crearLabel("Tasa del período (%):"), 0, 2);
            form.add(txtTasa, 1, 2);
            form.add(crearLabel("Periodicidad:"), 0, 3);
            form.add(cmbPeriodicidad, 1, 3);
            form.add(crearLabel("Número de cuotas:"), 0, 4);
            form.add(txtCuotas, 1, 4);
            form.add(crearLabel("Fecha de inicio:"), 0, 5);
            form.add(dpInicio, 1, 5);
            form.add(crearLabel("Meta de ventas:"), 2, 0);
            form.add(cmbMetaPer, 3, 0);
            form.add(crearLabel("Monto meta ($):"), 2, 1);
            HBox boxMeta = new HBox(5, txtMetaMonto, btnSugerirMeta);
            form.add(boxMeta, 3, 1);
            form.add(crearLabel("Notas:"), 2, 2);
            form.add(txtNotas, 3, 2);

            Label lblCuota = crearLabelBold("Cuota fija: $—");
            Label lblTotal = crearLabelBold("Total a pagar: $—");
            Label lblInteres = crearLabelBold("Total intereses: $—");
            HBox resumen = new HBox(25, lblCuota, lblTotal, lblInteres);
            resumen.setPadding(new Insets(5));

            TableView<PrestamoCuota> tblPreview = new TableView<>();
            tblPreview.setPrefHeight(180);
            TableColumn<PrestamoCuota, Integer> cNum = new TableColumn<>("#");
            cNum.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getNumero()));
            cNum.setMaxWidth(45);
            TableColumn<PrestamoCuota, String> cVenc = new TableColumn<>("Vence");
            cVenc.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getFechaVencimiento().format(FMT)));
            TableColumn<PrestamoCuota, BigDecimal> cMonto = new TableColumn<>("Cuota");
            cMonto.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMonto()));
            TableColumn<PrestamoCuota, BigDecimal> cInt = new TableColumn<>("Interés");
            cInt.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getInteres()));
            TableColumn<PrestamoCuota, BigDecimal> cCap = new TableColumn<>("Capital");
            cCap.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getCapital()));
            TableColumn<PrestamoCuota, BigDecimal> cSaldo = new TableColumn<>("Saldo Rest.");
            cSaldo.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getSaldoRestante()));
            tblPreview.getColumns().addAll(cNum, cVenc, cMonto, cInt, cCap, cSaldo);

            Runnable preview = () -> {
                try {
                    BigDecimal monto = parse(txtMonto.getText());
                    BigDecimal tasa = parse(txtTasa.getText());
                    int n = (int) parse(txtCuotas.getText()).doubleValue();
                    if (monto == null || tasa == null || n <= 0) {
                        lblCuota.setText("Cuota fija: $—");
                        lblTotal.setText("Total a pagar: $—");
                        lblInteres.setText("Total intereses: $—");
                        tblPreview.getItems().clear();
                        return;
                    }
                    String per = "SEMANAL".equals(cmbPeriodicidad.getValue()) ? "S" : "M";
                    BigDecimal cuota = PrestamoService.calcularCuotaFrancesa(monto, tasa, n);
                    List<PrestamoCuota> tabla = PrestamoService.generarTablaCuotas(0, monto, tasa, n, per,
                            dpInicio.getValue() != null ? dpInicio.getValue() : LocalDate.now());
                    BigDecimal total = BigDecimal.ZERO;
                    BigDecimal interesTotal = BigDecimal.ZERO;
                    for (PrestamoCuota c : tabla) {
                        total = total.add(c.getMonto());
                        interesTotal = interesTotal.add(c.getInteres());
                    }
                    lblCuota.setText("Cuota fija: $" + cuota.setScale(2, RoundingMode.HALF_UP));
                    lblTotal.setText("Total a pagar: $" + total.setScale(2, RoundingMode.HALF_UP));
                    lblInteres.setText("Total intereses: $" + interesTotal.setScale(2, RoundingMode.HALF_UP));
                    tblPreview.getItems().setAll(tabla);
                } catch (Exception ignore) {
                }
            };
            txtMonto.textProperty().addListener((o, a, b) -> preview.run());
            txtTasa.textProperty().addListener((o, a, b) -> preview.run());
            txtCuotas.textProperty().addListener((o, a, b) -> preview.run());
            cmbPeriodicidad.valueProperty().addListener((o, a, b) -> preview.run());
            dpInicio.valueProperty().addListener((o, a, b) -> preview.run());
            preview.run();

            Button btnGuardar = new Button("GUARDAR PRÉSTAMO");
            btnGuardar.setStyle("-fx-font-weight: bold; -fx-padding: 8 20");
            btnGuardar.setTooltip(new Tooltip("Guardar el préstamo y generar la tabla de amortización"));
            FontIcon icoGuardar = new FontIcon("fas-save");
            icoGuardar.setIconColor(Color.WHITE);
            btnGuardar.setGraphic(icoGuardar);
            Button btnCancelar = new Button("Cancelar");
            btnCancelar.setTooltip(new Tooltip("Cerrar sin guardar el préstamo"));
            btnCancelar.setOnAction(e -> modal.close());
            HBox botones = new HBox(10, btnGuardar, btnCancelar);
            botones.setAlignment(Pos.CENTER_RIGHT);

            btnGuardar.setOnAction(e -> {
                try {
                    BigDecimal monto = parse(txtMonto.getText());
                    BigDecimal tasa = parse(txtTasa.getText());
                    int n = (int) parse(txtCuotas.getText()).doubleValue();
                    String per = "SEMANAL".equals(cmbPeriodicidad.getValue()) ? "S" : "M";
                    EntidadFinanciera entidad = cmbEntidad.getValue();
                    String metaPer = "NINGUNA".equals(cmbMetaPer.getValue()) ? null : cmbMetaPer.getValue();
                    BigDecimal metaMonto = metaPer != null ? parse(txtMetaMonto.getText()) : null;
                    Prestamo p = prestamoService.crearPrestamo(entidad.getId(), monto, tasa, per, n,
                            dpInicio.getValue() != null ? dpInicio.getValue() : LocalDate.now(),
                            metaPer, metaMonto, txtNotas.getText());
                    modal.close();
                    mostrarAlertaDiferida(Alert.AlertType.INFORMATION,
                            "Préstamo registrado con éxito.\nCuota: $" + p.getCuotaMonto().setScale(2)
                                    + " — Total a pagar: $" + p.getTotalAPagar().setScale(2));
                    refrescar();
                    // seleccionar el nuevo
                    for (Object[] f : listaPrestamos) {
                        if ((int) f[0] == p.getId()) {
                            tblPrestamos.getSelectionModel().select(f);
                            break;
                        }
                    }
                } catch (NumberFormatException | java.time.DateTimeException ex) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Verifique los datos ingresados: " + ex.getMessage());
                } catch (Exception ex) {
                    mostrarError("No se pudo crear el préstamo", ex);
                }
            });

            VBox contenido = new VBox(12, form, resumen,
                    crearLabel("Vista previa de la tabla de amortización:"), tblPreview, botones);
            contenido.setPadding(new Insets(15));
            modal.setScene(new Scene(contenido, 920, 580));
            ThemeManager.aplicarTemaGuardado(modal.getScene());
            modalAbierto = modal;
            try {
                modal.showAndWait();
            } finally {
                modalAbierto = null;
            }
        } catch (Exception ex) {
            mostrarError("Error abriendo formulario de préstamo", ex);
        }
    }

    // ==================== Abono ====================

    @FXML
    private void abonar() {
        if (prestamoSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Seleccione un préstamo de la lista.");
            return;
        }
        if (!"ACTIVO".equals(prestamoSeleccionado.getEstado())) {
            mostrarAlerta(Alert.AlertType.WARNING, "El préstamo no está activo.");
            return;
        }
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Abonar a préstamo — " + prestamoSeleccionado.getEntidadNombre());

        VBox cuotasBox = new VBox(5);
        cuotasBox.getChildren().add(crearLabel("Cuotas pendientes (la abono se aplica en orden cronológico):"));
        TableView<PrestamoCuota> tbl = new TableView<>();
        tbl.setPrefHeight(200);
        TableColumn<PrestamoCuota, Integer> cNum = new TableColumn<>("#");
        cNum.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getNumero()));
        cNum.setMaxWidth(45);
        TableColumn<PrestamoCuota, String> cVenc = new TableColumn<>("Vence");
        cVenc.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getFechaVencimiento().format(FMT)));
        TableColumn<PrestamoCuota, BigDecimal> cMonto = new TableColumn<>("Cuota");
        cMonto.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMonto()));
        TableColumn<PrestamoCuota, BigDecimal> cPag = new TableColumn<>("Pagado");
        cPag.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getMontoPagado()));
        TableColumn<PrestamoCuota, BigDecimal> cFalta = new TableColumn<>("Falta");
        cFalta.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getFaltante()));
        TableColumn<PrestamoCuota, String> cEst = new TableColumn<>("Estado");
        cEst.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getEstado()));
        tbl.getColumns().addAll(cNum, cVenc, cMonto, cPag, cFalta, cEst);
        for (PrestamoCuota c : prestamoService.listarCuotas(prestamoSeleccionado.getId())) {
            if (!c.estaPagada()) tbl.getItems().add(c);
        }
        cuotasBox.getChildren().add(tbl);

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("0.00");
        txtMonto.setPrefWidth(140);
        ComboBox<String> cmbFuente = new ComboBox<>(FXCollections.observableArrayList(
                "MANUAL", "CAJA", "COBRO_CLIENTE", "VENTA"));
        cmbFuente.setValue("MANUAL");
        TextArea txtNotas = new TextArea();
        txtNotas.setPrefRowCount(2);
        txtNotas.setPromptText("Notas / referencia (opcional)");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.add(crearLabel("Monto a abonar ($):"), 0, 0);
        form.add(txtMonto, 1, 0);
        form.add(crearLabel("Fuente:"), 2, 0);
        form.add(cmbFuente, 3, 0);
        form.add(crearLabel("Notas:"), 0, 1);
        form.add(txtNotas, 1, 1, 3, 1);

        BigDecimal saldo = prestamoService.saldoPendiente(prestamoSeleccionado.getId());
        Label lblSaldo = crearLabelBold("Saldo pendiente del préstamo: $" + saldo.setScale(2));

        Button btnAplicar = new Button("REGISTRAR ABONO");
        btnAplicar.setStyle("-fx-font-weight: bold; -fx-padding: 8 20");
        btnAplicar.setTooltip(new Tooltip("Registrar el abono y aplicarlo a las cuotas en orden cronológico"));
        FontIcon ico = new FontIcon("fas-hand-holding-usd");
        ico.setIconColor(Color.WHITE);
        btnAplicar.setGraphic(ico);
        Button btnCerrar = new Button("Cancelar");
        btnCerrar.setTooltip(new Tooltip("Cerrar sin registrar el abono"));
        btnCerrar.setOnAction(e -> modal.close());
        HBox botones = new HBox(10, btnAplicar, btnCerrar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        btnAplicar.setOnAction(e -> {
            try {
                BigDecimal monto = parse(txtMonto.getText());
                if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Ingrese un monto válido mayor a cero.");
                    return;
                }
                if (monto.compareTo(saldo) > 0) {
                    mostrarAlerta(Alert.AlertType.WARNING, "El abono excede el saldo pendiente ($" + saldo.setScale(2) + ").");
                    return;
                }
                Integer cuotaId = null;
                PrestamoCuota sel = tbl.getSelectionModel().getSelectedItem();
                if (sel != null) cuotaId = sel.getId();
                BigDecimal aplicado = prestamoService.registrarAbono(prestamoSeleccionado.getId(), monto,
                        cuotaId, cmbFuente.getValue(), null, txtNotas.getText());
                modal.close();
                mostrarAlertaDiferida(Alert.AlertType.INFORMATION,
                        "Abono registrado: $" + aplicado.setScale(2) + (aplicado.compareTo(monto) < 0
                                ? "\n(Solo se aplicó el saldo pendiente disponible)" : ""));
                refrescar();
                cargarDetalle(prestamoSeleccionado.getId());
            } catch (NumberFormatException ex) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto inválido.");
            } catch (Exception ex) {
                mostrarError("No se pudo registrar el abono", ex);
            }
        });

        VBox contenido = new VBox(12, cuotasBox, form, lblSaldo, botones);
        contenido.setPadding(new Insets(15));
        modal.setScene(new Scene(contenido, 720, 480));
        ThemeManager.aplicarTemaGuardado(modal.getScene());
        modalAbierto = modal;
        try {
            modal.showAndWait();
        } finally {
            modalAbierto = null;
        }
    }

    // ==================== Meta ====================

    @FXML
    private void editarMeta() {
        if (prestamoSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Seleccione un préstamo de la lista.");
            return;
        }
        ComboBox<String> cmbPer = new ComboBox<>(FXCollections.observableArrayList(META_PERIODICIDADES));
        cmbPer.setValue(prestamoSeleccionado.getMetaPeriodicidad() != null
                ? prestamoSeleccionado.getMetaPeriodicidad() : "NINGUNA");
        TextField txtMonto = new TextField(prestamoSeleccionado.getMetaMonto() != null
                ? prestamoSeleccionado.getMetaMonto().toPlainString() : "");
        txtMonto.setPromptText("0.00");
        Button btnSugerir = new Button("SUGERIR");
        btnSugerir.setTooltip(new Tooltip("Sugerir una meta de ventas según la cuota del préstamo"));
        cmbPer.valueProperty().addListener((o, a, b) -> {
            boolean off = b == null || "NINGUNA".equals(b);
            txtMonto.setDisable(off);
            btnSugerir.setDisable(off);
        });
        btnSugerir.setOnAction(e -> {
            BigDecimal sugerida = PrestamoService.calcularMetaAutomatica(prestamoSeleccionado.getCuotaMonto(),
                    prestamoSeleccionado.getPeriodicidad(), cmbPer.getValue());
            txtMonto.setText(sugerida.toPlainString());
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(crearLabel("Periodicidad de la meta:"), 0, 0);
        form.add(cmbPer, 1, 0);
        form.add(crearLabel("Monto de la meta ($):"), 0, 1);
        form.add(new HBox(5, txtMonto, btnSugerir), 1, 1);
        Label info = crearLabel("La meta se compara contra las ventas del período anterior " +
                "(ayer o últimos 7 días) al regenerar alertas.");
        info.setWrapText(true);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Meta de ventas — " + prestamoSeleccionado.getEntidadNombre());
        dlg.setHeaderText("Configure la meta diaria/semanal de ventas para cubrir este préstamo");
        dlg.getDialogPane().setContent(new VBox(10, form, info));
        dlg.getButtonTypes().setAll(guardar, cancelar);
        if (ventanaActiva() != null) {
            dlg.initOwner(ventanaActiva());
        }
        ThemeManager.aplicarTemaGuardado(dlg.getDialogPane().getScene());
        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == guardar) {
            try {
                String per = "NINGUNA".equals(cmbPer.getValue()) ? null : cmbPer.getValue();
                BigDecimal monto = per != null ? parse(txtMonto.getText()) : null;
                prestamoService.actualizarMeta(prestamoSeleccionado.getId(), per, monto);
                refrescar();
                cargarDetalle(prestamoSeleccionado.getId());
            } catch (Exception ex) {
                mostrarError("No se pudo guardar la meta", ex);
            }
        }
    }

    @FXML
    private void cancelarPrestamo() {
        if (prestamoSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Seleccione un préstamo de la lista.");
            return;
        }
        if (!"ACTIVO".equals(prestamoSeleccionado.getEstado())) {
            mostrarAlerta(Alert.AlertType.WARNING, "El préstamo no está activo.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar préstamo");
        confirm.setHeaderText("¿Cancelar el préstamo de " + prestamoSeleccionado.getEntidadNombre() + "?");
        confirm.setContentText("El préstamo dejará de aparecer como activo.\nEsta acción no borra el historial de pagos.");
        if (ventanaActiva() != null) {
            confirm.initOwner(ventanaActiva());
        }
        ThemeManager.aplicarTemaGuardado(confirm.getDialogPane().getScene());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    prestamoService.cancelarPrestamo(prestamoSeleccionado.getId(), "Cancelado por usuario");
                    prestamoSeleccionado = null;
                    refrescar();
                } catch (Exception ex) {
                    mostrarError("No se pudo cancelar el préstamo", ex);
                }
            }
        });
    }

    // ==================== Entidades financieras ====================

    @FXML
    private void gestionarEntidades() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Entidades Financieras");

        TableView<EntidadFinanciera> tbl = new TableView<>();
        tbl.setPrefHeight(260);
        TableColumn<EntidadFinanciera, String> cNombre = new TableColumn<>("Nombre");
        cNombre.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getNombre()));
        cNombre.setPrefWidth(180);
        TableColumn<EntidadFinanciera, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getTipo()));
        cTipo.setPrefWidth(90);
        TableColumn<EntidadFinanciera, String> cIdent = new TableColumn<>("RUC/C.I.");
        cIdent.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getIdentificacion()));
        cIdent.setPrefWidth(120);
        TableColumn<EntidadFinanciera, String> cTel = new TableColumn<>("Teléfono");
        cTel.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getTelefono()));
        cTel.setPrefWidth(100);
        TableColumn<EntidadFinanciera, String> cContacto = new TableColumn<>("Contacto");
        cContacto.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getContacto()));
        cContacto.setPrefWidth(130);
        TableColumn<EntidadFinanciera, Boolean> cActiva = new TableColumn<>("Activa");
        cActiva.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().isEstado()));
        cActiva.setPrefWidth(60);
        tbl.getColumns().addAll(cNombre, cTipo, cIdent, cTel, cContacto, cActiva);
        recargarEntidades(tbl);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del banco o entidad");
        ComboBox<String> cmbTipo = new ComboBox<>(FXCollections.observableArrayList("BANCO", "PRIVADO", "OTRO"));
        cmbTipo.setValue("BANCO");
        TextField txtIdent = new TextField();
        txtIdent.setPromptText("RUC o C.I.");
        TextField txtTel = new TextField();
        txtTel.setPromptText("Teléfono");
        TextField txtContacto = new TextField();
        txtContacto.setPromptText("Persona de contacto");

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.add(crearLabel("Nombre:"), 0, 0); form.add(txtNombre, 1, 0);
        form.add(crearLabel("Tipo:"), 2, 0); form.add(cmbTipo, 3, 0);
        form.add(crearLabel("Identificación:"), 0, 1); form.add(txtIdent, 1, 1);
        form.add(crearLabel("Teléfono:"), 2, 1); form.add(txtTel, 3, 1);
        form.add(crearLabel("Contacto:"), 0, 2); form.add(txtContacto, 1, 2, 3, 1);

        Button btnAgregar = new Button("AGREGAR");
        btnAgregar.setStyle("-fx-font-weight: bold");
        btnAgregar.setTooltip(new Tooltip("Guardar la nueva entidad financiera"));
        FontIcon icoAdd = new FontIcon("fas-plus");
        icoAdd.setIconColor(Color.WHITE);
        btnAgregar.setGraphic(icoAdd);
        btnAgregar.setOnAction(e -> {
            try {
                if (txtNombre.getText().trim().isEmpty()) {
                    mostrarAlerta(modal, Alert.AlertType.WARNING, "Ingrese el nombre de la entidad.");
                    return;
                }
                EntidadFinanciera nueva = new EntidadFinanciera(txtNombre.getText().trim(), cmbTipo.getValue(),
                        txtIdent.getText().trim(), txtTel.getText().trim(), txtContacto.getText().trim());
                nueva.setEstado(true);
                int id = prestamoService.guardarEntidad(nueva);
                if (id > 0) {
                    txtNombre.clear(); txtIdent.clear(); txtTel.clear(); txtContacto.clear();
                    recargarEntidades(tbl);
                } else {
                    mostrarAlerta(modal, Alert.AlertType.ERROR, "No se pudo guardar la entidad.");
                }
            } catch (Exception ex) {
                mostrarError("No se pudo guardar la entidad", ex);
            }
        });

        Button btnDesactivar = new Button("Desactivar seleccionada");
        btnDesactivar.setTooltip(new Tooltip("Desactivar la entidad financiera seleccionada"));
        btnDesactivar.setOnAction(e -> {
            EntidadFinanciera sel = tbl.getSelectionModel().getSelectedItem();
            if (sel == null) {
                mostrarAlerta(modal, Alert.AlertType.WARNING, "Seleccione una entidad.");
                return;
            }
            try {
                prestamoService.desactivarEntidad(sel.getId());
                recargarEntidades(tbl);
            } catch (Exception ex) {
                mostrarError("No se pudo desactivar", ex);
            }
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setTooltip(new Tooltip("Cerrar la gestión de entidades"));
        btnCerrar.setOnAction(e -> modal.close());
        HBox acciones = new HBox(8, btnAgregar, btnDesactivar, btnCerrar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(10, tbl, form, acciones);
        contenido.setPadding(new Insets(15));
        modal.setScene(new Scene(contenido, 760, 480));
        ThemeManager.aplicarTemaGuardado(modal.getScene());
        modalAbierto = modal;
        try {
            modal.showAndWait();
        } finally {
            modalAbierto = null;
        }
        refrescar();
    }

    private void recargarEntidades(TableView<EntidadFinanciera> tbl) {
        tbl.getItems().setAll(prestamoService.listarEntidades(false));
    }

    // ==================== helpers ====================

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        mostrarAlerta(null, tipo, mensaje);
    }

    private void mostrarAlerta(Window owner, Alert.AlertType tipo, String mensaje) {
        Alert alert = new Alert(tipo, mensaje);
        Window propietario = owner != null ? owner : ventanaActiva();
        if (propietario != null) {
            alert.initOwner(propietario);
        }
        ThemeManager.aplicarTemaGuardado(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private void mostrarAlertaDiferida(Alert.AlertType tipo, String mensaje) {
        Alert alert = new Alert(tipo, mensaje);
        Window propietario = ventanaPrincipal();
        if (propietario != null) {
            alert.initOwner(propietario);
        }
        ThemeManager.aplicarTemaGuardado(alert.getDialogPane().getScene());
        Platform.runLater(alert::showAndWait);
    }

    private Window ventanaActiva() {
        if (modalAbierto != null && modalAbierto.isShowing()) {
            return modalAbierto;
        }
        return ventanaPrincipal();
    }

    private Window ventanaPrincipal() {
        try {
            if (tblPrestamos != null && tblPrestamos.getScene() != null) {
                return tblPrestamos.getScene().getWindow();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal calcularCuotaPreview(TextField monto, TextField tasa, TextField cuotas) {
        try {
            BigDecimal m = parse(monto.getText());
            BigDecimal t = parse(tasa.getText());
            int n = (int) parse(cuotas.getText()).doubleValue();
            if (m == null || t == null || n <= 0) return null;
            return PrestamoService.calcularCuotaFrancesa(m, t, n);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parse(String s) {
        if (s == null || s.trim().isEmpty()) throw new NumberFormatException("vacío");
        return new BigDecimal(s.trim().replace(",", "."));
    }

    private Label crearLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 13px");
        return lbl;
    }

    private Label crearLabelBold(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold");
        return lbl;
    }

    private void mostrarError(String titulo, Exception ex) {
        ex.printStackTrace();
        mostrarAlerta(Alert.AlertType.ERROR, titulo + ":\n" + ex.getMessage());
    }
}
