package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CuentaPorPagarDAO;
import com.tag.sysTagRep.dao.GrupoDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.MarcaDAO;
import com.tag.sysTagRep.dao.ProveedorDAO;
import com.tag.sysTagRep.dao.UbicacionDetalleDAO;
import com.tag.sysTagRep.dao.UbicacionPerchaDAO;
import com.tag.sysTagRep.model.CuentaPorPagar;
import com.tag.sysTagRep.model.Grupo;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.Marca;
import com.tag.sysTagRep.model.Proveedor;
import com.tag.sysTagRep.model.UbicacionDetalle;
import com.tag.sysTagRep.model.UbicacionPercha;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class InventarioController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<Grupo> cmbGrupo;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private TextField txtCostoSinIVA;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private ComboBox<UbicacionDetalle> cmbUbicacion;
    @FXML private TextField txtPrecioVenta;
    @FXML private DatePicker dpFechaIngreso;
    @FXML private ComboBox<Integer> cmbGanancia;
    @FXML private TextField txtCodigo;
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private ComboBox<String> cmbFormaPago;
    @FXML private ComboBox<Integer> cmbMesesPlazo;
    @FXML private ComboBox<String> cmbInteres;
    @FXML private HBox pnlCredito;

    @FXML private TableView<Inventario> tblInventario;
    @FXML private TableColumn<Inventario, Integer> colId;
    @FXML private TableColumn<Inventario, String> colDescripcion;
    @FXML private TableColumn<Inventario, String> colGrupo;
    @FXML private TableColumn<Inventario, String> colMarca;
    @FXML private TableColumn<Inventario, BigDecimal> colCostoSinIva;
    @FXML private TableColumn<Inventario, Integer> colCantidad;
    @FXML private TableColumn<Inventario, String> colUbicacionPercha;
    @FXML private TableColumn<Inventario, BigDecimal> colPrecioVenta;
    @FXML private TableColumn<Inventario, LocalDateTime> colFechaIngreso;
    @FXML private TableColumn<Inventario, Boolean> colEstado;
    @FXML private TableColumn<Inventario, String> colCodigo;
    @FXML private TableColumn<Inventario, String> colProveedor;
    @FXML private TableColumn<Inventario, Void> colAcciones;

    @FXML private SplitPane splitPane;
    @FXML private ScrollPane formPane;
    @FXML private Button btnToggleForm;
    @FXML private TextField txtBuscar;

    @FXML private Label lblPaginaInfo;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private ComboBox<Integer> cmbPageSize;

    private int currentPage = 1;
    private int pageSize = 25;
    private int totalPages = 1;
    private int totalCount = 0;
    private final InventarioDAO dao = new InventarioDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final GrupoDAO grupoDAO = new GrupoDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final UbicacionDetalleDAO ubicacionDetalleDAO = new UbicacionDetalleDAO();
    private final UbicacionPerchaDAO ubicacionDAO = new UbicacionPerchaDAO();
    private final CuentaPorPagarDAO cuentaPorPagarDAO = new CuentaPorPagarDAO();
    private final com.tag.sysTagRep.dao.LogDAO logDAO = new com.tag.sysTagRep.dao.LogDAO();
    private ObservableList<Inventario> listaInventario = FXCollections.observableArrayList();
    private ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();
    private ObservableList<Grupo> listaGrupos = FXCollections.observableArrayList();
    private ObservableList<Marca> listaMarcas = FXCollections.observableArrayList();
    private ObservableList<UbicacionDetalle> listaUbicaciones = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        iniciarCbGrupos();
        iniciarCbMarcas();
        iniciarCbUbicaciones();
        iniciarCbProveedores();
        iniciarSpCantidad();
        iniciarCbMargen();
        iniciarTablaContenido();
        cargarDatos();
        limpiarFrm();
        cargarAcciones();
        configurarCalculoPrecio();
        validarSoloNumeros();
        configurarCodigoAuto();
        iniciarCbFormaPago();
        iniciarPageSize();
        txtBuscar.textProperty().addListener((obs, old, val) -> { currentPage = 1; cargarDatos(); });
    }

    private void iniciarCbGrupos() {
        listaGrupos.setAll(grupoDAO.listar());
        cmbGrupo.setItems(listaGrupos);
        cmbGrupo.setConverter(new StringConverter<>() {
            @Override public String toString(Grupo g) { return (g == null) ? "" : g.getNombre(); }
            @Override public Grupo fromString(String s) { return null; }
        });
    }

    private void iniciarCbMarcas() {
        listaMarcas.setAll(marcaDAO.listar());
        cmbMarca.setItems(listaMarcas);
        cmbMarca.setConverter(new StringConverter<>() {
            @Override public String toString(Marca m) { return (m == null) ? "" : m.getNombre(); }
            @Override public Marca fromString(String s) { return null; }
        });
    }

    private void iniciarCbUbicaciones() {
        listaUbicaciones.setAll(ubicacionDetalleDAO.listarOcupados());
        cmbUbicacion.setItems(listaUbicaciones);
        cmbUbicacion.setConverter(new StringConverter<>() {
            @Override public String toString(UbicacionDetalle u) {
                if (u == null) return "";
                if (u.getIdProducto() != null && u.getMarcaNombre() != null && u.getProductoDescripcion() != null)
                    return u.getCodigoUbicacion() + " - " + u.getMarcaNombre() + " " + u.getProductoDescripcion();
                return u.getCodigoUbicacion();
            }
            @Override public UbicacionDetalle fromString(String s) { return null; }
        });
    }

    private void iniciarCbProveedores() {
        listaProveedores.setAll(proveedorDAO.listar());
        FilteredList<Proveedor> filteredProveedores = new FilteredList<>(listaProveedores, p -> true);

        cmbProveedor.setItems(filteredProveedores);
        cmbProveedor.setEditable(true);

        cmbProveedor.setConverter(new StringConverter<>() {
            @Override public String toString(Proveedor p) { return (p == null) ? "" : p.getNombre(); }
            @Override public Proveedor fromString(String string) {
                return listaProveedores.stream()
                        .filter(p -> p.getNombre().equalsIgnoreCase(string))
                        .findFirst().orElse(null);
            }
        });

        cmbProveedor.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            final Proveedor selected = cmbProveedor.getSelectionModel().getSelectedItem();
            if (selected == null || (selected.getNombre() != null && !selected.getNombre().equals(newVal))) {
                filteredProveedores.setPredicate(p -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return p.getNombre() != null && p.getNombre().toLowerCase().contains(newVal.toLowerCase());
                });
                cmbProveedor.show();
            }
        });
    }

    private void cargarDatos() {
        String filtro = txtBuscar.getText();
        totalCount = dao.contar(filtro);
        totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        listaInventario.setAll(dao.listarPaginado(currentPage, pageSize, filtro));
        tblInventario.setItems(listaInventario);
        actualizarPaginaInfo();
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

    @FXML
    private void guardar() {
        try {
            Inventario i = new Inventario();
            i.setDescripcion(txtDescripcion.getText());

            Grupo g = cmbGrupo.getValue();
            i.setGrupoId(g != null ? g.getId() : 0);

            Marca m = cmbMarca.getValue();
            i.setMarcaId(m != null ? m.getId() : 0);

            i.setCostoSinIVA(new BigDecimal(txtCostoSinIVA.getText().replace(",", ".")));
            i.setCantidad(spCantidad.getValue());

            UbicacionDetalle ub = cmbUbicacion.getValue();

            i.setPrecioVenta(new BigDecimal(txtPrecioVenta.getText().replace(",", ".")));
            i.setCodigo(generarCodigo(txtDescripcion.getText(), g, m, new BigDecimal(txtCostoSinIVA.getText().replace(",", "."))));
            i.setFecha_ingreso(dpFechaIngreso.getValue() != null ? dpFechaIngreso.getValue().atStartOfDay() : LocalDateTime.now());

            Proveedor p = cmbProveedor.getValue();
            if (p == null && cmbProveedor.getEditor().getText() != null) {
                String texto = cmbProveedor.getEditor().getText();
                p = listaProveedores.stream()
                        .filter(prov -> prov.getNombre().equalsIgnoreCase(texto))
                        .findFirst().orElse(null);
            }

            if (p != null) {
                i.setProveedorId(p.getId());
            } else {
                i.setProveedorId(0);
            }

            i.setFormaPago(cmbFormaPago.getValue());
            if ("TAG Crédito".equals(cmbFormaPago.getValue())) {
                i.setMesesPlazo(cmbMesesPlazo.getValue());
                i.setInteres(new BigDecimal(cmbInteres.getValue()));
            } else {
                i.setMesesPlazo(0);
                i.setInteres(BigDecimal.ZERO);
            }

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                int inventarioId = dao.guardar(i);
                if (ub != null && inventarioId > 0) {
                    ubicacionDetalleDAO.ocupar(ub.getId(), inventarioId);
                }
                if ("TAG Crédito".equals(cmbFormaPago.getValue()) && p != null && inventarioId > 0) {
                    crearCreditoProveedor(inventarioId, p.getId(), i.getCostoSinIVA().multiply(BigDecimal.valueOf(i.getCantidad())), i.getMesesPlazo(), i.getInteres());
                }
            } else {
                i.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(i);
                if (ub != null) {
                    ubicacionDetalleDAO.ocupar(ub.getId(), i.getId());
                }
            }

            limpiarFrm();
            currentPage = 1;
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("InventarioController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    private void crearCreditoProveedor(int inventarioId, int proveedorId, BigDecimal total, int mesesPlazo, BigDecimal interesPct) {
        BigDecimal tasa = interesPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalConInteres = total.multiply(BigDecimal.ONE.add(tasa));
        BigDecimal cuotaMensual = totalConInteres.divide(BigDecimal.valueOf(mesesPlazo), 2, RoundingMode.HALF_UP);

        CuentaPorPagar cpp = new CuentaPorPagar(inventarioId, proveedorId, totalConInteres, mesesPlazo, interesPct, cuotaMensual);
        cuentaPorPagarDAO.insertar(cpp);
    }

    private void cargarFormulario(Inventario i) {
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

        for (UbicacionDetalle ub : listaUbicaciones) {
            if (ub.getIdProducto() != null && ub.getIdProducto() == i.getId()) { cmbUbicacion.setValue(ub); break; }
        }

        txtPrecioVenta.setText(i.getPrecioVenta().toString());
        txtCodigo.setText(i.getCodigo());
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
    }

    public void limpiarFrm(){
        txtId.clear(); txtDescripcion.clear();
        cmbGrupo.setValue(null); cmbMarca.setValue(null);
        txtCostoSinIVA.clear(); spCantidad.getValueFactory().setValue(0);
        cmbUbicacion.setValue(null); txtPrecioVenta.clear(); txtCodigo.clear();
        dpFechaIngreso.setValue(LocalDate.now()); cmbProveedor.setValue(null);
        cmbProveedor.getEditor().clear();
        cmbFormaPago.getSelectionModel().selectFirst();
        cmbMesesPlazo.getSelectionModel().selectFirst();
        cmbInteres.getSelectionModel().selectFirst();
    }

    private void iniciarPageSize() {
        cmbPageSize.setItems(FXCollections.observableArrayList(25, 50, 100));
        cmbPageSize.setValue(25);
        cmbPageSize.setOnAction(e -> { pageSize = cmbPageSize.getValue(); currentPage = 1; cargarDatos(); });
    }

    @FXML private void irPaginaAnterior() {
        if (currentPage > 1) { currentPage--; cargarDatos(); }
    }

    @FXML private void irPaginaSiguiente() {
        if (currentPage < totalPages) { currentPage++; cargarDatos(); }
    }

    private void actualizarPaginaInfo() {
        lblPaginaInfo.setText("Página " + currentPage + " de " + totalPages + " (" + totalCount + " registros)");
        btnAnterior.setDisable(currentPage <= 1);
        btnSiguiente.setDisable(currentPage >= totalPages);
    }

    private void iniciarTablaContenido(){
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCostoSinIva.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colUbicacionPercha.setCellValueFactory(new PropertyValueFactory<>("ubicacionPercha"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colFechaIngreso.setCellValueFactory(new PropertyValueFactory<>("fecha_ingreso"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
    }

    private void iniciarCbMargen(){
        cmbGanancia.getItems().clear();
        for (int i = 10; i <= 100; i += 10) cmbGanancia.getItems().add(i);
        cmbGanancia.setValue(40);
    }

    private void configurarCalculoPrecio() {
        txtCostoSinIVA.textProperty().addListener((obs, old, newVal) -> calcularPrecioVenta());
        cmbGanancia.valueProperty().addListener((obs, old, newVal) -> calcularPrecioVenta());
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
        cmbFormaPago.setItems(FXCollections.observableArrayList(
            "Efectivo", "Tarjeta de Crédito", "Tarjeta de Débito",
            "Transferencia", "Depósito", "Cheque", "TAG Crédito"
        ));
        cmbFormaPago.getSelectionModel().selectFirst();

        cmbMesesPlazo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6));
        cmbMesesPlazo.getSelectionModel().selectFirst();

        cmbInteres.setItems(FXCollections.observableArrayList("0", "3", "6", "9", "12", "15"));
        cmbInteres.getSelectionModel().selectFirst();

        cmbFormaPago.valueProperty().addListener((obs, old, valor) -> {
            boolean esCredito = "TAG Crédito".equals(valor);
            pnlCredito.setVisible(esCredito);
            pnlCredito.setManaged(esCredito);
        });
    }

    @FXML private void toggleFormulario() {
        boolean visible = !formPane.isVisible();
        formPane.setVisible(visible); formPane.setManaged(visible);
        splitPane.setDividerPositions(visible ? 0.35 : 0.0);
        ((FontIcon) btnToggleForm.getGraphic()).setIconLiteral(visible ? "fas-chevron-left" : "fas-chevron-right");
    }

    @FXML
    private void irAGrupo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/GrupoView.fxml"));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Gestión de Grupos");
            modal.setScene(new Scene(vista, 700, 500));
            modal.showAndWait();
            iniciarCbGrupos();
        } catch (Exception e) { logDAO.guardar("InventarioController", "irAGrupo", e.getMessage(), e); }
    }

    @FXML
    private void irAMarca() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MarcaView.fxml"));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Gestión de Marcas");
            modal.setScene(new Scene(vista, 700, 500));
            modal.showAndWait();
            iniciarCbMarcas();
        } catch (Exception e) { logDAO.guardar("InventarioController", "irAMarca", e.getMessage(), e); }
    }

    @FXML
    private void irAUbicacionPerchero() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UbicacionPercheroView.fxml"));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Ubicaciones en Perchero");
            modal.setScene(new Scene(vista, 900, 650));
            modal.showAndWait();
            iniciarCbUbicaciones();
        } catch (Exception e) { logDAO.guardar("InventarioController", "irAUbicacionPerchero", e.getMessage(), e); }
    }

    @FXML
    private void irAProveedor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProveedorView.fxml"));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Gestión de Proveedores");
            modal.setScene(new Scene(vista, 800, 600));
            modal.showAndWait();
            iniciarCbProveedores();
        } catch (Exception e) { logDAO.guardar("InventarioController", "irAProveedor", e.getMessage(), e); }
    }

    private void cargarAcciones(){
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnActualizar = new Button();
            private final Button btnEliminar = new Button();
            private final HBox hbox = new HBox(10);
            {
                FontIcon iconEdit = new FontIcon(FontAwesomeSolid.EDIT); iconEdit.setIconSize(16); iconEdit.setIconColor(Color.DODGERBLUE);
                FontIcon iconTrash = new FontIcon(FontAwesomeSolid.TRASH); iconTrash.setIconSize(16); iconTrash.setIconColor(Color.RED);
                btnActualizar.setGraphic(iconEdit); btnEliminar.setGraphic(iconTrash);
                btnActualizar.setStyle("-fx-background-color: transparent;"); btnEliminar.setStyle("-fx-background-color: transparent;");
                hbox.setAlignment(Pos.CENTER); hbox.getChildren().addAll(btnActualizar, btnEliminar);
                btnActualizar.setOnAction(e -> cargarFormulario(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminar(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
    }

    private void eliminar(Inventario i) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el artículo seleccionado?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) { dao.eliminar(i.getId()); cargarDatos(); }
    }
}
