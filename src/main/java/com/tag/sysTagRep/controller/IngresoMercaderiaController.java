package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CuentaPorPagarDAO;
import com.tag.sysTagRep.dao.CodigoDAO;
import com.tag.sysTagRep.dao.FacturaDetalleDAO;
import com.tag.sysTagRep.dao.FacturaProveedorDAO;
import com.tag.sysTagRep.dao.GrupoDAO;
import com.tag.sysTagRep.dao.HistorialProductoDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.MarcaDAO;
import com.tag.sysTagRep.dao.ProveedorDAO;
import com.tag.sysTagRep.dao.UbicacionDetalleDAO;
import com.tag.sysTagRep.model.Codigo;
import com.tag.sysTagRep.model.CuentaPorPagar;
import com.tag.sysTagRep.model.FacturaProveedor;
import com.tag.sysTagRep.model.Grupo;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.Marca;
import com.tag.sysTagRep.model.Proveedor;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Formulario de ingreso de factura de mercadería. Permite registrar varios
 * productos bajo los mismos datos de factura (Nº factura, proveedor, fecha,
 * forma de pago). Puede usarse como pantalla independiente o como modal de
 * edición desde Inventario (un solo producto).
 */
public class IngresoMercaderiaController implements Initializable {

    @FXML private TextField txtId;
    @FXML private ComboBox<String> cmbDescripcion;
    @FXML private ComboBox<Grupo> cmbGrupo;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private TextField txtCostoSinIVA;
    @FXML private TextField txtIVA;
    @FXML private TextField txtTotalIVA;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private TextField txtPrecioVenta;
    @FXML private DatePicker dpFechaIngreso;
    @FXML private ComboBox<Integer> cmbGanancia;
    @FXML private TextField txtDescuento;
    @FXML private TextField txtCodigo;
    @FXML private ComboBox<Codigo> cmbCodigo;
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private ComboBox<String> cmbFormaPago;
    @FXML private ComboBox<Integer> cmbMesesPlazo;
    @FXML private ComboBox<String> cmbInteres;
    @FXML private HBox pnlCredito;
    @FXML private TextField txtNumeroFactura;

    @FXML private Button btnAgregar;
    @FXML private Button btnGuardar;
    @FXML private TableView<FilaProducto> tblProductos;
    @FXML private TableColumn<FilaProducto, String> colCodigo;
    @FXML private TableColumn<FilaProducto, String> colDescripcion;
    @FXML private TableColumn<FilaProducto, Number> colCantidad;
    @FXML private TableColumn<FilaProducto, Number> colCosto;
    @FXML private TableColumn<FilaProducto, Number> colIva;
    @FXML private TableColumn<FilaProducto, Number> colTotalLinea;
    @FXML private TableColumn<FilaProducto, FilaProducto> colEliminar;
    @FXML private TableColumn<FilaProducto, FilaProducto> colEditar;
    @FXML private Label lblTotalFactura;
    @FXML private HBox hbTotalFactura;

    private boolean cerrarAlGuardar = false;
    private boolean modoEdicion = false;
    private boolean modoEdicionFactura = false;
    private String facturaEditadaNumero;
    private int facturaEditadaProveedorId;
    private boolean servicioLogistico = false;
    private final InventarioDAO dao = new InventarioDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final GrupoDAO grupoDAO = new GrupoDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final CuentaPorPagarDAO cuentaPorPagarDAO = new CuentaPorPagarDAO();
    private final FacturaProveedorDAO facturaProveedorDAO = new FacturaProveedorDAO();
    private final CodigoDAO codigoDAO = new CodigoDAO();
    private final FacturaDetalleDAO facturaDetalleDAO = new FacturaDetalleDAO();
    private final HistorialProductoDAO historialProductoDAO = new HistorialProductoDAO();
    private final UbicacionDetalleDAO ubicacionDAO = new UbicacionDetalleDAO();
    private final LogDAO logDAO = new LogDAO();

    private ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();
    private ObservableList<Grupo> listaGrupos = FXCollections.observableArrayList();
    private ObservableList<Marca> listaMarcas = FXCollections.observableArrayList();
    private ObservableList<Codigo> listaCodigos = FXCollections.observableArrayList();
    private final ObservableList<String> listaDescripciones = FXCollections.observableArrayList();
    private final ObservableList<FilaProducto> listaProductos = FXCollections.observableArrayList();
    private Timeline debounceDescripciones;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        iniciarCbGrupos();
        iniciarCbMarcas();
        iniciarCbProveedores();
        iniciarCbCodigos();
        iniciarCbDescripciones();
        iniciarSpCantidad();
        iniciarCbMargen();
        configurarDescuento();
        iniciarCbFormaPago();
        configurarCalculoPrecio();
        validarSoloNumeros();
        configurarCodigoAuto();
        configurarTablaProductos();
        limpiarFrm();
    }

    private void iniciarCbDescripciones() {
        cmbDescripcion.setEditable(true);
        cmbDescripcion.setItems(listaDescripciones);
        cmbDescripcion.setConverter(new StringConverter<>() {
            @Override public String toString(String s) { return s == null ? "" : s; }
            @Override public String fromString(String s) { return s; }
        });
        // Mantener uppercase como txtDescripcion original
        cmbDescripcion.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.equals(newText.toUpperCase())) {
                String caret = newText;
                int pos = cmbDescripcion.getEditor().getCaretPosition();
                cmbDescripcion.getEditor().setText(newText.toUpperCase());
                cmbDescripcion.getEditor().positionCaret(pos);
            }
        });
        // ILIKE dinámico: MIN 2 chars, LIMIT 50, debounce 300ms, '%texto%'
        cmbDescripcion.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (debounceDescripciones != null) debounceDescripciones.stop();
            String filtro = newVal == null ? "" : newVal.trim();
            if (filtro.length() < 2) {
                listaDescripciones.clear();
                Platform.runLater(() -> cmbDescripcion.hide());
                return;
            }
            debounceDescripciones = new Timeline(new KeyFrame(Duration.millis(300), ev -> {
                List<String> res = dao.buscarDescripciones(filtro, 50);
                Platform.runLater(() -> {
                    String editorTxt = cmbDescripcion.getEditor().getText();
                    if (editorTxt == null) editorTxt = "";
                    // Evitar loop si el usuario cambió texto mientras se consultaba
                    if (!editorTxt.trim().equalsIgnoreCase(filtro.trim())) return;
                    listaDescripciones.setAll(res);
                    // Restaurar texto y caret que se pierde al setAll
                    cmbDescripcion.getEditor().setText(editorTxt);
                    cmbDescripcion.getEditor().positionCaret(editorTxt.length());
                    if (!res.isEmpty() && cmbDescripcion.getEditor().isFocused()) {
                        cmbDescripcion.show();
                    } else {
                        cmbDescripcion.hide();
                    }
                });
            }));
            debounceDescripciones.play();
        });
    }

    private String descripcionIngresada() {
        String ed = cmbDescripcion.getEditor().getText();
        if (ed != null && !ed.trim().isEmpty()) return ed.trim().toUpperCase();
        String sel = cmbDescripcion.getValue();
        return sel == null ? "" : sel.trim().toUpperCase();
    }

    private void iniciarCbGrupos() {
        listaGrupos.setAll(grupoDAO.listar());
        ComboFilter.habilitar(cmbGrupo, listaGrupos, new StringConverter<>() {
            @Override public String toString(Grupo g) { return (g == null) ? "" : g.getNombre(); }
            @Override public Grupo fromString(String s) { return null; }
        });
    }

    private void iniciarCbMarcas() {
        listaMarcas.setAll(marcaDAO.listar());
        ComboFilter.habilitar(cmbMarca, listaMarcas, new StringConverter<>() {
            @Override public String toString(Marca m) { return (m == null) ? "" : m.getNombre(); }
            @Override public Marca fromString(String s) { return null; }
        });
    }

    private void iniciarCbProveedores() {
        listaProveedores.setAll(proveedorDAO.listar());
        ComboFilter.habilitar(cmbProveedor, listaProveedores, new StringConverter<>() {
            @Override public String toString(Proveedor p) { return (p == null) ? "" : p.getNombre(); }
            @Override public Proveedor fromString(String string) {
                return listaProveedores.stream()
                        .filter(p -> p.getNombre().equalsIgnoreCase(string))
                        .findFirst().orElse(null);
            }
        });
    }

    private void iniciarCbCodigos() {
        listaCodigos.setAll(codigoDAO.listar());
        ComboFilter.habilitar(cmbCodigo, listaCodigos, new StringConverter<>() {
            @Override public String toString(Codigo c) { return (c == null) ? "" : c.getNombre(); }
            @Override public Codigo fromString(String string) {
                return listaCodigos.stream()
                        .filter(c -> c.getNombre().equalsIgnoreCase(string))
                        .findFirst().orElse(null);
            }
        });
    }

    private String codigoSeleccionado() {
        Codigo c = cmbCodigo.getValue();
        if (c != null) return c.getNombre();
        String texto = cmbCodigo.getEditor().getText();
        return texto != null ? texto.trim() : "";
    }

    private void registrarCodigoNuevo(String codigo) {
        if (codigo == null || codigo.isEmpty()) return;
        if (listaCodigos.stream().noneMatch(c -> c.getNombre().equalsIgnoreCase(codigo)) && !codigoDAO.existe(codigo)) {
            Codigo nuevo = new Codigo();
            nuevo.setNombre(codigo);
            nuevo.setEstado(true);
            try {
                codigoDAO.guardar(nuevo);
            } catch (java.sql.SQLException e) {
                logDAO.guardar("IngresoMercaderiaController", "registrarCodigoNuevo", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al guardar código: " + e.getMessage()).showAndWait();
                return;
            }
            listaCodigos.setAll(codigoDAO.listar());
        }
    }

    private String generarCodigo() {
        String pv = txtPrecioVenta.getText().replace(",", ".");
        if (pv.isEmpty()) return "";
        try {
            BigDecimal precio = new BigDecimal(pv);
            return com.tag.sysTagRep.util.EtiquetaUtil.cifrarPrecio(precio.setScale(2, RoundingMode.HALF_UP).toString());
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private Proveedor proveedorSeleccionado() {
        Proveedor p = cmbProveedor.getValue();
        if (p == null && cmbProveedor.getEditor().getText() != null) {
            String texto = cmbProveedor.getEditor().getText();
            p = listaProveedores.stream()
                    .filter(prov -> prov.getNombre().equalsIgnoreCase(texto))
                    .findFirst().orElse(null);
        }
        return p;
    }

    @FXML
    private void servicioLogistico() {
        servicioLogistico = true;
        cmbDescripcion.setValue("SERVICIO LOGISTICO");
        cmbDescripcion.getEditor().setText("SERVICIO LOGISTICO");

        Grupo transporte = listaGrupos.stream()
                .filter(g -> "TRANSPORTE".equalsIgnoreCase(g.getNombre()))
                .findFirst().orElse(null);
        if (transporte == null) {
            Grupo nuevo = new Grupo();
            nuevo.setNombre("TRANSPORTE");
            nuevo.setEstado(true);
            try {
                grupoDAO.guardar(nuevo);
            } catch (java.sql.SQLException e) {
                logDAO.guardar("IngresoMercaderiaController", "servicioLogistico", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al guardar grupo TRANSPORTE: " + e.getMessage()).showAndWait();
                return;
            }
            listaGrupos.setAll(grupoDAO.listar());
            transporte = listaGrupos.stream()
                    .filter(g -> "TRANSPORTE".equalsIgnoreCase(g.getNombre()))
                    .findFirst().orElse(null);
        }
        cmbGrupo.setValue(transporte);

        registrarCodigoNuevo("000");
        Codigo codigo000 = listaCodigos.stream()
                .filter(c -> "000".equalsIgnoreCase(c.getNombre()))
                .findFirst().orElse(null);
        cmbCodigo.setValue(codigo000);
        cmbCodigo.getEditor().setText("000");

        cmbGanancia.setValue(0);
        txtCostoSinIVA.setText("2");
        txtPrecioVenta.setText("0.00");
    }

    @FXML
    private void agregarProducto() {
        try {
            String desc = descripcionIngresada();
            if (desc.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese la descripción del producto.").showAndWait();
                return;
            }
            if (txtCostoSinIVA.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese el costo sin IVA.").showAndWait();
                return;
            }
            BigDecimal costo = new BigDecimal(txtCostoSinIVA.getText().replace(",", "."));
            if (costo.signum() <= 0) {
                new Alert(Alert.AlertType.WARNING, "El costo sin IVA debe ser mayor a cero.").showAndWait();
                return;
            }
            int cant = spCantidad.getValue() != null ? spCantidad.getValue() : 1;
            if (cant <= 0) {
                new Alert(Alert.AlertType.WARNING, "La cantidad debe ser mayor a cero.").showAndWait();
                return;
            }
            BigDecimal iva = costo.multiply(new BigDecimal("0.15")).setScale(3, RoundingMode.HALF_UP);
            BigDecimal totalLinea = costo.add(iva).multiply(BigDecimal.valueOf(cant)).setScale(3, RoundingMode.HALF_UP);
            BigDecimal precioVenta = txtPrecioVenta.getText().isEmpty()
                    ? costo.add(iva)
                    : new BigDecimal(txtPrecioVenta.getText().replace(",", "."));

            Grupo g = cmbGrupo.getValue();
            Marca m = cmbMarca.getValue();
            String codigoProd = codigoSeleccionado();
            registrarCodigoNuevo(codigoProd);

            FilaProducto fp = new FilaProducto(
                    txtCodigo.getText() != null ? txtCodigo.getText() : "",
                    codigoProd,
                    desc,
                    g != null ? g.getId() : 0,
                    m != null ? m.getId() : 0,
                    costo, cant, precioVenta,
                    cmbGanancia.getValue() != null ? cmbGanancia.getValue() : 0
            );
            fp.setIva(iva.doubleValue());
            fp.setTotalLinea(totalLinea.doubleValue());
            fp.setEstado(true);
            listaProductos.add(fp);

            calcularTotalFactura();
            limpiarProducto();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Valores numéricos inválidos.").showAndWait();
        }
    }

    private void eliminarProducto(FilaProducto fp) {
        listaProductos.remove(fp);
        calcularTotalFactura();
    }

    private void cargarProductoEnFormulario(FilaProducto fp) {
        cmbDescripcion.setValue(fp.getDescripcion());
        cmbDescripcion.getEditor().setText(fp.getDescripcion());
        for (Grupo g : listaGrupos) {
            if (g.getId() == fp.getGrupoId()) { cmbGrupo.setValue(g); break; }
        }
        for (Marca m : listaMarcas) {
            if (m.getId() == fp.getMarcaId()) { cmbMarca.setValue(m); break; }
        }
        txtCostoSinIVA.setText(redondearMostrar(BigDecimal.valueOf(fp.getCostoSinIVA())).toPlainString());
        spCantidad.getValueFactory().setValue(fp.getCantidad());
        txtPrecioVenta.setText(redondearMostrar(BigDecimal.valueOf(fp.getPrecioVenta())).toPlainString());
        txtCodigo.setText(fp.getCodigo());
        if (fp.getCodigoManual() != null && !fp.getCodigoManual().isEmpty()) {
            for (Codigo c : listaCodigos) {
                if (c.getNombre().equalsIgnoreCase(fp.getCodigoManual())) {
                    cmbCodigo.setValue(c);
                    break;
                }
            }
            cmbCodigo.getEditor().setText(fp.getCodigoManual());
        } else {
            cmbCodigo.setValue(null);
            cmbCodigo.getEditor().clear();
        }
        cmbGanancia.setValue(fp.getMargen());
        eliminarProducto(fp);
    }

    private BigDecimal redondearMostrar(BigDecimal v) {
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP);
        BigDecimal intOriginal = v.setScale(0, RoundingMode.DOWN);
        if (r.setScale(0, RoundingMode.DOWN).compareTo(intOriginal) != 0) {
            r = v.setScale(2, RoundingMode.DOWN);
        }
        return r;
    }

    private void calcularTotalFactura() {
        BigDecimal total = BigDecimal.ZERO;
        for (FilaProducto fp : listaProductos) {
            total = total.add(BigDecimal.valueOf(fp.getTotalLinea()));
        }
        BigDecimal desc = BigDecimal.ZERO;
        try {
            String t = txtDescuento.getText().replace(",", ".");
            if (!t.isEmpty()) desc = new BigDecimal(t);
        } catch (NumberFormatException ignored) {}
        total = total.subtract(desc);
        lblTotalFactura.setText("$ " + redondearMostrar(total).toPlainString());
    }

    private void configurarTablaProductos() {
        tblProductos.setItems(listaProductos);
        tblProductos.setColumnResizePolicy(rf -> {
            double total = rf.getTable().getWidth();
            if (total <= 0) return false;
            double datos = total - colEditar.getWidth() - colEliminar.getWidth();
            colDescripcion.setPrefWidth(datos * 0.50);
            colCodigo.setPrefWidth(datos * 0.10);
            colCantidad.setPrefWidth(datos * 0.10);
            colCosto.setPrefWidth(datos * 0.10);
            colIva.setPrefWidth(datos * 0.10);
            colTotalLinea.setPrefWidth(datos * 0.10);
            return true;
        });

        colCodigo.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCodigoManual()));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));
        colTotalLinea.setCellValueFactory(new PropertyValueFactory<>("totalLinea"));

        colCosto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + redondearMostrar(BigDecimal.valueOf(n.doubleValue())).toPlainString());
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colIva.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + redondearMostrar(BigDecimal.valueOf(n.doubleValue())).toPlainString());
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colTotalLinea.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + redondearMostrar(BigDecimal.valueOf(n.doubleValue())).toPlainString());
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
            }
        });
        colCantidad.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : String.valueOf(n.intValue()));
                setStyle("-fx-alignment: CENTER;");
            }
        });
        colEliminar.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    FilaProducto fp = getTableView().getItems().get(getIndex());
                    if (fp != null) eliminarProducto(fp);
                });
            }
            @Override protected void updateItem(FilaProducto item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setStyle("-fx-alignment: CENTER;");
            }
        });
        colEditar.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button("✎");
            {
                btn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    FilaProducto fp = getTableView().getItems().get(getIndex());
                    if (fp != null) cargarProductoEnFormulario(fp);
                });
            }
            @Override protected void updateItem(FilaProducto item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    @FXML
    private void guardar() {
        try {
            if (modoEdicion) {
                guardarUnico();
                return;
            }
            if (listaProductos.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Agregue al menos un producto a la factura.").showAndWait();
                return;
            }
            Proveedor p = proveedorSeleccionado();
            String numFactura = txtNumeroFactura.getText() != null ? txtNumeroFactura.getText().trim() : "";

            if (!modoEdicionFactura && facturaProveedorDAO.existeNumeroFactura(numFactura, p != null ? p.getId() : 0)) {
                new Alert(Alert.AlertType.WARNING, "Ya existe una factura registrada con el N° " + numFactura
                        + (p != null ? " del proveedor " + p.getNombre() : "") + ".").showAndWait();
                return;
            }

            LocalDateTime fecha = dpFechaIngreso.getValue() != null
                    ? dpFechaIngreso.getValue().atStartOfDay() : LocalDateTime.now();
            String formaPago = cmbFormaPago.getValue();
            int meses = 0;
            BigDecimal interes = BigDecimal.ZERO;
            if ("TAG Crédito".equals(formaPago)) {
                meses = cmbMesesPlazo.getValue() != null ? cmbMesesPlazo.getValue() : 0;
                interes = cmbInteres.getValue() != null ? new BigDecimal(cmbInteres.getValue()) : BigDecimal.ZERO;
            }

            if (modoEdicionFactura) {
                reemplazarFactura(facturaEditadaNumero, facturaEditadaProveedorId);
            }

            List<FacturaProveedor> lineasFactura = new ArrayList<>();
            for (FilaProducto fp : listaProductos) {
                Inventario i = construirInventario(fp, p, numFactura, fecha, formaPago, meses, interes);
                int id = dao.guardar(i);
                if ("TAG Crédito".equals(formaPago) && p != null && id > 0) {
                    BigDecimal total = i.getCostoSinIVA().multiply(BigDecimal.valueOf(i.getCantidad()));
                    crearCreditoProveedor(id, p.getId(), total, meses, interes);
                }

                FacturaProveedor linea = new FacturaProveedor();
                linea.setNumeroFactura(numFactura);
                linea.setProveedorId(p != null ? p.getId() : 0);
                linea.setCodigo(fp.getCodigo());
                linea.setCodigoManual(fp.getCodigoManual());
                linea.setDescripcion(fp.getDescripcion());
                linea.setGrupoId(fp.getGrupoId());
                linea.setMarcaId(fp.getMarcaId());
                linea.setCostoSinIVA(BigDecimal.valueOf(fp.getCostoSinIVA()));
                linea.setIva(BigDecimal.valueOf(fp.getIva()));
                linea.setCantidad(fp.getCantidad());
                linea.setTotalLinea(BigDecimal.valueOf(fp.getTotalLinea()));
                linea.setFecha(fecha);
                lineasFactura.add(linea);
            }
            facturaProveedorDAO.insertar(lineasFactura);

            // Generar hoja A4 en background para no colgar la UI (como InventarioController)
            final String facturaNumFinal = numFactura;
            final Proveedor provFinal = p;
            final boolean esColocarPercheroFlow = !modoEdicion && !modoEdicionFactura && !cerrarAlGuardar;
            Runnable generarHojaAsync = () -> {
                Thread t = new Thread(() -> {
                    long t0 = System.currentTimeMillis();
                    logDAO.guardar("IngresoMercaderiaController", "generarHoja", "INICIO factura=" + facturaNumFinal + " prov=" + (provFinal != null ? provFinal.getId() : 0));
                    try {
                        List<Inventario> recien = dao.listarPorNumeroFactura(facturaNumFinal, provFinal != null ? provFinal.getId() : 0);
                        logDAO.guardar("IngresoMercaderiaController", "generarHoja", "listarPorNumeroFactura OK size=" + recien.size() + " en " + (System.currentTimeMillis()-t0) + "ms");
                        if (recien.isEmpty()) {
                            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "No se encontraron productos para hoja A4 (factura " + facturaNumFinal + ")").showAndWait());
                            return;
                        }
                        // Limitar total etiquetas para no colgar con cantidades enormes
                        long estEtiquetas = recien.stream().mapToLong(r -> r.getCantidad() > 0 ? r.getCantidad() : 1).sum();
                        if (estEtiquetas > 300) {
                            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "Factura con " + estEtiquetas + " etiquetas, se limitará a 300 para no colgar. Productos: " + recien.size()).showAndWait());
                        }
                        File hoja = com.tag.sysTagRep.util.HojaEtiquetasPDF.generarHojaA4(recien, facturaNumFinal);
                        long dt = System.currentTimeMillis()-t0;
                        logDAO.guardar("IngresoMercaderiaController", "generarHoja", "PDF OK " + hoja.getAbsolutePath() + " (" + hoja.length()/1024 + "KB) en " + dt + "ms");
                        // Auto-abrir PDF sin bloquear UI (background)
                        try {
                            String os2 = System.getProperty("os.name").toLowerCase();
                            if (os2.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", hoja.getAbsolutePath()).start();
                            else new ProcessBuilder("xdg-open", hoja.getAbsolutePath()).start();
                        } catch (Exception exOpen) {
                            logDAO.guardar("IngresoMercaderiaController", "abrirHoja", exOpen.getMessage(), exOpen);
                        }
                        javafx.application.Platform.runLater(() -> {
                            long totalEtiquetas = Math.min(estEtiquetas, 300);
                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setTitle("Hoja A4 generada");
                            a.setHeaderText("Hoja A4 generada en " + dt + "ms");
                            a.setContentText("Archivo: " + hoja.getAbsolutePath() + "\nProductos: " + recien.size() + " | Etiquetas: " + totalEtiquetas + " (incluye SERVICIO LOGISTICO)");
                            a.showAndWait();
                        });
                    } catch (Throwable ex) {
                        logDAO.guardar("IngresoMercaderiaController", "generarHoja", "ERROR " + ex.getMessage(), new Exception(ex));
                        ex.printStackTrace();
                        javafx.application.Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR, "Error al generar hoja A4: " + ex.getMessage()).showAndWait()
                        );
                    }
                });
                t.setDaemon(true);
                t.setName("hoja-a4-" + facturaNumFinal);
                t.start();
            };

            if (esColocarPercheroFlow) {
                int porUbicar = listaProductos.size();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Ingreso guardado");
                alert.setHeaderText("Guardado correctamente.");
                alert.setContentText("Tienes " + porUbicar + " producto(s) por colocar en el perchero. ¿Deseas ubicarlos ahora?");
                ButtonType btnPerchero = new ButtonType("Colocar en Perchero");
                ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnPerchero, btnAhoraNo);
                Optional<ButtonType> resultado = alert.showAndWait();
                if (resultado.isPresent() && resultado.get() == btnPerchero) {
                    abrirModal("/view/UbicacionPercheroView.fxml", "Ubicación Percha", 1000, 650);
                }
                // Generar hoja después (con o sin ubicar) sin bloquear UI
                limpiarFrm();
                generarHojaAsync.run();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
                if (cerrarAlGuardar) {
                    cerrarVentana();
                } else {
                    limpiarFrm();
                }
                generarHojaAsync.run();
            }
        } catch (Exception e) {
            logDAO.guardar("IngresoMercaderiaController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Reemplaza una factura de proveedor existente por la versión editada:
     * borra cuentas por pagar, inventario y líneas de factura asociadas, para que
     * el flujo de guardado posterior las vuelva a crear con los datos nuevos.
     */
    private void reemplazarFactura(String numeroFactura, int proveedorId) throws Exception {
        if (numeroFactura == null || numeroFactura.isEmpty()) return;
        List<Inventario> inventarios = dao.listarPorNumeroFactura(numeroFactura, proveedorId);
        List<Integer> ids = new ArrayList<>();
        for (Inventario inv : inventarios) ids.add(inv.getId());

        if (!ids.isEmpty() && (facturaDetalleDAO.existeVentaPorInventarioIds(ids)
                || historialProductoDAO.existeVentaPorInventarioIds(ids))) {
            throw new IllegalStateException("No se puede modificar la factura: uno de sus productos ya fue vendido.");
        }

        dao.eliminarFacturaConDependencias(numeroFactura, proveedorId);
    }

    private void guardarUnico() {
        String formaPago = cmbFormaPago.getValue();
        int meses = 0;
        BigDecimal interes = BigDecimal.ZERO;
        if ("TAG Crédito".equals(formaPago)) {
            meses = cmbMesesPlazo.getValue() != null ? cmbMesesPlazo.getValue() : 0;
            interes = cmbInteres.getValue() != null ? new BigDecimal(cmbInteres.getValue()) : BigDecimal.ZERO;
        }
        Inventario i = construirInventario(null, proveedorSeleccionado(),
                txtNumeroFactura.getText() != null ? txtNumeroFactura.getText().trim() : "",
                dpFechaIngreso.getValue() != null ? dpFechaIngreso.getValue().atStartOfDay() : LocalDateTime.now(),
                formaPago, meses, interes);
        i.setId(Integer.parseInt(txtId.getText()));
        dao.actualizar(i);
        new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        if (cerrarAlGuardar) cerrarVentana();
    }

    private boolean esServicioLogistico(FilaProducto fp) {
        if (fp == null) return false;
        return "SERVICIO LOGISTICO".equalsIgnoreCase(fp.getDescripcion())
                || "000".equals(fp.getCodigoManual());
    }

    private Inventario construirInventario(FilaProducto fp, Proveedor p, String numFactura,
                                           LocalDateTime fecha, String formaPago, int meses, BigDecimal interes) {
        Inventario i = new Inventario();
        if (fp != null) {
            i.setDescripcion(fp.getDescripcion());
            i.setGrupoId(fp.getGrupoId());
            i.setMarcaId(fp.getMarcaId());
            i.setCostoSinIVA(BigDecimal.valueOf(fp.getCostoSinIVA()));
            i.setCantidad(fp.getCantidad());
            i.setPrecioVenta(BigDecimal.valueOf(fp.getPrecioVenta()));
            i.setTagCodigo(fp.getCodigo());
            i.setCodigo(fp.getCodigoManual());
            i.setEstado(!esServicioLogistico(fp));
        } else {
            i.setDescripcion(descripcionIngresada());
            Grupo g = cmbGrupo.getValue();
            i.setGrupoId(g != null ? g.getId() : 0);
            Marca m = cmbMarca.getValue();
            i.setMarcaId(m != null ? m.getId() : 0);
            i.setCostoSinIVA(new BigDecimal(txtCostoSinIVA.getText().replace(",", ".")));
            i.setCantidad(spCantidad.getValue());
            i.setPrecioVenta(new BigDecimal(txtPrecioVenta.getText().replace(",", ".")));
            i.setTagCodigo(txtCodigo.getText());
            i.setCodigo(codigoSeleccionado());
        }
        i.setFecha_ingreso(fecha);
        i.setNumeroFactura(numFactura);
        i.setProveedorId(p != null ? p.getId() : 0);
        i.setFormaPago(formaPago);
        if ("TAG Crédito".equals(formaPago)) {
            i.setMesesPlazo(meses);
            i.setInteres(interes);
        } else {
            i.setMesesPlazo(0);
            i.setInteres(BigDecimal.ZERO);
        }
        return i;
    }

    private void crearCreditoProveedor(int inventarioId, int proveedorId, BigDecimal total, int mesesPlazo, BigDecimal interesPct) {
        if (mesesPlazo <= 0) mesesPlazo = 1;
        BigDecimal tasa = interesPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalConInteres = total.multiply(BigDecimal.ONE.add(tasa));
        BigDecimal cuotaMensual = totalConInteres.divide(BigDecimal.valueOf(mesesPlazo), 2, RoundingMode.HALF_UP);

        CuentaPorPagar cpp = new CuentaPorPagar(inventarioId, proveedorId, totalConInteres, mesesPlazo, interesPct, cuotaMensual);
        cuentaPorPagarDAO.insertar(cpp);
    }

    /**
     * Precarga el formulario con una factura de proveedor completa (modo edición de
     * factura multi-línea). Se reutiliza para editar desde "Facturas Ingresadas".
     */
    public void cargarFacturaParaEdicion(String numeroFactura, int proveedorId) {
        modoEdicion = false;
        modoEdicionFactura = true;
        facturaEditadaNumero = numeroFactura;
        facturaEditadaProveedorId = proveedorId;

        List<FacturaProveedor> lineas = facturaProveedorDAO.listarPorFactura(numeroFactura);
        if (lineas.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No se encontraron líneas para la factura seleccionada.").showAndWait();
            return;
        }

        List<Inventario> inventarios = dao.listarPorNumeroFactura(numeroFactura, proveedorId);

        txtNumeroFactura.setText(numeroFactura);
        if (lineas.get(0).getFecha() != null) {
            dpFechaIngreso.setValue(lineas.get(0).getFecha().toLocalDate());
        }

        for (Proveedor p : listaProveedores) {
            if (p.getId() == proveedorId) { cmbProveedor.setValue(p); break; }
        }

        String formaPago = "Efectivo";
        int meses = 0;
        BigDecimal interes = BigDecimal.ZERO;
        if (!inventarios.isEmpty() && inventarios.get(0).getFormaPago() != null) {
            formaPago = inventarios.get(0).getFormaPago();
            meses = inventarios.get(0).getMesesPlazo();
            interes = inventarios.get(0).getInteres() != null ? inventarios.get(0).getInteres() : BigDecimal.ZERO;
        }
        cmbFormaPago.setValue(formaPago);
        if ("TAG Crédito".equals(formaPago)) {
            cmbMesesPlazo.setValue(meses > 0 ? meses : 1);
            cmbInteres.setValue(interes.toString());
            pnlCredito.setVisible(true);
            pnlCredito.setManaged(true);
        } else {
            cmbMesesPlazo.getSelectionModel().selectFirst();
            cmbInteres.getSelectionModel().selectFirst();
            pnlCredito.setVisible(false);
            pnlCredito.setManaged(false);
        }

        for (int i = 0; i < lineas.size(); i++) {
            FacturaProveedor l = lineas.get(i);
            Inventario inv = inventarios.get(i);
            BigDecimal costo = l.getCostoSinIVA() != null ? l.getCostoSinIVA() : BigDecimal.ZERO;
            BigDecimal iva = l.getIva() != null ? l.getIva() : BigDecimal.ZERO;
            BigDecimal precioVenta = inv.getPrecioVenta() != null ? inv.getPrecioVenta() : costo.add(iva);
            FilaProducto fp = new FilaProducto(
                    l.getCodigo() != null ? l.getCodigo() : "",
                    l.getCodigoManual() != null ? l.getCodigoManual() : "",
                    l.getDescripcion() != null ? l.getDescripcion() : "",
                    l.getGrupoId(), l.getMarcaId(),
                    costo, l.getCantidad(),
                    precioVenta, 40
            );
            fp.setIva(iva.doubleValue());
            fp.setTotalLinea(l.getTotalLinea() != null ? l.getTotalLinea().doubleValue() : 0);
            fp.setEstado(!esServicioLogistico(fp));
            listaProductos.add(fp);
        }

        tblProductos.setVisible(true);
        tblProductos.setManaged(true);
        btnAgregar.setVisible(true);
        btnAgregar.setManaged(true);
        hbTotalFactura.setVisible(true);
        hbTotalFactura.setManaged(true);

        calcularTotalFactura();
    }

    /**
     * Precarga el formulario con un artículo existente (modo edición).
     */
    public void cargarParaEdicion(Inventario i) {
        modoEdicion = true;
        txtId.setText(String.valueOf(i.getId()));
        cmbDescripcion.setValue(i.getDescripcion());
        cmbDescripcion.getEditor().setText(i.getDescripcion() == null ? "" : i.getDescripcion());

        for (Grupo g : listaGrupos) {
            if (g.getId() == i.getGrupoId()) { cmbGrupo.setValue(g); break; }
        }
        for (Marca m : listaMarcas) {
            if (m.getId() == i.getMarcaId()) { cmbMarca.setValue(m); break; }
        }

        txtCostoSinIVA.setText(i.getCostoSinIVA().toString());
        spCantidad.getValueFactory().setValue(i.getCantidad());

        txtPrecioVenta.setText(i.getPrecioVenta().toString());
        txtCodigo.setText(i.getTagCodigo());
        if (i.getCodigo() != null && !i.getCodigo().isEmpty()) {
            for (Codigo c : listaCodigos) {
                if (c.getNombre().equalsIgnoreCase(i.getCodigo())) {
                    cmbCodigo.setValue(c);
                    break;
                }
            }
            cmbCodigo.getEditor().setText(i.getCodigo());
        }
        txtNumeroFactura.setText(i.getNumeroFactura());
        dpFechaIngreso.setValue(i.getFecha_ingreso() != null ? i.getFecha_ingreso().toLocalDate() : null);

        if (i.getProveedorId() > 0) {
            for (Proveedor p : listaProveedores) {
                if (p.getId() == i.getProveedorId()) {
                    cmbProveedor.setValue(p);
                    break;
                }
            }
        } else {
            cmbProveedor.setValue(null);
        }

        cmbFormaPago.setValue(i.getFormaPago() != null ? i.getFormaPago() : "Efectivo");
        if ("TAG Crédito".equals(i.getFormaPago())) {
            cmbMesesPlazo.setValue(i.getMesesPlazo() > 0 ? i.getMesesPlazo() : 1);
            cmbInteres.setValue(i.getInteres() != null ? i.getInteres().toString() : "0");
        } else {
            cmbMesesPlazo.getSelectionModel().selectFirst();
            cmbInteres.getSelectionModel().selectFirst();
        }

        tblProductos.setVisible(false);
        tblProductos.setManaged(false);
        btnAgregar.setVisible(false);
        btnAgregar.setManaged(false);
        hbTotalFactura.setVisible(false);
        hbTotalFactura.setManaged(false);
    }

    public void setCerrarAlGuardar(boolean cerrar) {
        this.cerrarAlGuardar = cerrar;
    }

    private void cerrarVentana() {
        Stage stage = (Stage) cmbDescripcion.getScene().getWindow();
        stage.close();
    }

    private void limpiarProducto() {
        servicioLogistico = false;
        cmbDescripcion.getEditor().clear();
        cmbDescripcion.setValue(null);
        listaDescripciones.clear();
        cmbGrupo.setValue(null);
        cmbMarca.setValue(null);
        txtCostoSinIVA.clear();
        txtIVA.clear();
        txtTotalIVA.clear();
        spCantidad.getValueFactory().setValue(1);
        txtPrecioVenta.clear();
        txtCodigo.clear();
        cmbCodigo.setValue(null);
        cmbCodigo.getEditor().clear();
        cmbGanancia.setValue(40);
        txtDescuento.setText("0.00");
    }

    public void limpiarFrm(){
        modoEdicion = false;
        modoEdicionFactura = false;
        txtId.clear();
        limpiarProducto();
        txtNumeroFactura.setText("001-001-00000123");
        dpFechaIngreso.setValue(LocalDate.now());
        cmbProveedor.setValue(null);
        cmbProveedor.getEditor().clear();
        cmbFormaPago.getSelectionModel().selectFirst();
        cmbMesesPlazo.getSelectionModel().selectFirst();
        cmbInteres.getSelectionModel().selectFirst();
 // NO limpiar listaProductos - los productos ya fueron guardados en BD
// solo limpiar campos de entrada para nuevo producto
        cmbDescripcion.getEditor().clear();
        cmbDescripcion.setValue(null);
        listaDescripciones.clear();
        txtCostoSinIVA.clear();
        txtPrecioVenta.clear();
        spCantidad.getValueFactory().setValue(1);
        cmbGanancia.setValue(40);
        calcularTotalFactura();
        tblProductos.setItems(listaProductos); // refrescar tabla con lo que hay
        tblProductos.setVisible(true);
        tblProductos.setManaged(true);
        btnAgregar.setVisible(true);
        btnAgregar.setManaged(true);
        hbTotalFactura.setVisible(true);
        hbTotalFactura.setManaged(true);
    }

    private void iniciarCbMargen(){
        ObservableList<Integer> items = FXCollections.observableArrayList();
        for (int i = 10; i <= 200; i += 10) items.add(i);
        ComboFilter.habilitarEnteros(cmbGanancia, items);
        cmbGanancia.setValue(40);
    }

    private void configurarDescuento() {
        txtDescuento.setText("0.00");
        txtDescuento.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !val.matches("\\d*(\\.\\d{0,3})?")) txtDescuento.setText(old);
        });
        txtDescuento.textProperty().addListener((obs, old, newVal) -> calcularTotalFactura());
    }

    private void configurarCalculoPrecio() {
        txtCostoSinIVA.textProperty().addListener((obs, old, newVal) -> {
            calcularPrecioVenta();
            calcularIva();
        });
        cmbGanancia.valueProperty().addListener((obs, old, newVal) -> calcularPrecioVenta());
        spCantidad.valueProperty().addListener((obs, old, newVal) -> calcularIva());
    }

    private void calcularIva() {
        if (txtCostoSinIVA.getText().isEmpty()) {
            txtIVA.clear();
            txtTotalIVA.clear();
            return;
        }
        try {
            BigDecimal costo = new BigDecimal(txtCostoSinIVA.getText().replace(",", "."));
            BigDecimal iva = costo.multiply(new BigDecimal("0.15")).setScale(3, RoundingMode.HALF_UP);
            int cant = spCantidad.getValue() != null ? spCantidad.getValue() : 1;
            txtIVA.setText(redondearMostrar(iva).toPlainString());
            txtTotalIVA.setText(redondearMostrar(costo.add(iva).multiply(BigDecimal.valueOf(cant))).toPlainString());
        } catch (Exception ignored) {}
    }

    private void calcularPrecioVenta() {
        if (txtCostoSinIVA.getText().isEmpty()) return;
        try {
            BigDecimal costo = new BigDecimal(txtCostoSinIVA.getText().replace(",", "."));
            BigDecimal iva = costo.multiply(new BigDecimal("0.15")).setScale(3, RoundingMode.HALF_UP);
            int margen = cmbGanancia.getValue() != null ? cmbGanancia.getValue() : 0;
            BigDecimal totalConIVA = costo.add(iva);
            BigDecimal precio = totalConIVA.add(totalConIVA.multiply(BigDecimal.valueOf(margen))
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            txtPrecioVenta.setText(redondearMostrar(precio).toPlainString());
        } catch (Exception ignored) {}
    }

    private void validarSoloNumeros(){
        txtCostoSinIVA.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !val.matches("\\d*(\\.\\d{0,3})?")) txtCostoSinIVA.setText(old);
        });
    }

    private void configurarCodigoAuto() {
        txtCodigo.setEditable(false);
        Runnable actualizarCodigo = () -> {
            if (txtPrecioVenta.getText() != null && !txtPrecioVenta.getText().isEmpty()) {
                txtCodigo.setText(generarCodigo());
            } else {
                txtCodigo.clear();
            }
        };
        cmbDescripcion.getEditor().textProperty().addListener((obs, o, n) -> actualizarCodigo.run());
        cmbDescripcion.valueProperty().addListener((obs, o, n) -> actualizarCodigo.run());
        txtCostoSinIVA.textProperty().addListener((obs, o, n) -> actualizarCodigo.run());
        txtPrecioVenta.textProperty().addListener((obs, o, n) -> actualizarCodigo.run());
        cmbGrupo.setOnAction(e -> actualizarCodigo.run());
        cmbMarca.setOnAction(e -> actualizarCodigo.run());
    }

    private void iniciarSpCantidad(){
        spCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999, 1));
        spCantidad.setEditable(true);
    }

    private void iniciarCbFormaPago() {
        ComboFilter.habilitar(cmbFormaPago, FXCollections.observableArrayList(
            "Efectivo", "Tarjeta de Crédito", "Tarjeta de Débito",
            "Transferencia", "Depósito", "Cheque", "TAG Crédito"
        ));
        cmbFormaPago.getSelectionModel().selectFirst();

        ComboFilter.habilitarEnteros(cmbMesesPlazo, FXCollections.observableArrayList(1, 2, 3, 4, 5, 6));
        cmbMesesPlazo.getSelectionModel().selectFirst();

        ComboFilter.habilitar(cmbInteres, FXCollections.observableArrayList("0", "3", "6", "9", "12", "15"));
        cmbInteres.getSelectionModel().selectFirst();

        cmbFormaPago.valueProperty().addListener((obs, old, valor) -> {
            boolean esCredito = "TAG Crédito".equals(valor);
            pnlCredito.setVisible(esCredito);
            pnlCredito.setManaged(esCredito);
        });
    }

    @FXML
    private void irAGrupo() {
        abrirModal("/view/GrupoView.fxml", "Gestión de Grupos", 700, 500);
    }

    @FXML
    private void irAMarca() {
        abrirModal("/view/MarcaView.fxml", "Gestión de Marcas", 700, 500);
    }

    @FXML
    private void irAProveedor() {
        abrirModal("/view/ProveedorView.fxml", "Gestión de Proveedores", 800, 600);
    }

    @FXML
    private void irACodigo() {
        abrirModal("/view/CodigoView.fxml", "Gestión de Códigos", 700, 500);
    }

    private void abrirModal(String fxml, String titulo, double ancho, double alto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(titulo);
            modal.setScene(new Scene(vista, ancho, alto));
            modal.showAndWait();
            recargarCatalogos();
        } catch (Exception e) {
            logDAO.guardar("IngresoMercaderiaController", "abrirModal", e.getMessage(), e);
        }
    }

    private void recargarCatalogos() {
        listaGrupos.setAll(grupoDAO.listar());
        listaMarcas.setAll(marcaDAO.listar());
        listaProveedores.setAll(proveedorDAO.listar());
        listaCodigos.setAll(codigoDAO.listar());
    }

    public static class FilaProducto {
        private final StringProperty codigo;
        private final StringProperty codigoManual;
        private final StringProperty descripcion;
        private final IntegerProperty grupoId;
        private final IntegerProperty marcaId;
        private final DoubleProperty costoSinIVA;
        private final IntegerProperty cantidad;
        private final DoubleProperty iva = new SimpleDoubleProperty(0);
        private final DoubleProperty totalLinea = new SimpleDoubleProperty(0);
        private final DoubleProperty precioVenta;
        private final IntegerProperty margen;
        private boolean estado = true;

        public FilaProducto(String codigo, String codigoManual, String descripcion, int grupoId, int marcaId,
                            BigDecimal costoSinIVA, int cantidad, BigDecimal precioVenta, int margen) {
            this.codigo = new SimpleStringProperty(codigo);
            this.codigoManual = new SimpleStringProperty(codigoManual);
            this.descripcion = new SimpleStringProperty(descripcion);
            this.grupoId = new SimpleIntegerProperty(grupoId);
            this.marcaId = new SimpleIntegerProperty(marcaId);
            this.costoSinIVA = new SimpleDoubleProperty(costoSinIVA.doubleValue());
            this.cantidad = new SimpleIntegerProperty(cantidad);
            this.precioVenta = new SimpleDoubleProperty(precioVenta.doubleValue());
            this.margen = new SimpleIntegerProperty(margen);
        }

        public String getCodigo() { return codigo.get(); }
        public StringProperty codigoProperty() { return codigo; }
        public String getCodigoManual() { return codigoManual.get(); }
        public String getDescripcion() { return descripcion.get(); }
        public StringProperty descripcionProperty() { return descripcion; }
        public int getGrupoId() { return grupoId.get(); }
        public int getMarcaId() { return marcaId.get(); }
        public double getCostoSinIVA() { return costoSinIVA.get(); }
        public DoubleProperty costoSinIVAAProperty() { return costoSinIVA; }
        public int getCantidad() { return cantidad.get(); }
        public IntegerProperty cantidadProperty() { return cantidad; }
        public double getIva() { return iva.get(); }
        public DoubleProperty ivaProperty() { return iva; }
        public void setIva(double v) { iva.set(v); }
        public double getTotalLinea() { return totalLinea.get(); }
        public DoubleProperty totalLineaProperty() { return totalLinea; }
        public void setTotalLinea(double v) { totalLinea.set(v); }
        public double getPrecioVenta() { return precioVenta.get(); }
        public DoubleProperty precioVentaProperty() { return precioVenta; }
        public int getMargen() { return margen.get(); }
        public IntegerProperty margenProperty() { return margen; }
        public boolean getEstado() { return estado; }
        public void setEstado(boolean estado) { this.estado = estado; }
    }
}
