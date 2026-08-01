package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.MarcaDAO;
import com.tag.sysTagRep.model.Marca;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
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
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class MarcaController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TableView<Marca> tblMarca;
    @FXML private TableColumn<Marca, Integer> colId;
    @FXML private TableColumn<Marca, String> colNombre;
    @FXML private TableColumn<Marca, String> colEstado;
    @FXML private TableColumn<Marca, Void> colAcciones;

    @FXML private TextField txtBuscar;

    private final MarcaDAO dao = new MarcaDAO();
    private final LogDAO logDAO = new LogDAO();
    private ObservableList<Marca> listaMarca = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        ComboFilter.habilitar(cmbEstado, FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        cmbEstado.setValue("ACTIVO");

        tblMarca.setItems(listaMarca);
        SortTable.agregarBotones(tblMarca);
        cargarDatos();
        cargarAcciones();
        filtroBusqueda();
    }

    private void cargarDatos() {
        listaMarca.setAll(dao.listar());
    }

    @FXML
    private void guardar() {
        try {
            if (txtNombre.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese un nombre.").showAndWait();
                return;
            }
            Marca m = new Marca();
            m.setNombre(txtNombre.getText().trim());
            m.setEstado("ACTIVO".equals(cmbEstado.getValue()));

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(m);
            } else {
                m.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(m);
            }
            limpiarFrm();
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("MarcaController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    private void cargarFormulario(Marca m) {
        txtId.setText(String.valueOf(m.getId()));
        txtNombre.setText(m.getNombre());
        cmbEstado.setValue(m.isEstado() ? "ACTIVO" : "INACTIVO");
    }

    public void limpiarFrm() {
        txtId.clear();
        txtNombre.clear();
        cmbEstado.setValue("ACTIVO");
    }

    private void filtroBusqueda() {
        FilteredList<Marca> filtered = new FilteredList<>(listaMarca, p -> true);
        tblMarca.setItems(filtered);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) { filtered.setPredicate(p -> true); return; }
            String texto = val.toLowerCase();
            filtered.setPredicate(m -> m.getNombre().toLowerCase().contains(texto));
        });
    }

    private void cargarAcciones() {
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnActualizar = new Button();
            private final Button btnEliminar = new Button();
            private final HBox hbox = new HBox(10);
            {
                FontIcon iconEdit = new FontIcon(FontAwesomeSolid.EDIT); iconEdit.setIconSize(16); iconEdit.setIconColor(Color.DODGERBLUE);
                FontIcon iconTrash = new FontIcon(FontAwesomeSolid.TRASH); iconTrash.setIconSize(16); iconTrash.setIconColor(Color.RED);
                btnActualizar.setGraphic(iconEdit); btnEliminar.setGraphic(iconTrash);
                btnActualizar.setStyle("-fx-background-color: transparent;"); btnEliminar.setStyle("-fx-background-color: transparent;");
                btnActualizar.setTooltip(new Tooltip("Actualizar")); btnEliminar.setTooltip(new Tooltip("Eliminar"));
                hbox.setAlignment(Pos.CENTER); hbox.getChildren().addAll(btnActualizar, btnEliminar);
                btnActualizar.setOnAction(e -> cargarFormulario(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> {
                    Marca m = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar esta marca?", ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) { dao.eliminar(m.getId()); cargarDatos(); }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
    }
}
