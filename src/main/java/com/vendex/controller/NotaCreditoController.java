package com.vendex.controller;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.service.NotaCreditoService;
import com.vendex.util.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class NotaCreditoController implements Initializable {

    @FXML private ComboBox<FacturaRegistro> cmbFactura;
    @FXML private Label lblCliente, lblNumFactura, lblFechaFactura, lblTotalFactura, lblSaldoDisponible;
    @FXML private TableView<FacturaDetalle> tblDetallesOrigen;
    @FXML private TableColumn<FacturaDetalle, String> colCodigo, colDesc;
    @FXML private TableColumn<FacturaDetalle, Integer> colCantOrig;
    @FXML private TableColumn<FacturaDetalle, BigDecimal> colPrecio;
    @FXML private TableColumn<FacturaDetalle, Void> colAccion;

    @FXML private TableView<NotaCreditoService.DetalleNCInput> tblDetallesNc;
    @FXML private TableColumn<NotaCreditoService.DetalleNCInput, String> colNcCodigo, colNcDesc;
    @FXML private TableColumn<NotaCreditoService.DetalleNCInput, BigDecimal> colNcCant, colNcPrecio;
    @FXML private TableColumn<NotaCreditoService.DetalleNCInput, Void> colNcAccion;

    @FXML private TextArea txtMotivo;
    @FXML private ComboBox<String> cmbTipoMotivo;
    @FXML private CheckBox chkReingresaStock;
    @FXML private Label lblSubtotal, lblIva, lblTotal;
    @FXML private ComboBox<String> cmbAmbiente;
    @FXML private Label lblSecuencial;

    private final FacturaRegistroDAO facturaDAO = new FacturaRegistroDAOPostgres();
    private final FacturaDetalleDAO facturaDetalleDAO = new FacturaDetalleDAOPostgres();
    private final NotaCreditoRegistroDAO ncDAO = new NotaCreditoRegistroDAOPostgres();
    private final SecuenciaDocumentoDAO secDAO = new SecuenciaDocumentoDAOPostgres();
    private final EmpresaDAO empresaDAO = new EmpresaDAOPostgres();
    private final ClienteDAO clienteDAO = new ClienteDAOPostgres();
    private final NotaCreditoService ncService = new NotaCreditoService();
    private final LogDAO logDAO = new LogDAOPostgres();

    private final ObservableList<NotaCreditoService.DetalleNCInput> detallesNc = FXCollections.observableArrayList();
    private String rutaP12 = ""; private String claveP12 = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ComboFilter.habilitar(cmbTipoMotivo, FXCollections.observableArrayList("DEVOLUCION","DESCUENTO","ANULACION"));
        cmbTipoMotivo.setValue("DEVOLUCION");
        ComboFilter.habilitar(cmbAmbiente, FXCollections.observableArrayList(AppConstants.AMBIENTE_PRUEBAS, AppConstants.AMBIENTE_PRODUCCION));
        cmbAmbiente.setValue(ConfigAmbiente.cargar());
        cmbAmbiente.valueProperty().addListener((o,old,v)-> ConfigAmbiente.guardar(v));
        String[] f = ConfigFirma.cargar(); rutaP12=f[0]; claveP12=f[1];
        cargarFacturasAutorizadas();
        iniciarTablas();
        actualizarSecuencial();
    }

    private void cargarFacturasAutorizadas() {
        List<FacturaRegistro> facturas = new ArrayList<>();
        // listar facturas AUTORIZADAS (+ PENDIENTE en modo contingencia)
        boolean modoContingencia = com.vendex.util.SRIContingenciaConfig.isModoContingencia();
        String estadoFiltro = modoContingencia ? "COALESCE(ce.estado_sri, fr.estado_sri) IN ('AUTORIZADO','PENDIENTE','RECIBIDA','ERROR')" : "COALESCE(ce.estado_sri, fr.estado_sri)='AUTORIZADO'";
        String sql = "SELECT fr.*, c.nombre as nombre_cliente, COALESCE(ce.estado_sri, fr.estado_sri) as estado_sri_actual, ce.mensaje_sri FROM factura_registro fr LEFT JOIN cliente c ON c.id=fr.cliente_id LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=fr.clave_acceso WHERE " + estadoFiltro + " ORDER BY fr.id DESC LIMIT 200";
        try (java.sql.Connection con = DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaRegistro fr = new FacturaRegistro();
                fr.setId(rs.getInt("id")); fr.setCodigo(rs.getString("codigo")); fr.setNumComprobante(rs.getString("num_comprobante"));
                fr.setClienteId(rs.getInt("cliente_id")); fr.setTotal(rs.getBigDecimal("total")); fr.setFecha(rs.getObject("fecha", LocalDateTime.class));
                fr.setClaveAcceso(rs.getString("clave_acceso")); fr.setEstadoSri(rs.getString("estado_sri_actual"));
                try { fr.setNombreCliente(rs.getString("nombre_cliente")); } catch (Exception ignore) {}
                facturas.add(fr);
            }
        } catch (Exception e) { e.printStackTrace(); }
        ComboFilter.habilitar(cmbFactura, FXCollections.observableArrayList(facturas), new StringConverter<>() {
            public String toString(FacturaRegistro f){ return f==null?"": f.getNumComprobante()+" | "+ (f.getNombreCliente()!=null?f.getNombreCliente():"")+" | $"+f.getTotal(); }
            public FacturaRegistro fromString(String s){ return null; }
        });
        cmbFactura.valueProperty().addListener((o,old,sel)-> onFacturaSeleccionada(sel));
    }

    private void iniciarTablas() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantOrig.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colAccion.setCellFactory(c-> new TableCell<>(){ private final Button b=new Button("Agregar"); { b.setOnAction(e->{ var item=getTableView().getItems().get(getIndex()); agregarDetalleNc(item); }); } protected void updateItem(Void v, boolean empty){ super.updateItem(v,empty); setGraphic(empty?null:b); setAlignment(Pos.CENTER); } });
        colAccion.setCellValueFactory(p-> new ReadOnlyObjectWrapper<>(null));

        colNcCodigo.setCellValueFactory(cd-> new ReadOnlyObjectWrapper<>(cd.getValue().codigo));
        colNcDesc.setCellValueFactory(cd-> new ReadOnlyObjectWrapper<>(cd.getValue().descripcion));
        colNcCant.setCellValueFactory(cd-> new ReadOnlyObjectWrapper<>(cd.getValue().cantidad));
        colNcPrecio.setCellValueFactory(cd-> new ReadOnlyObjectWrapper<>(cd.getValue().precioUnitario));
        colNcAccion.setCellFactory(c-> new TableCell<>(){ private final Button b=new Button("Quitar"); { b.setStyle("-fx-text-fill:red"); b.setOnAction(e->{ detallesNc.remove(getTableView().getItems().get(getIndex())); calcularTotales(); }); } protected void updateItem(Void v, boolean empty){ super.updateItem(v,empty); setGraphic(empty?null:b); setAlignment(Pos.CENTER); } });
        colNcAccion.setCellValueFactory(p-> new ReadOnlyObjectWrapper<>(null));
        tblDetallesNc.setItems(detallesNc);
    }

    private void onFacturaSeleccionada(FacturaRegistro fr) {
        detallesNc.clear();
        if (fr==null) { tblDetallesOrigen.setItems(FXCollections.observableArrayList()); lblCliente.setText(""); lblNumFactura.setText(""); lblTotalFactura.setText(""); lblSaldoDisponible.setText(""); calcularTotales(); return; }
        List<FacturaDetalle> dets = facturaDetalleDAO.listarPorFacturaRegistroId(fr.getId());
        tblDetallesOrigen.setItems(FXCollections.observableArrayList(dets));
        Cliente cli = clienteDAO.obtenerPorId(fr.getClienteId());
        lblCliente.setText(cli!=null? cli.getNombre()+" ("+cli.getIdentificacion()+")":"");
        lblNumFactura.setText(fr.getNumComprobante()); lblFechaFactura.setText(fr.getFecha()!=null? fr.getFecha().toLocalDate().toString():"");
        lblTotalFactura.setText(fr.getTotal()!=null? "$ "+fr.getTotal():"");
        BigDecimal acreditado = ncDAO.sumarValorModificacionPorFactura(fr.getId(), AppConstants.ESTADO_AUTORIZADO);
        BigDecimal saldo = fr.getTotal()!=null? fr.getTotal().subtract(acreditado): BigDecimal.ZERO;
        lblSaldoDisponible.setText("$ "+saldo.setScale(2, RoundingMode.HALF_UP));
        calcularTotales();
    }

    private void agregarDetalleNc(FacturaDetalle origen) {
        TextInputDialog dlg = new TextInputDialog(String.valueOf(origen.getCantidad()));
        dlg.setTitle("Cantidad a acreditar"); dlg.setHeaderText(origen.getDescripcion()); dlg.setContentText("Cantidad (max "+origen.getCantidad()+"):");
        dlg.showAndWait().ifPresent(val-> {
            try {
                BigDecimal cant = new BigDecimal(val.trim());
                if (cant.compareTo(BigDecimal.ZERO)<=0 || cant.compareTo(new BigDecimal(origen.getCantidad()))>0) { new Alert(Alert.AlertType.WARNING, "Cantidad invalida.").showAndWait(); return; }
                NotaCreditoService.DetalleNCInput in = new NotaCreditoService.DetalleNCInput(origen.getInventarioId(), origen.getId(), origen.getCodigo(), origen.getDescripcion(), cant, origen.getPrecioUnitario());
                detallesNc.add(in);
                calcularTotales();
            } catch (NumberFormatException ex) { new Alert(Alert.AlertType.WARNING, "Cantidad invalida").showAndWait(); }
        });
    }

    private void calcularTotales() {
        BigDecimal sub = BigDecimal.ZERO;
        for (var d: detallesNc) {
            BigDecimal precioSinIva = d.precioUnitario.divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            sub = sub.add(precioSinIva.multiply(d.cantidad).setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal iva = sub.multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tot = sub.add(iva).setScale(2, RoundingMode.HALF_UP);
        lblSubtotal.setText("$ "+sub.toPlainString());
        lblIva.setText("$ "+iva.toPlainString());
        lblTotal.setText("$ "+tot.toPlainString());
    }

    private void actualizarSecuencial() {
        try { lblSecuencial.setText(secDAO.obtener("NOTA_CREDITO").getProximoCodigo()); } catch (Exception ignore) {}
    }

    @FXML private void emitirNotaCredito() {
        try {
            FacturaRegistro fr = cmbFactura.getValue();
            if (fr==null) throw new IllegalArgumentException("Seleccione una factura autorizada.");
            if (detallesNc.isEmpty()) throw new IllegalArgumentException("Agregue al menos un item a acreditar.");
            String motivo = txtMotivo.getText(); if (motivo==null||motivo.trim().isEmpty()) throw new IllegalArgumentException("Motivo obligatorio (max 300).");
            String tipoMotivo = cmbTipoMotivo.getValue(); boolean reingresa = chkReingresaStock.isSelected();
            String ambiente = cmbAmbiente.getValue()!=null? cmbAmbiente.getValue(): AppConstants.AMBIENTE_PRUEBAS;
            File dir = obtenerDirEscritorio();
            Empresa emp = empresaDAO.listar().isEmpty()?null:empresaDAO.listar().get(0);
            if (emp==null) throw new IllegalStateException("Empresa no configurada");
            int usuarioId = LoginController.usuarioAutenticado!=null? LoginController.usuarioAutenticado.getId():1;

            // copia
            List<NotaCreditoService.DetalleNCInput> copia = new ArrayList<>(detallesNc);
            NotaCreditoService.ResultadoNotaCredito res = ncService.emitirNotaCredito(fr.getId(), copia, motivo.trim(), tipoMotivo, reingresa, ambiente, rutaP12, claveP12, dir, usuarioId);

            // Contingencia todo a cola: provisional PENDIENTE + encolar
            SRIWebService.SRIResponse provisional = new SRIWebService.SRIResponse();
            provisional.setEstado(AppConstants.ESTADO_PENDIENTE);
            provisional.setMensaje("Pendiente SRI - en cola contingencia");
            try { ncService.finalizarEnvioSRI(provisional, res, dir); } catch (Exception ex) { logDAO.guardar("NotaCreditoController","provisional", ex.getMessage(), ex); }
            try { new com.vendex.dao.ComprobantePendienteSriDAOPostgres().encolar("NOTA_CREDITO", res.claveAcceso, res.numComprobante, ambiente); } catch (Exception ex) { logDAO.guardar("NotaCreditoController","encolar", ex.getMessage(), ex instanceof Exception ? (Exception)ex : new Exception(ex)); }
            new Alert(Alert.AlertType.INFORMATION, "NC " + res.numComprobante + " registrada (pendiente SRI).\nClave: " + res.claveAcceso + "\nPDF provisional: " + res.rutaPDF + "\nEn cola cada 2min. Banner mostrara pendientes.").showAndWait();
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(res.rutaPDF)); } catch (Exception ignore) {}
            detallesNc.clear(); txtMotivo.clear(); cargarFacturasAutorizadas(); actualizarSecuencial(); calcularTotales();
        } catch (Exception ex) { logDAO.guardar("NotaCreditoController","emitirNotaCredito", ex.getMessage(), ex); new Alert(Alert.AlertType.ERROR, "Error: "+ex.getMessage()).showAndWait(); }
    }

    private File obtenerDirEscritorio(){ File h=new File(System.getProperty("user.home")); for(String n: new String[]{AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT, AppConstants.DIRECTORIO_ESCRITORIO_ALT}){ File d=new File(h,n); if(d.exists()&&d.isDirectory()) return d; } File d=new File(h, AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT); d.mkdirs(); return d; }
}
