package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.GrupoDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.model.Grupo;
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

public class GrupoController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TableView<Grupo> tblGrupo;
    @FXML private TableColumn<Grupo, Integer> colId;
    @FXML private TableColumn<Grupo, String> colNombre;
    @FXML private TableColumn<Grupo, String> colEstado;
    @FXML private TableColumn<Grupo, Void> colAcciones;

    @FXML private TextField txtBuscar;

    private final GrupoDAO dao = new GrupoDAO();
    private final LogDAO logDAO = new LogDAO();
    private ObservableList<Grupo> listaGrupo = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cmbEstado.getItems().addAll("ACTIVO", "INACTIVO");
        cmbEstado.setValue("ACTIVO");

        tblGrupo.setItems(listaGrupo);
        cargarDatos();
        cargarAcciones();
        filtroBusqueda();
    }

    private void cargarDatos() {
        listaGrupo.setAll(dao.listar());
    }

    @FXML
    private void guardar() {
        try {
            if (txtNombre.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese un nombre.").showAndWait();
                return;
            }
            Grupo g = new Grupo();
            g.setNombre(txtNombre.getText().trim());
            g.setEstado("ACTIVO".equals(cmbEstado.getValue()));

            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                dao.guardar(g);
            } else {
                g.setId(Integer.parseInt(txtId.getText()));
                dao.actualizar(g);
            }
            limpiarFrm();
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Guardado correctamente.").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("GrupoController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    private void cargarFormulario(Grupo g) {
        txtId.setText(String.valueOf(g.getId()));
        txtNombre.setText(g.getNombre());
        cmbEstado.setValue(g.isEstado() ? "ACTIVO" : "INACTIVO");
    }

    public void limpiarFrm() {
        txtId.clear();
        txtNombre.clear();
        cmbEstado.setValue("ACTIVO");
    }

    private void filtroBusqueda() {
        FilteredList<Grupo> filtered = new FilteredList<>(listaGrupo, p -> true);
        tblGrupo.setItems(filtered);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) { filtered.setPredicate(p -> true); return; }
            String texto = val.toLowerCase();
            filtered.setPredicate(g -> g.getNombre().toLowerCase().contains(texto));
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
                    Grupo g = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar este grupo?", ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) { dao.eliminar(g.getId()); cargarDatos(); }
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
