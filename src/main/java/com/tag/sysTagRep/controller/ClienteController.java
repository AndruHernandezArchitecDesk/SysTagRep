package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ClienteDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.model.Cliente;
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

public class ClienteController implements Initializable {
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
    @FXML private TextField txtBuscar;

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colIdentificacion;
    @FXML private TableColumn<Cliente, String> colDireccion;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCelular;
    @FXML private TableColumn<Cliente, Boolean> colEstado;
    @FXML private TableColumn<Cliente, String> colFechaRegistro;
    @FXML private TableColumn<Cliente, Void> colAcciones;

    private final ClienteDAO dao = new ClienteDAO();
    private final LogDAO logDAO = new LogDAO();
    private ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatos();
        iniciarCmbEstado();
        iniciarTablaContenido();
        cargarAcciones();
        aplicarLimitadores();
        configurarBusqueda();
    }

    private void aplicarLimitadores() {
        txtNombre.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 30) txtNombre.setText(old);
        });

        txtCorreo.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 60) txtCorreo.setText(old);
        });

        txtIdentificacion.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                if (!newValue.matches("\\d*")) {
                    txtIdentificacion.setText(old);
                } else if (newValue.length() > 13) {
                    txtIdentificacion.setText(old);
                }
            }
        });

        txtDireccion.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= 70 ? change : null));

        txtTelefono.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                if (!newValue.matches("\\d*")) {
                    txtTelefono.setText(old);
                } else if (newValue.length() > 10) {
                    txtTelefono.setText(old);
                }
            }
        });

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

    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((obs, old, newValue) -> {
            String filtro = newValue == null ? "" : newValue.trim().toLowerCase();
            if (filtro.isEmpty()) {
                tblClientes.setItems(listaClientes);
                return;
            }
            ObservableList<Cliente> resultado = FXCollections.observableArrayList();
            for (Cliente c : listaClientes) {
                if (c.getNombre() != null && c.getNombre().toLowerCase().contains(filtro)) {
                    resultado.add(c);
                } else if (c.getIdentificacion() != null && c.getIdentificacion().contains(filtro)) {
                    resultado.add(c);
                }
            }
            tblClientes.setItems(resultado);
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

        tblClientes.setItems(listaClientes);
        SortTable.agregarBotones(tblClientes);
    }

    private void iniciarCmbEstado(){
        ComboFilter.habilitar(cmbEstado, FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        cmbEstado.setValue("ACTIVO");
    }

    @FXML
    private void toggleFormulario() {

        if (formularioVisible) {
            formPane.setVisible(false);
            formPane.setManaged(false);
            splitPane.setDividerPositions(0.0);

            ((FontIcon) btnToggleForm.getGraphic())
                    .setIconLiteral("fas-chevron-right");

        } else {
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

            Cliente c = new Cliente();
            c.setNombre(txtNombre.getText().trim());
            c.setIdentificacion(txtIdentificacion.getText().trim());
            c.setDireccion(txtDireccion.getText().trim());
            c.setCorreo(txtCorreo.getText().trim());
            c.setTelefono(txtTelefono.getText().trim());
            c.setCelular(txtCelular.getText().trim());
            c.setEstado("ACTIVO".equals(cmbEstado.getValue()));

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(c);
            } else {
                c.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(c);
            }

            limpiarFrm();
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("ClienteController", "guardar", e.getMessage(), e);
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
        listaClientes.clear();
        listaClientes.addAll(dao.listar());
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
                    Cliente c = getTableView().getItems().get(getIndex());
                    cargarFormulario(c);
                });

                btnEliminar.setOnAction(e -> {
                    Cliente c = getTableView().getItems().get(getIndex());
                    eliminarCliente(c);
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

    private void eliminarCliente(Cliente c) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Eliminar el cliente seleccionado?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            dao.eliminar(c.getId());
            cargarDatos();
        }
    }

    private void cargarFormulario(Cliente c) {
        txtId.setText(String.valueOf(c.getId()));
        txtNombre.setText(c.getNombre());
        txtIdentificacion.setText(c.getIdentificacion());
        txtDireccion.setText(c.getDireccion());
        txtCorreo.setText(c.getCorreo());
        txtTelefono.setText(c.getTelefono());
        txtCelular.setText(c.getCelular());
        cmbEstado.setValue(c.isEstado() ? "ACTIVO" : "INACTIVO");
    }
}
