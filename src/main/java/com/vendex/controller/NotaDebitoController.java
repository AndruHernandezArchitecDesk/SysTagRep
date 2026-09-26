package com.vendex.controller;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.service.NotaDebitoService;
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

public class NotaDebitoController implements Initializable {

    @FXML private ComboBox<FacturaRegistro> cmbFactura;
    @FXML private Label lblCliente, lblNumFactura, lblFechaFactura, lblTotalFactura;
    @FXML private TableView<NotaDebitoService.MotivoNDInput> tblMotivos;
    @FXML private TableColumn<NotaDebitoService.MotivoNDInput, String> colRazon, colValor;
    @FXML private TextField txtRazon, txtValor;
    @FXML private ComboBox<String> cmbFormaPago, cmbAmbiente;
    @FXML private Label lblSubtotal, lblIva, lblTotal, lblSecuencial;

    private final FacturaRegistroDAO facturaDAO = new FacturaRegistroDAOPostgres();
    private final NotaDebitoService ndService = new NotaDebitoService();
    private final SecuenciaDocumentoDAO secDAO = new SecuenciaDocumentoDAOPostgres();
    private final EmpresaDAO empresaDAO = new EmpresaDAOPostgres();
    private final LogDAO logDAO = new LogDAO();

    private final ObservableList<NotaDebitoService.MotivoNDInput> motivos = FXCollections.observableArrayList();
    private String rutaP12 = ""; private String claveP12 = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ComboFilter.habilitar(cmbFormaPago, FXCollections.observableArrayList("01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20"));
        cmbFormaPago.setValue("01");
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
        boolean modoContingencia = com.vendex.util.SRIContingenciaConfig.isModoContingencia();
        String filtro = modoContingencia ? "COALESCE(ce.estado_sri, fr.estado_sri) IN ('AUTORIZADO','PENDIENTE','RECIBIDA','ERROR')" : "COALESCE(ce.estado_sri, fr.estado_sri)='AUTORIZADO'";
        String sql = "SELECT fr.*, c.nombre as nombre_cliente, COALESCE(ce.estado_sri, fr.estado_sri) as estado_sri_actual, ce.mensaje_sri FROM factura_registro fr LEFT JOIN cliente c ON c.id=fr.cliente_id LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=fr.clave_acceso WHERE " + filtro + " ORDER BY fr.id DESC LIMIT 200";
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
        colRazon.setCellValueFactory(cd-> new ReadOnlyObjectWrapper<>(cd.getValue().razon));
        colValor.setCellValueFactory(cd-> new ReadOnlyObjectWrapper<>(cd.getValue().valor != null ? cd.getValue().valor.toPlainString() : ""));
        tblMotivos.setItems(motivos);
    }

    private void onFacturaSeleccionada(FacturaRegistro fr) {
        if (fr==null) { lblCliente.setText(""); lblNumFactura.setText(""); lblFechaFactura.setText(""); lblTotalFactura.setText(""); calcularTotales(); return; }
        Cliente cli = new ClienteDAOPostgres().obtenerPorId(fr.getClienteId());
        lblCliente.setText(cli!=null? cli.getNombre()+" ("+cli.getIdentificacion()+")":"");
        lblNumFactura.setText(fr.getNumComprobante());
        lblFechaFactura.setText(fr.getFecha()!=null? fr.getFecha().toLocalDate().toString():"");
        lblTotalFactura.setText(fr.getTotal()!=null? "$ "+fr.getTotal():"");
        calcularTotales();
    }

    @FXML
    private void agregarMotivo() {
        String razon = txtRazon.getText();
        if (razon==null||razon.trim().isEmpty()) { new Alert(Alert.AlertType.WARNING, "La razón es obligatoria.").showAndWait(); return; }
        try {
            BigDecimal valor = new BigDecimal(txtValor.getText().trim());
            if (valor.compareTo(BigDecimal.ZERO)<=0) { new Alert(Alert.AlertType.WARNING, "Valor debe ser > 0.").showAndWait(); return; }
            motivos.add(new NotaDebitoService.MotivoNDInput(razon.trim(), valor));
            txtRazon.clear(); txtValor.clear();
            calcularTotales();
        } catch (NumberFormatException ex) { new Alert(Alert.AlertType.WARNING, "Valor numérico inválido.").showAndWait(); }
    }

    private void calcularTotales() {
        BigDecimal sub = BigDecimal.ZERO;
        for (var d: motivos) {
            if (d.gravaIva) {
                BigDecimal sinIva = d.valor.divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
                sub = sub.add(sinIva);
            } else {
                sub = sub.add(d.valor);
            }
        }
        BigDecimal iva = sub.multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tot = sub.add(iva).setScale(2, RoundingMode.HALF_UP);
        lblSubtotal.setText("$ "+sub.toPlainString());
        lblIva.setText("$ "+iva.toPlainString());
        lblTotal.setText("$ "+tot.toPlainString());
    }

    private void actualizarSecuencial() {
        try { lblSecuencial.setText(secDAO.obtener("NOTA_DEBITO").getProximoCodigo()); } catch (Exception ignore) {}
    }

    @FXML
    private void emitirNotaDebito() {
        try {
            FacturaRegistro fr = cmbFactura.getValue();
            if (fr==null) throw new IllegalArgumentException("Seleccione una factura autorizada.");
            if (motivos.isEmpty()) throw new IllegalArgumentException("Agregue al menos un motivo.");
            String formaPago = cmbFormaPago.getValue();
            if (formaPago==null||formaPago.trim().isEmpty()) throw new IllegalArgumentException("Seleccione una forma de pago.");
            String ambiente = cmbAmbiente.getValue()!=null? cmbAmbiente.getValue(): AppConstants.AMBIENTE_PRUEBAS;
            File dir = obtenerDirEscritorio();
            Empresa emp = empresaDAO.listar().isEmpty()?null:empresaDAO.listar().get(0);
            if (emp==null) throw new IllegalStateException("Empresa no configurada");
            int usuarioId = LoginController.usuarioAutenticado!=null? LoginController.usuarioAutenticado.getId():1;

            List<NotaDebitoService.MotivoNDInput> copia = new ArrayList<>(motivos);
            NotaDebitoService.ResultadoNotaDebito res = ndService.emitirNotaDebito(fr.getId(), copia, formaPago, ambiente, rutaP12, claveP12, dir, usuarioId);

            // Contingencia todo a cola: provisional + encolar
            SRIWebService.SRIResponse provisional = new SRIWebService.SRIResponse();
            provisional.setEstado(AppConstants.ESTADO_PENDIENTE);
            provisional.setMensaje("Pendiente SRI - en cola");
            try { ndService.finalizarEnvioSRI(provisional, res, dir); } catch (Exception ex) { logDAO.guardar("NotaDebitoController","provisional", ex.getMessage(), ex); }
            try { new com.vendex.dao.ComprobantePendienteSriDAO().encolar("NOTA_DEBITO", res.claveAcceso, res.numComprobante, ambiente); } catch (Exception ex) { logDAO.guardar("NotaDebitoController","encolar", ex.getMessage(), ex instanceof Exception ? (Exception)ex : new Exception(ex)); }
            new Alert(Alert.AlertType.INFORMATION, "ND " + res.numComprobante + " registrada (pendiente SRI).\nClave: " + res.claveAcceso + "\nPDF provisional: " + res.rutaPDF).showAndWait();
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(res.rutaPDF)); } catch (Exception ignore) {}
            motivos.clear(); txtRazon.clear(); txtValor.clear(); cargarFacturasAutorizadas(); actualizarSecuencial(); calcularTotales();
        } catch (Exception ex) { logDAO.guardar("NotaDebitoController","emitirNotaDebito", ex.getMessage(), ex); new Alert(Alert.AlertType.ERROR, "Error: "+ex.getMessage()).showAndWait(); }
    }

    private File obtenerDirEscritorio(){ File h=new File(System.getProperty("user.home")); for(String n: new String[]{AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT, AppConstants.DIRECTORIO_ESCRITORIO_ALT}){ File d=new File(h,n); if(d.exists()&&d.isDirectory()) return d; } File d=new File(h, AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT); d.mkdirs(); return d; }
}
