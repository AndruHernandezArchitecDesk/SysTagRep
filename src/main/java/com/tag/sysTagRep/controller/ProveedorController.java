package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.ProveedorDAO;
import com.tag.sysTagRep.model.Proveedor;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class ProveedorController implements Initializable {
    private boolean formularioVisible = true;

    @FXML private SplitPane splitPane;
    @FXML private ScrollPane formPane;
    @FXML private Button btnToggleForm;

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtIdentificacion;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCelular;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TableView<Proveedor> tblProveedores;
    @FXML private TableColumn<Proveedor, Integer> colId;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colIdentificacion;
    @FXML private TableColumn<Proveedor, String> colDireccion;
    @FXML private TableColumn<Proveedor, String> colCorreo;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colCelular;
    @FXML private TableColumn<Proveedor, Boolean> colEstado;
    @FXML private TableColumn<Proveedor, String> colFechaRegistro;
    @FXML private TableColumn<Proveedor, Void> colAcciones;

    private final ProveedorDAO dao = new ProveedorDAO();
    private final LogDAO logDAO = new LogDAO();
    private ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatos();
        iniciarCmbEstado();
        iniciarTablaContenido();
        cargarAcciones();
        aplicarLimitadores();
    }

    private void aplicarLimitadores() {
        // Nombre: max 30
        txtNombre.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 30) txtNombre.setText(old);
        });

        // Correo: max 60
        txtCorreo.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 60) txtCorreo.setText(old);
        });

        // Identificación: solo números y max 13
        txtIdentificacion.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                if (!newValue.matches("\\d*")) {
                    txtIdentificacion.setText(old);
                } else if (newValue.length() > 13) {
                    txtIdentificacion.setText(old);
                }
            }
        });

        // Dirección: max 70
        txtDireccion.setTextFormatter(new TextFormatter<>(change -> 
            change.getControlNewText().length() <= 70 ? change : null));

        // Teléfono: solo números y max 10
        txtTelefono.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                if (!newValue.matches("\\d*")) {
                    txtTelefono.setText(old);
                } else if (newValue.length() > 10) {
                    txtTelefono.setText(old);
                }
            }
        });

        // Celular: solo números y max 10
        txtCelular.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                if (!newValue.matches("\\d*")) {
                    txtCelular.setText(old);
                } else if (newValue.length() > 10) {
                    txtCelular.setText(old);
                }
            }
        });
    }

    private void iniciarTablaContenido(){
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colIdentificacion.setCellValueFactory(new PropertyValueFactory<>("identificacion"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCelular.setCellValueFactory(new PropertyValueFactory<>("celular"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colFechaRegistro.setCellValueFactory(new PropertyValueFactory<>("fecha_registro"));

        tblProveedores.setItems(listaProveedores);
        SortTable.agregarBotones(tblProveedores);
    }


    private void iniciarCmbEstado(){
        ComboFilter.habilitar(cmbEstado, FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        cmbEstado.setValue("ACTIVO");
    }

    @FXML
    private void toggleFormulario() {

        if (formularioVisible) {
            // OCULTAR
            formPane.setVisible(false);
            formPane.setManaged(false);
            splitPane.setDividerPositions(0.0);

            ((FontIcon) btnToggleForm.getGraphic())
                    .setIconLiteral("fas-chevron-right");

        } else {
            // MOSTRAR
            formPane.setVisible(true);
            formPane.setManaged(true);
            splitPane.setDividerPositions(0.35);

            ((FontIcon) btnToggleForm.getGraphic())
                    .setIconLiteral("fas-chevron-left");
        }

        formularioVisible = !formularioVisible;
    }

    @FXML
    private void guardar() {
        try {
            if (!validarCampos()) {
                return;
            }

            Proveedor v = new Proveedor();
            v.setNombre(txtNombre.getText().trim());
            v.setIdentificacion(txtIdentificacion.getText().trim());
            v.setDireccion(txtDireccion.getText().trim());
            v.setCorreo(txtCorreo.getText().trim());
            v.setTelefono(txtTelefono.getText().trim());
            v.setCelular(txtCelular.getText().trim());
            v.setEstado("ACTIVO".equals(cmbEstado.getValue()));

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(v);
            } else {
                v.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(v);
            }

            limpiarFrm();
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("ProveedorController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    private boolean validarCampos() {
        String msg = "";
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) msg += "- Nombre es obligatorio.\n";
        
        String ident = txtIdentificacion.getText();
        if (ident == null || ident.trim().isEmpty()) {
            msg += "- Identificación es obligatoria.\n";
        } else if (ident.length() < 10) {
            msg += "- Identificación debe tener al menos 10 dígitos.\n";
        }

        if (cmbEstado.getValue() == null) msg += "- Debe seleccionar un estado.\n";

        if (!msg.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validación de datos");
            alert.setHeaderText("Por favor complete los siguientes campos:");
            alert.setContentText(msg);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    public void limpiarFrm(){
        txtId.clear();
        txtNombre.clear();
        txtIdentificacion.clear();
        txtDireccion.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtCelular.clear();
        cmbEstado.setValue("ACTIVO");
    }

    private void cargarDatos() {
        listaProveedores.clear();
        listaProveedores.addAll(dao.listar());
    }

    private void cargarAcciones(){
        colAcciones.setCellFactory(param -> new TableCell<>() {

            private final FontIcon iconEditar  =
                    new FontIcon(FontAwesomeSolid.EDIT);

            private final FontIcon iconEliminar =
                    new FontIcon(FontAwesomeSolid.TRASH);

            private final Button btnActualizar = new Button();
            private final Button btnEliminar = new Button();

            private final HBox hbox = new HBox(10);

            {
                iconEditar.setIconSize(16);
                iconEditar.setIconColor(Color.DODGERBLUE);

                iconEliminar.setIconSize(16);
                iconEliminar.setIconColor(Color.RED);

                btnActualizar.setGraphic(iconEditar);
                btnEliminar.setGraphic(iconEliminar);

                btnActualizar.setStyle("-fx-background-color: transparent;");
                btnEliminar.setStyle("-fx-background-color: transparent;");

                btnActualizar.setTooltip(new Tooltip("Actualizar"));
                btnEliminar.setTooltip(new Tooltip("Eliminar"));

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnActualizar, btnEliminar);

                btnActualizar.setOnAction(e -> {
                    Proveedor v = getTableView().getItems().get(getIndex());
                    cargarFormulario(v);
                });

                btnEliminar.setOnAction(e -> {
                    Proveedor v = getTableView().getItems().get(getIndex());
                    eliminarProveedor(v);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
    }

    private void eliminarProveedor(Proveedor v) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Eliminar el proveedor seleccionado?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            dao.eliminar(v.getId());
            cargarDatos();
        }
    }

    private void cargarFormulario(Proveedor v) {
        txtId.setText(String.valueOf(v.getId()));
        txtNombre.setText(v.getNombre());
        txtIdentificacion.setText(v.getIdentificacion());
        txtDireccion.setText(v.getDireccion());
        txtCorreo.setText(v.getCorreo());
        txtTelefono.setText(v.getTelefono());
        txtCelular.setText(v.getCelular());
        cmbEstado.setValue(v.isEstado() ? "ACTIVO" : "INACTIVO");
    }
}
