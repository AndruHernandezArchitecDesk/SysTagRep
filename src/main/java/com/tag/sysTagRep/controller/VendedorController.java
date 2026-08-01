package com.tag.sysTagRep.controller;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.VendedorDAO;
import com.tag.sysTagRep.model.Vendedor;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class VendedorController implements Initializable{

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private TextField txtIdentificacion;
    @FXML private TextField txtCorreo;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TableView<Vendedor> tblVendedores;
    @FXML private TableColumn<Vendedor, Integer> colId;
    @FXML private TableColumn<Vendedor, String> colNombre;
    @FXML private TableColumn<Vendedor, String> colIdentificacion;
    @FXML private TableColumn<Vendedor, String> colCorreo;
    @FXML private TableColumn<Vendedor, String> colEstado;
    @FXML private TableColumn<Vendedor, Void> colAcciones;

    private boolean formularioVisible = true;
    @FXML private SplitPane splitPane;
    @FXML private ScrollPane formPane;
    @FXML private Button btnToggleForm;
    @FXML private TextField txtBuscar;

    private final VendedorDAO dao = new VendedorDAO();
    private final LogDAO logDAO = new LogDAO();
    private ObservableList<Vendedor> listaVendedores = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colIdentificacion.setCellValueFactory(new PropertyValueFactory<>("identificacion"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tblVendedores.setItems(listaVendedores);
        SortTable.agregarBotones(tblVendedores);

        cargarDatos();
        cargarAcciones();
        iniciarCmbEstado();
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

    private void iniciarCmbEstado(){
        ComboFilter.habilitar(cmbEstado, FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        cmbEstado.setValue("ACTIVO");
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colIdentificacion.setCellValueFactory(new PropertyValueFactory<>("identificacion"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void cargarDatos() {
        listaVendedores.clear();
        listaVendedores.addAll(dao.listar());
    }

    @FXML
    private void guardar() {
        try {
            Vendedor v = new Vendedor();
            v.setNombre(txtNombre.getText());
            v.setIdentificacion(txtIdentificacion.getText());
            v.setCorreo(txtCorreo.getText());
            v.setEstado("ACTIVO".equals(cmbEstado.getValue()));

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(v);
            } else if (Integer.parseInt(txtId.getText()) != 0) {
                v.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(v);
            }

            limpiarFrm();
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("VendedorController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
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
                    Vendedor v = getTableView().getItems().get(getIndex());
                    cargarFormulario(v);
                });

                btnEliminar.setOnAction(e -> {
                    Vendedor v = getTableView().getItems().get(getIndex());
                    eliminarVendedor(v);
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

    private void eliminarVendedor(Vendedor v) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText(null);
        alert.setContentText("¿Eliminar el vendedor seleccionado?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            dao.eliminar(v.getId());
            cargarDatos();
        }
    }

    private void cargarFormulario(Vendedor v) {
        txtId.setText(String.valueOf(v.getId()));
        txtNombre.setText(v.getNombre());
        txtIdentificacion.setText(v.getIdentificacion());
        txtCorreo.setText(v.getCorreo());
        cmbEstado.setValue(v.isEstado() ? "ACTIVO" : "INACTIVO");
    }

    public void limpiarFrm(){
        txtId.clear();
        txtNombre.clear();
        txtIdentificacion.clear();
        txtCorreo.clear();
        cmbEstado.setValue(null);
    }
}
