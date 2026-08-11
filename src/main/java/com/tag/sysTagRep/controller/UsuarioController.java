package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.UsuarioDAO;
import com.tag.sysTagRep.model.Usuario;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.*;
import java.util.stream.Collectors;

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
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRol;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TableView<VistaPermiso> tblPermisos;
    @FXML private TableColumn<VistaPermiso, String> colVista;
    @FXML private TableColumn<VistaPermiso, Boolean> colHabilitado;

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
    private final LogDAO logDAO = new LogDAO();
    private ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();
    private ObservableList<VistaPermiso> listaPermisos = FXCollections.observableArrayList();

    private static final LinkedHashMap<String, String> VISTAS = new LinkedHashMap<>();
    static {
        VISTAS.put("comprobantes_nota_venta", "Comprobantes > Proforma");
        VISTAS.put("comprobantes_factura", "Comprobantes > Factura Electrónica");
        VISTAS.put("perchero_ubicacion", "Inventario > Ubicación Percha");
        VISTAS.put("perchero_inventario", "Inventario > Gestión de Inventario");
        VISTAS.put("perchero_gestion_stock", "Inventario > Gestión de Stock");
        VISTAS.put("credito_por_cobrar", "Crédito > Por Cobrar");
        VISTAS.put("credito_por_pagar", "Crédito > Por Pagar");
        VISTAS.put("historial_productos", "Historial > Productos");
        VISTAS.put("historial_ventas", "Historial > Ventas");
        VISTAS.put("historial_compras", "Historial > Compras");
        VISTAS.put("reportes_comprobantes_venta", "Emisión > Comprobantes de Venta");
        VISTAS.put("reportes_comprobantes_compra", "Ingreso > Facturas Ingresadas");
        VISTAS.put("admin_vendedores", "Administración > Vendedores");
        VISTAS.put("admin_usuarios", "Administración > Usuarios");
        VISTAS.put("admin_proveedores", "Administración > Proveedores");
        VISTAS.put("admin_grupos", "Catálogos > Grupos");
        VISTAS.put("admin_marcas", "Catálogos > Marcas");
        VISTAS.put("admin_ubicaciones", "Catálogos > Ubicaciones");
    }

    private static final Set<String> VENDEDOR_DEFAULT = Set.of(
        "comprobantes_nota_venta", "comprobantes_factura",
        "perchero_ubicacion",
        "credito_por_cobrar",
        "reportes_comprobantes_venta"
    );

    private static final Set<String> ALMACENERO_DEFAULT = Set.of(
        "comprobantes_nota_venta", "comprobantes_factura",
        "perchero_ubicacion", "perchero_inventario", "perchero_gestion_stock",
        "credito_por_cobrar",
        "reportes_comprobantes_venta"
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatos();
        iniciarCmbEstado();
        iniciarCmbRol();
        iniciarTablaContenido();
        iniciarTablaPermisos();
        cargarAcciones();
        aplicarLimitadores();

        cmbRol.valueProperty().addListener((obs, old, rol) -> aplicarPermisosPorDefecto(rol));
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
        SortTable.agregarBotones(tblUsuarios);
    }

    private void iniciarTablaPermisos() {
        colVista.setCellValueFactory(cd -> cd.getValue().nombreProperty());
        colHabilitado.setCellFactory(col -> new TableCell<>() {
            private final CheckBox chk = new CheckBox();
            {
                chk.setOnAction(e -> {
                    VistaPermiso vp = getTableView().getItems().get(getIndex());
                    if (vp != null) vp.setHabilitado(chk.isSelected());
                });
            }
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else { chk.setSelected(item != null && item); setGraphic(chk); setAlignment(Pos.CENTER); }
            }
        });
        colHabilitado.setCellValueFactory(cd -> cd.getValue().habilitadoProperty().asObject());
        tblPermisos.setItems(listaPermisos);
        tblPermisos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblPermisos);
    }

    private void iniciarCmbEstado(){
        ComboFilter.habilitar(cmbEstado, FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        cmbEstado.setValue("ACTIVO");
    }

    private void iniciarCmbRol(){
        ComboFilter.habilitar(cmbRol, FXCollections.observableArrayList("ADMINISTRADOR", "VENDEDOR", "ALMACENERO"));
        cmbRol.setValue("VENDEDOR");
    }

    private void aplicarPermisosPorDefecto(String rol) {
        if (rol == null || (txtId.getText() != null && !txtId.getText().isEmpty())) return;
        listaPermisos.clear();
        Set<String> habilitadas;
        switch (rol) {
            case "ADMINISTRADOR": habilitadas = VISTAS.keySet(); break;
            case "ALMACENERO": habilitadas = ALMACENERO_DEFAULT; break;
            default: habilitadas = VENDEDOR_DEFAULT;
        }
        for (Map.Entry<String, String> e : VISTAS.entrySet()) {
            listaPermisos.add(new VistaPermiso(e.getKey(), e.getValue(), habilitadas.contains(e.getKey())));
        }
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
        try {
            if (!validarCampos()) return;

            Usuario u = new Usuario();
            u.setNombre(txtNombre.getText().trim());
            u.setApellido(txtApellido.getText().trim());
            u.setCorreo(txtCorreo.getText().trim());
            u.setUsername(txtUsuario.getText().trim());
            u.setPassword(txtPassword.getText());
            u.setRol(cmbRol.getValue());
            u.setEstado("ACTIVO".equals(cmbEstado.getValue()));

            String permisosStr = listaPermisos.stream()
                    .filter(VistaPermiso::isHabilitado)
                    .map(VistaPermiso::getKey)
                    .collect(Collectors.joining(","));
            u.setPermisos(permisosStr);

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(u);
            } else {
                u.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(u);
            }

            limpiarFrm();
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("UsuarioController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    private boolean validarCampos() {
        String msg = "";
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) msg += "- Nombre es obligatorio.\n";
        if (txtApellido.getText() == null || txtApellido.getText().trim().isEmpty()) msg += "- Apellido es obligatorio.\n";
        if (txtCorreo.getText() == null || txtCorreo.getText().trim().isEmpty()) msg += "- Correo es obligatorio.\n";
        if (txtUsuario.getText() == null || txtUsuario.getText().trim().isEmpty()) msg += "- Usuario es obligatorio.\n";
        if (txtPassword.getText() == null || txtPassword.getText().trim().isEmpty()) {
            if (txtId.getText() == null || txtId.getText().isEmpty()) msg += "- Contraseña es obligatoria.\n";
        }
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
        txtUsuario.clear();
        txtPassword.clear();
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
        txtUsuario.setText(u.getUsername());
        txtPassword.clear();
        cmbRol.setValue(u.getRol());
        cmbEstado.setValue(u.isEstado() ? "ACTIVO" : "INACTIVO");

        Set<String> habilitadas = u.getPermisos() != null && !u.getPermisos().isEmpty()
                ? new HashSet<>(Arrays.asList(u.getPermisos().split(",")))
                : Set.of();
        listaPermisos.clear();
        for (Map.Entry<String, String> e : VISTAS.entrySet()) {
            listaPermisos.add(new VistaPermiso(e.getKey(), e.getValue(), habilitadas.contains(e.getKey())));
        }
    }

    public static class VistaPermiso {
        private final String key;
        private final SimpleStringProperty nombre;
        private final SimpleBooleanProperty habilitado;

        public VistaPermiso(String key, String nombre, boolean habilitado) {
            this.key = key;
            this.nombre = new SimpleStringProperty(nombre);
            this.habilitado = new SimpleBooleanProperty(habilitado);
        }

        public String getKey() { return key; }
        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public boolean isHabilitado() { return habilitado.get(); }
        public SimpleBooleanProperty habilitadoProperty() { return habilitado; }
        public void setHabilitado(boolean h) { habilitado.set(h); }
    }
}
