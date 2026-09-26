package com.vendex.controller;

import com.vendex.dao.PermisoDAO;
import com.vendex.dao.RolDAO;
import com.vendex.exception.SinPermisoException;
import com.vendex.model.Permiso;
import com.vendex.model.Rol;
import com.vendex.util.SesionActual;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import com.vendex.dao.RolDAOPostgres;
import com.vendex.dao.PermisoDAOPostgres;

public class GestionRolesController implements Initializable {

    @FXML private ComboBox<Rol> cmbRol;
    @FXML private TableView<FilaPermiso> tblMatriz;
    @FXML private TableColumn<FilaPermiso, String> colCategoria;
    @FXML private TableColumn<FilaPermiso, String> colCodigo;
    @FXML private TableColumn<FilaPermiso, String> colDescripcion;
    @FXML private TableColumn<FilaPermiso, Boolean> colAsignado;
    @FXML private TextField txtLimiteDescuento;
    @FXML private Label lblInfo;
    @FXML private Button btnGuardar;

    private final RolDAO rolDAO = new RolDAOPostgres();
    private final PermisoDAO permisoDAO = new PermisoDAOPostgres();
    private final ObservableList<FilaPermiso> filas = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try { SesionActual.exigirPermiso("USUARIO_GESTIONAR"); } catch (SinPermisoException e) {
            if (lblInfo != null) lblInfo.setText("Sin permiso USUARIO_GESTIONAR");
            if (btnGuardar != null) btnGuardar.setDisable(true);
            return;
        }
        colCategoria.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPermiso().getCategoria()));
        colCodigo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPermiso().getCodigo()));
        colDescripcion.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPermiso().getDescripcion()));
        colAsignado.setCellValueFactory(cd -> cd.getValue().asignadoProperty().asObject());
        colAsignado.setCellFactory(col -> new TableCell<>() {
            private final CheckBox chk = new CheckBox();
            {
                chk.setOnAction(e -> {
                    FilaPermiso fp = getTableView().getItems().get(getIndex());
                    if (fp != null) fp.setAsignado(chk.isSelected());
                });
            }
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else { chk.setSelected(item != null && item); setGraphic(chk); }
            }
        });
        tblMatriz.setItems(filas);
        List<Rol> roles = rolDAO.listar();
        cmbRol.setItems(FXCollections.observableArrayList(roles));
        cmbRol.setCellFactory(lv -> new ListCell<>() { @Override protected void updateItem(Rol r, boolean empty){ super.updateItem(r, empty); setText(empty||r==null?null:r.getNombre()); }});
        cmbRol.setButtonCell(new ListCell<>() { @Override protected void updateItem(Rol r, boolean empty){ super.updateItem(r, empty); setText(empty||r==null?null:r.getNombre()); }});
        cmbRol.valueProperty().addListener((obs,o,n)-> cargarMatriz(n));
        if (!roles.isEmpty()) cmbRol.setValue(roles.get(0));
    }

    private void cargarMatriz(Rol rol) {
        filas.clear();
        if (rol == null) return;
        List<Permiso> todos = permisoDAO.listarTodos();
        Set<String> asignados = permisoDAO.listarPorRol(rol.getId());
        for (Permiso p : todos) {
            filas.add(new FilaPermiso(p, asignados.contains(p.getCodigo())));
        }
        txtLimiteDescuento.setText(rol.getLimiteDescuentoPct()==null?"":rol.getLimiteDescuentoPct().toPlainString());
        if (lblInfo != null) lblInfo.setText("Rol: " + rol.getNombre() + " - " + (rol.getDescripcion()!=null?rol.getDescripcion():""));
    }

    @FXML
    private void guardar() {
        Rol rol = cmbRol.getValue();
        if (rol == null) { new Alert(Alert.AlertType.WARNING,"Seleccione un rol").showAndWait(); return; }
        try { SesionActual.exigirPermiso("USUARIO_GESTIONAR"); } catch (SinPermisoException e) { new Alert(Alert.AlertType.ERROR,"Sin permiso: "+e.getPermiso()).showAndWait(); return; }
        // validar tope
        BigDecimal limite = null;
        String txt = txtLimiteDescuento.getText();
        if (txt != null && !txt.trim().isEmpty()) {
            try {
                limite = new BigDecimal(txt.trim());
                if (limite.compareTo(BigDecimal.ZERO) < 0 || limite.compareTo(new BigDecimal("100")) > 0) throw new NumberFormatException();
            } catch (Exception e) { new Alert(Alert.AlertType.WARNING,"Tope descuento debe ser 0-100 o vacío (sin límite)").showAndWait(); return; }
        }
        List<String> codigos = filas.stream().filter(FilaPermiso::isAsignado).map(f->f.getPermiso().getCodigo()).collect(Collectors.toList());
        permisoDAO.actualizarPermisosDeRol(rol.getId(), codigos);
        rolDAO.actualizarLimiteDescuento(rol.getId(), limite);
        // auditoría
        try { new com.vendex.dao.AuditoriaAccionDAOPostgres().registrar(com.vendex.util.SesionActual.getUsuario().getId(),"USUARIO_GESTIONAR","PERMITIDO","Actualizar rol "+rol.getNombre()+" permisos="+codigos.size()+" tope="+limite); } catch(Exception ignored){}
        new Alert(Alert.AlertType.INFORMATION,"Rol actualizado. Los cambios aplican en el siguiente login de usuarios de ese rol.").showAndWait();
        cargarMatriz(rolDAO.obtenerPorId(rol.getId()));
    }

    public static class FilaPermiso {
        private final Permiso permiso;
        private final SimpleBooleanProperty asignado;
        public FilaPermiso(Permiso p, boolean asig){ this.permiso=p; this.asignado=new SimpleBooleanProperty(asig); }
        public Permiso getPermiso(){ return permiso; }
        public boolean isAsignado(){ return asignado.get(); }
        public void setAsignado(boolean v){ asignado.set(v); }
        public SimpleBooleanProperty asignadoProperty(){ return asignado; }
    }
}