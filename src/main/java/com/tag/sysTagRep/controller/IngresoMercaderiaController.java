package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CuentaPorPagarDAO;
import com.tag.sysTagRep.dao.CodigoDAO;
import com.tag.sysTagRep.dao.FacturaProveedorDAO;
import com.tag.sysTagRep.dao.GrupoDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.MarcaDAO;
import com.tag.sysTagRep.dao.ProveedorDAO;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Formulario de ingreso de factura de mercadería. Permite registrar varios
 * productos bajo los mismos datos de factura (Nº factura, proveedor, fecha,
 * forma de pago). Puede usarse como pantalla independiente o como modal de
 * edición desde Inventario (un solo producto).
 */
public class IngresoMercaderiaController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<Grupo> cmbGrupo;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private TextField txtCostoSinIVA;
    @FXML private TextField txtIVA;
    @FXML private TextField txtTotalIVA;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private TextField txtPrecioVenta;
    @FXML private DatePicker dpFechaIngreso;
    @FXML private ComboBox<Integer> cmbGanancia;
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
    @FXML private TableColumn<FilaProducto, Number> colPrecioVenta;
    @FXML private TableColumn<FilaProducto, Number> colMargen;
    @FXML private TableColumn<FilaProducto, FilaProducto> colEliminar;
    @FXML private Label lblTotalFactura;
    @FXML private HBox hbTotalFactura;

    private boolean cerrarAlGuardar = false;
    private boolean modoEdicion = false;

    private final InventarioDAO dao = new InventarioDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final GrupoDAO grupoDAO = new GrupoDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final CuentaPorPagarDAO cuentaPorPagarDAO = new CuentaPorPagarDAO();
    private final FacturaProveedorDAO facturaProveedorDAO = new FacturaProveedorDAO();
    private final CodigoDAO codigoDAO = new CodigoDAO();
    private final LogDAO logDAO = new LogDAO();

    private ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();
    private ObservableList<Grupo> listaGrupos = FXCollections.observableArrayList();
    private ObservableList<Marca> listaMarcas = FXCollections.observableArrayList();
    private ObservableList<Codigo> listaCodigos = FXCollections.observableArrayList();
    private final ObservableList<FilaProducto> listaProductos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        iniciarCbGrupos();
        iniciarCbMarcas();
        iniciarCbProveedores();
        iniciarCbCodigos();
        iniciarSpCantidad();
        iniciarCbMargen();
        iniciarCbFormaPago();
        configurarCalculoPrecio();
        validarSoloNumeros();
        configurarCodigoAuto();
        configurarTablaProductos();
        limpiarFrm();
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
            codigoDAO.guardar(nuevo);
            listaCodigos.setAll(codigoDAO.listar());
        }
    }

    private String generarCodigo(String desc, Grupo grupo, Marca marca, BigDecimal costoSinIVA) {
        String d = (desc != null && desc.length() >= 3)
                ? desc.substring(0, 3).toUpperCase()
                : (desc != null ? desc.toUpperCase() : "XXX");
        String g = (grupo != null && grupo.getNombre() != null && grupo.getNombre().length() >= 3)
                ? grupo.getNombre().substring(0, 3).toUpperCase() : (grupo != null ? grupo.getNombre().toUpperCase() : "SIN");
        String m = (marca != null && marca.getNombre() != null && marca.getNombre().length() >= 3)
                ? marca.getNombre().substring(0, 3).toUpperCase() : (marca != null ? marca.getNombre().toUpperCase() : "SIN");
        int costo = (costoSinIVA != null) ? costoSinIVA.intValue() : 0;
        return d + g + m + costo;
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
    private void agregarProducto() {
        try {
            String desc = txtDescripcion.getText() == null ? "" : txtDescripcion.getText().trim();
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
            BigDecimal iva = costo.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalLinea = costo.add(iva).multiply(BigDecimal.valueOf(cant)).setScale(2, RoundingMode.HALF_UP);
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

    private void calcularTotalFactura() {
        BigDecimal total = BigDecimal.ZERO;
        for (FilaProducto fp : listaProductos) {
            total = total.add(BigDecimal.valueOf(fp.getTotalLinea()));
        }
        lblTotalFactura.setText("$ " + total.setScale(2, RoundingMode.HALF_UP).toString());
    }

    private void configurarTablaProductos() {
        tblProductos.setItems(listaProductos);

        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));
        colTotalLinea.setCellValueFactory(new PropertyValueFactory<>("totalLinea"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colMargen.setCellValueFactory(new PropertyValueFactory<>("margen"));

        colCosto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + String.format("%.2f", n.doubleValue()));
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colIva.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + String.format("%.2f", n.doubleValue()));
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colTotalLinea.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + String.format("%.2f", n.doubleValue()));
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
            }
        });
        colPrecioVenta.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : "$ " + String.format("%.2f", n.doubleValue()));
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colCantidad.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : String.valueOf(n.intValue()));
                setStyle("-fx-alignment: CENTER;");
            }
        });
        colMargen.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : n.intValue() + " %");
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
            LocalDateTime fecha = dpFechaIngreso.getValue() != null
                    ? dpFechaIngreso.getValue().atStartOfDay() : LocalDateTime.now();
            String formaPago = cmbFormaPago.getValue();
            int meses = 0;
            BigDecimal interes = BigDecimal.ZERO;
            if ("TAG Crédito".equals(formaPago)) {
                meses = cmbMesesPlazo.getValue() != null ? cmbMesesPlazo.getValue() : 0;
                interes = cmbInteres.getValue() != null ? new BigDecimal(cmbInteres.getValue()) : BigDecimal.ZERO;
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
                lineasFactura.add(linea);
            }
            facturaProveedorDAO.insertar(lineasFactura);

            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
            if (cerrarAlGuardar) {
                cerrarVentana();
            } else {
                limpiarFrm();
            }
        } catch (Exception e) {
            logDAO.guardar("IngresoMercaderiaController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
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
        } else {
            i.setDescripcion(txtDescripcion.getText());
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
     * Precarga el formulario con un artículo existente (modo edición).
     */
    public void cargarParaEdicion(Inventario i) {
        modoEdicion = true;
        txtId.setText(String.valueOf(i.getId()));
        txtDescripcion.setText(i.getDescripcion());

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
        Stage stage = (Stage) txtDescripcion.getScene().getWindow();
        stage.close();
    }

    private void limpiarProducto() {
        txtDescripcion.clear();
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
    }

    public void limpiarFrm(){
        modoEdicion = false;
        txtId.clear();
        limpiarProducto();
        txtNumeroFactura.clear();
        dpFechaIngreso.setValue(LocalDate.now());
        cmbProveedor.setValue(null);
        cmbProveedor.getEditor().clear();
        cmbFormaPago.getSelectionModel().selectFirst();
        cmbMesesPlazo.getSelectionModel().selectFirst();
        cmbInteres.getSelectionModel().selectFirst();
        listaProductos.clear();
        lblTotalFactura.setText("$ 0.00");

        tblProductos.setVisible(true);
        tblProductos.setManaged(true);
        btnAgregar.setVisible(true);
        btnAgregar.setManaged(true);
        hbTotalFactura.setVisible(true);
        hbTotalFactura.setManaged(true);
    }

    private void iniciarCbMargen(){
        ObservableList<Integer> items = FXCollections.observableArrayList();
        for (int i = 10; i <= 100; i += 10) items.add(i);
        ComboFilter.habilitarEnteros(cmbGanancia, items);
        cmbGanancia.setValue(40);
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
            double costo = Double.parseDouble(txtCostoSinIVA.getText().replace(",", "."));
            double iva = costo * 0.15;
            int cant = spCantidad.getValue() != null ? spCantidad.getValue() : 1;
            txtIVA.setText(String.format("%.2f", iva).replace(",", "."));
            txtTotalIVA.setText(String.format("%.2f", (costo + iva) * cant).replace(",", "."));
        } catch (Exception ignored) {}
    }

    private void calcularPrecioVenta() {
        if (txtCostoSinIVA.getText().isEmpty()) return;
        try {
            double costo = Double.parseDouble(txtCostoSinIVA.getText().replace(",", "."));
            int margen = cmbGanancia.getValue() != null ? cmbGanancia.getValue() : 0;
            txtPrecioVenta.setText(String.format("%.2f", costo + (costo * margen / 100.0)).replace(",", "."));
        } catch (Exception ignored) {}
    }

    private void validarSoloNumeros(){
        txtCostoSinIVA.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !val.matches("\\d*(\\.\\d{0,2})?")) txtCostoSinIVA.setText(old);
        });
    }

    private void configurarCodigoAuto() {
        txtCodigo.setEditable(false);
        Runnable actualizarCodigo = () -> {
            Grupo g = cmbGrupo.getValue();
            Marca m = cmbMarca.getValue();
            String desc = txtDescripcion.getText();
            BigDecimal costo = null;
            try {
                costo = (!txtCostoSinIVA.getText().isEmpty())
                        ? new BigDecimal(txtCostoSinIVA.getText().replace(",", "."))
                        : null;
            } catch (NumberFormatException ignored) {}
            if (desc != null && !desc.isEmpty()) {
                txtCodigo.setText(generarCodigo(desc, g, m, costo));
            } else {
                txtCodigo.clear();
            }
        };
        txtDescripcion.textProperty().addListener((obs, o, n) -> actualizarCodigo.run());
        txtCostoSinIVA.textProperty().addListener((obs, o, n) -> actualizarCodigo.run());
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
    }
}
