package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.UsuarioDAO;
import com.tag.sysTagRep.model.Usuario;
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
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class UsuarioController implements Initializable {
    private boolean formularioVisible = true;

    @FXML private SplitPane splitPane;
    @FXML private ScrollPane formPane;
    @FXML private Button btnToggleForm;
    @FXML private TextField txtBuscar;

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCorreo;
    @FXML private ComboBox<String> cmbRol;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colApellido;
    @FXML private TableColumn<Usuario, String> colCorreo;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, LocalDateTime> colFechaCreacion;
    @FXML private TableColumn<Usuario, LocalDateTime> colUltimoLogin;
    @FXML private TableColumn<Usuario, Boolean> colEstado;
    @FXML private TableColumn<Usuario, Void> colAcciones;

    private final UsuarioDAO dao = new UsuarioDAO();
    private ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatos();
        iniciarCmbEstado();
        iniciarCmbRol();
        iniciarTablaContenido();
        cargarAcciones();
        aplicarLimitadores();
    }

    private void aplicarLimitadores() {
        txtNombre.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 30) txtNombre.setText(old);
        });
        txtApellido.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 30) txtApellido.setText(old);
        });
        txtCorreo.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && newValue.length() > 50) txtCorreo.setText(old);
        });
    }

    private void iniciarTablaContenido(){
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellido.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colFechaCreacion.setCellValueFactory(new PropertyValueFactory<>("fecha_creacion"));
        colUltimoLogin.setCellValueFactory(new PropertyValueFactory<>("ultimo_login"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tblUsuarios.setItems(listaUsuarios);
    }

    private void iniciarCmbEstado(){
        cmbEstado.getItems().addAll("ACTIVO", "INACTIVO");
        cmbEstado.setValue("ACTIVO");
    }

    private void iniciarCmbRol(){
        cmbRol.getItems().addAll("ADMINISTRADOR", "VENDEDOR", "ALMACENERO");
        cmbRol.setValue("VENDEDOR");
    }

    @FXML
    private void toggleFormulario() {
        if (formularioVisible) {
            formPane.setVisible(false);
            formPane.setManaged(false);
            splitPane.setDividerPositions(0.0);
            ((FontIcon) btnToggleForm.getGraphic()).setIconLiteral("fas-chevron-right");
        } else {
            formPane.setVisible(true);
            formPane.setManaged(true);
            splitPane.setDividerPositions(0.35);
            ((FontIcon) btnToggleForm.getGraphic()).setIconLiteral("fas-chevron-left");
        }
        formularioVisible = !formularioVisible;
    }

    @FXML
    private void guardar() {
        if (!validarCampos()) return;

        Usuario u = new Usuario();
        u.setNombre(txtNombre.getText().trim());
        u.setApellido(txtApellido.getText().trim());
        u.setCorreo(txtCorreo.getText().trim());
        u.setRol(cmbRol.getValue());
        u.setEstado(cmbEstado.getValue().equals("ACTIVO"));

        if (txtId.getText() == null || txtId.getText().isEmpty()) {
            dao.guardar(u);
        } else {
            u.setId(Integer.parseInt(txtId.getText()));
            dao.actualizar(u);
        }

        limpiarFrm();
        cargarDatos();
    }

    private boolean validarCampos() {
        String msg = "";
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) msg += "- Nombre es obligatorio.\n";
        if (txtApellido.getText() == null || txtApellido.getText().trim().isEmpty()) msg += "- Apellido es obligatorio.\n";
        if (txtCorreo.getText() == null || txtCorreo.getText().trim().isEmpty()) msg += "- Correo es obligatorio.\n";
        if (cmbRol.getValue() == null) msg += "- Rol es obligatorio.\n";
        if (cmbEstado.getValue() == null) msg += "- Estado es obligatorio.\n";

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
        txtApellido.clear();
        txtCorreo.clear();
        cmbRol.setValue("VENDEDOR");
        cmbEstado.setValue("ACTIVO");
    }

    private void cargarDatos() {
        listaUsuarios.clear();
        listaUsuarios.addAll(dao.listar());
    }

    private void cargarAcciones(){
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final FontIcon iconEditar = new FontIcon(FontAwesomeSolid.EDIT);
            private final FontIcon iconEliminar = new FontIcon(FontAwesomeSolid.TRASH);
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

                btnActualizar.setOnAction(e -> cargarFormulario(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarUsuario(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
    }

    private void eliminarUsuario(Usuario u) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Eliminar el usuario seleccionado?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            dao.eliminar(u.getId());
            cargarDatos();
        }
    }

    private void cargarFormulario(Usuario u) {
        txtId.setText(String.valueOf(u.getId()));
        txtNombre.setText(u.getNombre());
        txtApellido.setText(u.getApellido());
        txtCorreo.setText(u.getCorreo());
        cmbRol.setValue(u.getRol());
        cmbEstado.setValue(u.isEstado() ? "ACTIVO" : "INACTIVO");
    }
}
