package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.ProveedorDAO;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.Proveedor;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class InventarioController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtGrupo;
    @FXML private TextField txtMarca;
    @FXML private TextField txtCostoSinIVA;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private TextField txtUbicacionPercha;
    @FXML private TextField txtPrecioVenta;
    @FXML private DatePicker dpFechaIngreso;
    @FXML private ComboBox<Integer> cmbGanancia;
    @FXML private TextField txtCodigo;
    @FXML private ComboBox<Proveedor> cmbProveedor;

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

    private FilteredList<Inventario> inventarioFiltrado;
    private final InventarioDAO dao = new InventarioDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private ObservableList<Inventario> listaInventario = FXCollections.observableArrayList();
    private ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        iniciarCbProveedores();
        iniciarSpCantidad();
        iniciarCbMargen();
        iniciarTablaContenido();
        cargarDatos();
        limpiarFrm();
        cargarAcciones();
        configurarCalculoPrecio();
        validarSoloNumeros();
        filtroBusqueda();
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
            if (selected == null || !selected.getNombre().equals(newVal)) {
                filteredProveedores.setPredicate(p -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return p.getNombre().toLowerCase().contains(newVal.toLowerCase());
                });
                cmbProveedor.show();
            }
        });
    }

    private void cargarDatos() {
        listaInventario.setAll(dao.listar());
        if (inventarioFiltrado != null) {
            aplicarFiltro(txtBuscar.getText());
        }
    }

    @FXML
    private void guardar() {
        try {
            Inventario i = new Inventario();
            i.setDescripcion(txtDescripcion.getText());
            i.setGrupo(txtGrupo.getText());
            i.setMarca(txtMarca.getText());
            i.setCostoSinIVA(new BigDecimal(txtCostoSinIVA.getText().replace(",", ".")));
            i.setCantidad(spCantidad.getValue());
            i.setUbicacionPercha(txtUbicacionPercha.getText());
            i.setPrecioVenta(new BigDecimal(txtPrecioVenta.getText().replace(",", ".")));
            i.setCodigo(txtCodigo.getText());
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

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(i);
            } else {
                i.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(i);
            }

            limpiarFrm();
            cargarDatos();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage());
            alert.show();
        }
    }

    private void cargarFormulario(Inventario i) {
        txtId.setText(String.valueOf(i.getId()));
        txtDescripcion.setText(i.getDescripcion());
        txtGrupo.setText(i.getGrupo());
        txtMarca.setText(i.getMarca());
        txtCostoSinIVA.setText(i.getCostoSinIVA().toString());
        spCantidad.getValueFactory().setValue(i.getCantidad());
        txtUbicacionPercha.setText(i.getUbicacionPercha());
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
    }

    public void limpiarFrm(){
        txtId.clear(); txtDescripcion.clear(); txtGrupo.clear(); txtMarca.clear();
        txtCostoSinIVA.clear(); spCantidad.getValueFactory().setValue(0);
        txtUbicacionPercha.clear(); txtPrecioVenta.clear(); txtCodigo.clear();
        dpFechaIngreso.setValue(LocalDate.now()); cmbProveedor.setValue(null);
        cmbProveedor.getEditor().clear();
    }

    private void filtroBusqueda(){
        inventarioFiltrado = new FilteredList<>(listaInventario, p -> true);
        tblInventario.setItems(inventarioFiltrado);
        txtBuscar.textProperty().addListener((obs, old, val) -> aplicarFiltro(val));
    }

    private void aplicarFiltro(String texto) {
        if (texto == null || texto.trim().isEmpty()) { inventarioFiltrado.setPredicate(p -> true); return; }
        String[] partes = texto.toLowerCase().split("%");
        inventarioFiltrado.setPredicate(inv -> {
            String searchStr = (inv.getDescripcion() + " " + inv.getGrupo() + " " + inv.getMarca() + " " + inv.getCodigo()).toLowerCase();
            for (String p : partes) if (!p.isBlank() && !searchStr.contains(p)) return false;
            return true;
        });
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

    private void iniciarSpCantidad(){
        spCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999, 1));
        spCantidad.setEditable(true);
    }

    @FXML private void toggleFormulario() {
        boolean visible = !formPane.isVisible();
        formPane.setVisible(visible); formPane.setManaged(visible);
        splitPane.setDividerPositions(visible ? 0.35 : 0.0);
        ((FontIcon) btnToggleForm.getGraphic()).setIconLiteral(visible ? "fas-chevron-left" : "fas-chevron-right");
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
