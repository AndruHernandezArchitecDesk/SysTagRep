package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CodigoDAO;
import com.tag.sysTagRep.dao.GrupoDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.MarcaDAO;
import com.tag.sysTagRep.model.Codigo;
import com.tag.sysTagRep.model.Grupo;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.Marca;
import com.tag.sysTagRep.util.ComboFilter;
import com.tag.sysTagRep.util.EtiquetaUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Ingreso por producto: ligero, sin factura.
 * Solo pide descripcion, grupo, marca, codigo, cantidad, precio venta unit.
 * Sin fecha ni forma de pago (fecha=now, formaPago=NULL).
 * FKs nulleables (0->NULL) para no dar error: proveedor_id, grupo_id, marca_id.
 */
public class IngresoProductoController implements Initializable {

    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<Grupo> cmbGrupo;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private ComboBox<Codigo> cmbCodigo;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private TextField txtPrecioVenta;

    private final InventarioDAO dao = new InventarioDAO();
    private final GrupoDAO grupoDAO = new GrupoDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final CodigoDAO codigoDAO = new CodigoDAO();
    private final LogDAO logDAO = new LogDAO();

    private ObservableList<Grupo> listaGrupos = FXCollections.observableArrayList();
    private ObservableList<Marca> listaMarcas = FXCollections.observableArrayList();
    private ObservableList<Codigo> listaCodigos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        iniciarSpCantidad();
        iniciarCbGrupos();
        iniciarCbMarcas();
        iniciarCbCodigos();
        validarSoloNumeros();
        txtDescripcion.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.equals(val.toUpperCase())) txtDescripcion.setText(val.toUpperCase());
        });
    }

    private void iniciarSpCantidad() {
        spCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99999, 1));
        spCantidad.setEditable(true);
    }

    private void iniciarCbGrupos() {
        listaGrupos.setAll(grupoDAO.listar());
        ComboFilter.habilitar(cmbGrupo, listaGrupos, new StringConverter<>() {
            @Override public String toString(Grupo g) { return g == null ? "" : g.getNombre(); }
            @Override public Grupo fromString(String s) { return null; }
        });
    }

    private void iniciarCbMarcas() {
        listaMarcas.setAll(marcaDAO.listar());
        ComboFilter.habilitar(cmbMarca, listaMarcas, new StringConverter<>() {
            @Override public String toString(Marca m) { return m == null ? "" : m.getNombre(); }
            @Override public Marca fromString(String s) { return null; }
        });
    }

    private void iniciarCbCodigos() {
        listaCodigos.setAll(codigoDAO.listar());
        ComboFilter.habilitar(cmbCodigo, listaCodigos, new StringConverter<>() {
            @Override public String toString(Codigo c) { return c == null ? "" : c.getNombre(); }
            @Override public Codigo fromString(String s) { return null; }
        });
    }

    private String codigoSeleccionado() {
        Codigo c = cmbCodigo.getValue();
        if (c != null) return c.getNombre();
        String texto = cmbCodigo.getEditor().getText();
        return texto != null ? texto.trim().toUpperCase() : "";
    }

    private void registrarCodigoNuevo(String codigo) {
        if (codigo == null || codigo.isEmpty()) return;
        if (listaCodigos.stream().noneMatch(c -> c.getNombre().equalsIgnoreCase(codigo)) && !codigoDAO.existe(codigo)) {
            Codigo nuevo = new Codigo();
            nuevo.setNombre(codigo);
            nuevo.setEstado(true);
            try { codigoDAO.guardar(nuevo); } catch (Exception e) { logDAO.guardar("IngresoProductoController","registrarCodigoNuevo",e.getMessage(),e); }
            listaCodigos.setAll(codigoDAO.listar());
        }
    }

    private void validarSoloNumeros() {
        txtPrecioVenta.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty() && !val.matches("\\d*(\\.\\d{0,4})?")) txtPrecioVenta.setText(old);
        });
    }

    @FXML private void irAGrupo() { abrirModal("/view/GrupoView.fxml","Gestión de Grupos",700,500); }
    @FXML private void irAMarca() { abrirModal("/view/MarcaView.fxml","Gestión de Marcas",700,500); }
    @FXML private void irACodigo() { abrirModal("/view/CodigoView.fxml","Gestión de Códigos",700,500); }

    private void abrirModal(String fxml, String titulo, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(titulo);
            modal.setScene(new Scene(vista, w, h));
            modal.showAndWait();
            listaGrupos.setAll(grupoDAO.listar());
            listaMarcas.setAll(marcaDAO.listar());
            listaCodigos.setAll(codigoDAO.listar());
        } catch (Exception e) { logDAO.guardar("IngresoProductoController","abrirModal",e.getMessage(),e); }
    }

    @FXML
    private void guardar() {
        try {
            String desc = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
            if (desc.isEmpty()) { new Alert(Alert.AlertType.WARNING,"La descripción es obligatoria.").showAndWait(); return; }
            int cantidad = spCantidad.getValue() != null ? spCantidad.getValue() : 0;
            if (cantidad <= 0) { new Alert(Alert.AlertType.WARNING,"La cantidad debe ser mayor a cero.").showAndWait(); return; }
            String sPrecio = txtPrecioVenta.getText() != null ? txtPrecioVenta.getText().replace(",",".").trim() : "";
            if (sPrecio.isEmpty()) { new Alert(Alert.AlertType.WARNING,"El precio de venta es obligatorio.").showAndWait(); return; }
            BigDecimal precioVenta = new BigDecimal(sPrecio).setScale(2, RoundingMode.HALF_UP);
            if (precioVenta.signum() <= 0) { new Alert(Alert.AlertType.WARNING,"El precio debe ser mayor a cero.").showAndWait(); return; }

            String codigoManual = codigoSeleccionado();
            registrarCodigoNuevo(codigoManual);

            Grupo g = cmbGrupo.getValue();
            Marca m = cmbMarca.getValue();
            // costo_sin_iva obligatorio en BD: derivar de precioVenta (precio /1.15) para no pedir campo extra
            BigDecimal costoSinIva = precioVenta.divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            if (costoSinIva.signum() <= 0) costoSinIva = precioVenta;

            String tagCodigo = "";
            try { tagCodigo = EtiquetaUtil.cifrarPrecio(precioVenta.toPlainString()); } catch (Exception ignored) {}

            Inventario inv = new Inventario();
            inv.setDescripcion(desc);
            inv.setGrupoId(g != null ? g.getId() : 0);
            inv.setMarcaId(m != null ? m.getId() : 0);
            inv.setCodigo(codigoManual != null && !codigoManual.isEmpty() ? codigoManual : null);
            inv.setTagCodigo(tagCodigo != null && !tagCodigo.isEmpty() ? tagCodigo : null);
            inv.setCantidad(cantidad);
            inv.setPrecioVenta(precioVenta);
            inv.setCostoSinIVA(costoSinIva);
            inv.setFecha_ingreso(LocalDateTime.now());
            inv.setEstado(true);
            // FK-safe: proveedor null, ubicacion null, factura null, forma pago null
            inv.setProveedorId(0);
            inv.setUbicacionPerchaId(0);
            inv.setNumeroFactura(null);
            inv.setFormaPago(null);
            inv.setMesesPlazo(0);
            inv.setInteres(BigDecimal.ZERO);

            int id = dao.guardar(inv);
            if (id <= 0) { new Alert(Alert.AlertType.ERROR,"No se pudo guardar el producto.").showAndWait(); return; }

            new Alert(Alert.AlertType.INFORMATION,"Producto ingresado correctamente (ID "+id+").").showAndWait();
            cerrarVentana();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR,"Precio inválido. Use formato 0.00").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("IngresoProductoController","guardar",e.getMessage(),e);
            new Alert(Alert.AlertType.ERROR,"Error al guardar: "+e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancelar() { cerrarVentana(); }

    private void cerrarVentana() {
        Stage stage = (Stage) txtDescripcion.getScene().getWindow();
        stage.close();
    }
}
