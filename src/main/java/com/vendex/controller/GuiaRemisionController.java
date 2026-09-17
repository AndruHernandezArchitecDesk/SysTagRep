package com.vendex.controller;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.service.GuiaRemisionService;
import com.vendex.util.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class GuiaRemisionController implements Initializable {

    @FXML private TextField txtDirPartida, txtRazonTransportista, txtRucTransportista, txtPlaca;
    @FXML private ComboBox<String> cmbTipoIdTransportista, cmbAmbiente;
    @FXML private DatePicker dpFechaIni, dpFechaFin;
    @FXML private Label lblSecuencial;

    @FXML private TextField txtIdentDest, txtRazonDest, txtDirDest, txtMotivoTraslado, txtCodigoInterno, txtDescripcion, txtCantidad;
    @FXML private ComboBox<FacturaRegistro> cmbFacturaSustento;
    @FXML private TableView<GuiaRemisionService.DestinatarioGRInput> tblDestinatarios;
    @FXML private TableColumn<GuiaRemisionService.DestinatarioGRInput, String> colDestIdent, colDestRazon, colDestDir, colDestMotivo, colDestSustento;
    @FXML private TableView<GuiaRemisionService.DetalleGRInput> tblDetalles;
    @FXML private TableColumn<GuiaRemisionService.DetalleGRInput, String> colDetCodigo, colDetDesc, colDetCant;

    private final GuiaRemisionService grService = new GuiaRemisionService();
    private final SecuenciaDocumentoDAO secDAO = new SecuenciaDocumentoDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final LogDAO logDAO = new LogDAO();

    private final ObservableList<GuiaRemisionService.DestinatarioGRInput> destinatarios = FXCollections.observableArrayList();
    private final ObservableList<GuiaRemisionService.DetalleGRInput> detallesActual = FXCollections.observableArrayList();
    private GuiaRemisionService.DestinatarioGRInput destinatarioSeleccionado;
    private String rutaP12 = "", claveP12 = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ComboFilter.habilitar(cmbTipoIdTransportista, FXCollections.observableArrayList("04", "05", "06"));
        cmbTipoIdTransportista.setValue("04");
        ComboFilter.habilitar(cmbAmbiente, FXCollections.observableArrayList(AppConstants.AMBIENTE_PRUEBAS, AppConstants.AMBIENTE_PRODUCCION));
        cmbAmbiente.setValue(ConfigAmbiente.cargar());
        cmbAmbiente.valueProperty().addListener((o, old, v) -> ConfigAmbiente.guardar(v));
        String[] f = ConfigFirma.cargar(); rutaP12 = f[0]; claveP12 = f[1];
        // dirPartida autocompletada desde empresa
        try {
            Empresa emp = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
            if (emp != null) txtDirPartida.setText(emp.getDireccionCallePrincipal() + " y " + emp.getDireccionCalleSecundaria());
            else txtDirPartida.setText("");
        } catch (Exception e) { txtDirPartida.setText(""); }
        dpFechaIni.setValue(LocalDate.now());
        dpFechaFin.setValue(LocalDate.now());
        cargarFacturasSustento();
        iniciarTablas();
        actualizarSecuencial();
    }

    private void cargarFacturasSustento() {
        String sql = "SELECT fr.*, c.nombre as nombre_cliente FROM factura_registro fr LEFT JOIN cliente c ON c.id=fr.cliente_id LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=fr.clave_acceso WHERE COALESCE(ce.estado_sri, fr.estado_sri)='AUTORIZADO' ORDER BY fr.id DESC LIMIT 200";
        java.util.List<FacturaRegistro> facturas = new java.util.ArrayList<>();
        try (java.sql.Connection con = DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaRegistro fr = new FacturaRegistro();
                fr.setId(rs.getInt("id")); fr.setCodigo(rs.getString("codigo")); fr.setNumComprobante(rs.getString("num_comprobante"));
                fr.setClienteId(rs.getInt("cliente_id")); fr.setTotal(rs.getBigDecimal("total")); fr.setFecha(rs.getObject("fecha", LocalDateTime.class));
                fr.setClaveAcceso(rs.getString("clave_acceso"));
                try { fr.setNombreCliente(rs.getString("nombre_cliente")); } catch (Exception ignore) {}
                facturas.add(fr);
            }
        } catch (Exception e) { e.printStackTrace(); }
        // Agregamos opción vacía "Sin sustento"
        ComboFilter.habilitar(cmbFacturaSustento, FXCollections.observableArrayList(facturas), new StringConverter<>() {
            public String toString(FacturaRegistro f){ if (f==null) return "Sin documento sustento"; return f.getNumComprobante()+" | "+(f.getNombreCliente()!=null?f.getNombreCliente():"")+" | $"+f.getTotal(); }
            public FacturaRegistro fromString(String s){ return null; }
        });
        cmbFacturaSustento.setPromptText("Sin documento sustento");
    }

    private void iniciarTablas() {
        colDestIdent.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().identificacionDestinatario));
        colDestRazon.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().razonSocialDestinatario));
        colDestDir.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().direccionDestinatario));
        colDestMotivo.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().motivoTraslado));
        colDestSustento.setCellValueFactory(cd -> {
            String s = cd.getValue().facturaRegistroId != null ? "Factura" : "Sin sustento";
            return new ReadOnlyObjectWrapper<>(s);
        });
        tblDestinatarios.setItems(destinatarios);
        tblDestinatarios.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> onDestinatarioSeleccionado(sel));

        colDetCodigo.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().codigoInterno));
        colDetDesc.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().descripcion));
        colDetCant.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cantidad != null ? cd.getValue().cantidad.toPlainString() : ""));
        tblDetalles.setItems(detallesActual);
    }

    private void onDestinatarioSeleccionado(GuiaRemisionService.DestinatarioGRInput sel) {
        destinatarioSeleccionado = sel;
        detallesActual.clear();
        if (sel != null && sel.detalles != null) detallesActual.addAll(sel.detalles);
    }

    @FXML
    private void agregarDestinatario() {
        String ident = txtIdentDest.getText();
        String razon = txtRazonDest.getText();
        String dir = txtDirDest.getText();
        String motivo = txtMotivoTraslado.getText();
        if (ident==null||ident.trim().isEmpty()||razon==null||razon.trim().isEmpty()||dir==null||dir.trim().isEmpty()||motivo==null||motivo.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Complete identificación, razón social, dirección y motivo de traslado.").showAndWait(); return;
        }
        GuiaRemisionService.DestinatarioGRInput d = new GuiaRemisionService.DestinatarioGRInput(ident.trim(), razon.trim(), dir.trim(), motivo.trim());
        FacturaRegistro fr = cmbFacturaSustento.getValue();
        if (fr != null) d.facturaRegistroId = fr.getId();
        destinatarios.add(d);
        tblDestinatarios.getSelectionModel().select(d);
        tblDestinatarios.scrollTo(d);
        // onDestinatarioSeleccionado se dispara por listener, pero aseguramos
        onDestinatarioSeleccionado(d);
        txtCodigoInterno.requestFocus();
        txtIdentDest.clear(); txtRazonDest.clear(); txtDirDest.clear(); txtMotivoTraslado.clear(); cmbFacturaSustento.setValue(null);
    }

    @FXML
    private void quitarDestinatario() {
        GuiaRemisionService.DestinatarioGRInput sel = tblDestinatarios.getSelectionModel().getSelectedItem();
        if (sel != null) {
            destinatarios.remove(sel);
            if (sel == destinatarioSeleccionado) { destinatarioSeleccionado = null; detallesActual.clear(); }
        }
    }

    @FXML
    private void agregarDetalle() {
        if (destinatarioSeleccionado == null) { new Alert(Alert.AlertType.WARNING, "Seleccione un destinatario primero.").showAndWait(); return; }
        String cod = txtCodigoInterno.getText();
        String desc = txtDescripcion.getText();
        String cantStr = txtCantidad.getText();
        if (desc==null||desc.trim().isEmpty()) { new Alert(Alert.AlertType.WARNING, "Descripción obligatoria.").showAndWait(); return; }
        if (cantStr==null||cantStr.trim().isEmpty()) { new Alert(Alert.AlertType.WARNING, "Cantidad obligatoria.").showAndWait(); return; }
        try {
            BigDecimal cant = new BigDecimal(cantStr.trim());
            if (cant.compareTo(BigDecimal.ZERO) <= 0) { new Alert(Alert.AlertType.WARNING, "Cantidad > 0.").showAndWait(); return; }
            GuiaRemisionService.DetalleGRInput det = new GuiaRemisionService.DetalleGRInput(cod!=null&&!cod.trim().isEmpty()?cod.trim():"001", desc.trim(), cant);
            destinatarioSeleccionado.detalles.add(det);
            detallesActual.add(det);
            txtCodigoInterno.clear(); txtDescripcion.clear(); txtCantidad.clear();
        } catch (NumberFormatException e) { new Alert(Alert.AlertType.WARNING, "Cantidad inválida.").showAndWait(); }
    }

    @FXML
    private void quitarDetalle() {
        GuiaRemisionService.DetalleGRInput sel = tblDetalles.getSelectionModel().getSelectedItem();
        if (sel != null && destinatarioSeleccionado != null) {
            destinatarioSeleccionado.detalles.remove(sel);
            detallesActual.remove(sel);
        }
    }

    private void actualizarSecuencial() {
        try { lblSecuencial.setText(secDAO.obtener("GUIA_REMISION").getProximoCodigo()); } catch (Exception ignore) {}
    }

    @FXML
    private void emitirGuiaRemision() {
        try {
            if (destinatarios.isEmpty()) throw new IllegalArgumentException("Agregue al menos un destinatario.");
            for (var d : destinatarios) if (d.detalles == null || d.detalles.isEmpty()) throw new IllegalArgumentException("Destinatario "+d.razonSocialDestinatario+" sin ítems.");
            String dirPartida = txtDirPartida.getText();
            String razonTrans = txtRazonTransportista.getText();
            String rucTrans = txtRucTransportista.getText();
            String placa = txtPlaca.getText();
            String tipoIdTrans = cmbTipoIdTransportista.getValue();
            LocalDate ini = dpFechaIni.getValue();
            LocalDate fin = dpFechaFin.getValue();
            String ambiente = cmbAmbiente.getValue()!=null? cmbAmbiente.getValue(): AppConstants.AMBIENTE_PRUEBAS;
            File dir = obtenerDirEscritorio();
            Empresa emp = empresaDAO.listar().isEmpty()?null:empresaDAO.listar().get(0);
            if (emp==null) throw new IllegalStateException("Empresa no configurada");
            int usuarioId = LoginController.usuarioAutenticado!=null? LoginController.usuarioAutenticado.getId():1;

            // copia profunda para no perder referencia tras limpiar
            java.util.List<GuiaRemisionService.DestinatarioGRInput> copia = new java.util.ArrayList<>();
            for (GuiaRemisionService.DestinatarioGRInput d : destinatarios) {
                GuiaRemisionService.DestinatarioGRInput c = new GuiaRemisionService.DestinatarioGRInput(d.identificacionDestinatario, d.razonSocialDestinatario, d.direccionDestinatario, d.motivoTraslado);
                c.facturaRegistroId = d.facturaRegistroId;
                for (GuiaRemisionService.DetalleGRInput det : d.detalles) c.detalles.add(new GuiaRemisionService.DetalleGRInput(det.codigoInterno, det.descripcion, det.cantidad, det.inventarioId));
                copia.add(c);
            }

            GuiaRemisionService.ResultadoGuiaRemision res = grService.emitirGuiaRemision(copia, dirPartida, razonTrans, tipoIdTrans, rucTrans, placa, ini, fin, ambiente, rutaP12, claveP12, dir, usuarioId);

            Task<SRIWebService.SRIResponse> tarea = new Task<>(){ protected SRIWebService.SRIResponse call(){ return grService.enviarYSolicitarAutorizacion(ambiente, res.xmlFirmado, res.claveAcceso); }};
            Alert prog = new Alert(Alert.AlertType.INFORMATION); prog.setTitle("Guía de Remisión"); prog.setHeaderText("Consultando al SRI..."); prog.setContentText("Enviando GR "+res.numComprobante+" al SRI\nNo cierre la ventana."); prog.getButtonTypes().setAll(new ButtonType("Minimizar", ButtonBar.ButtonData.CANCEL_CLOSE));
            tarea.setOnSucceeded(e-> {
                SRIWebService.SRIResponse resp = tarea.getValue();
                new Thread(() -> {
                    try { grService.finalizarEnvioSRI(resp, res, dir); } catch (Exception ex) { logDAO.guardar("GuiaRemisionController","finalizarEnvioSRI", ex.getMessage(), ex); }
                    javafx.application.Platform.runLater(() -> {
                        prog.close();
                        String estado = resp.getEstado();
                        String msg = resp.getMensaje();
                        if (msg != null && msg.toLowerCase().contains("no cumple") || "DEVUELTA".equals(estado) || "RECHAZADA".equals(estado) || "NO AUTORIZADO".equals(estado)) {
                            logDAO.guardar("GuiaRemisionController","SRI-DEVUELTA", "GR "+res.numComprobante+" estado="+estado+" msg="+msg+" clave="+res.claveAcceso);
                        }
                        if (AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado) || "NO AUTORIZADO".equals(estado)) {
                            new Alert(Alert.AlertType.ERROR, "SRI "+estado+": "+msg+"\nClave: "+res.claveAcceso+"\nRevisa tabla logs y carpeta ~/vendex_errors").showAndWait();
                        } else {
                            new Alert(Alert.AlertType.INFORMATION, "GR "+res.numComprobante+" registrada. Estado: "+estado+(msg!=null&&!msg.isEmpty()?"\n"+msg:"")+"\nClave: "+res.claveAcceso+"\nPDF: "+res.rutaPDF).showAndWait();
                        }
                        new Thread(() -> { try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(res.rutaPDF)); } catch (Exception ignore) {} }, "Hilo-Abrir-PDF-GR").start();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                            // Intentar enviar correo al primer destinatario si tiene email registrado como cliente
                            try {
                                String ident = destinatarios.isEmpty() ? null : destinatarios.get(0).identificacionDestinatario;
                                String correo = null;
                                if (ident != null) {
                                    for (Cliente cli : new ClienteDAO().listar()) {
                                        if (ident.equals(cli.getIdentificacion()) && cli.getCorreo()!=null && cli.getCorreo().contains("@")) { correo = cli.getCorreo(); break; }
                                    }
                                }
                                if (correo!=null && ElectronicoUtil.debeEnviarNotificacion(estado, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion())) {
                                    String c = correo;
                                    String nombre = destinatarios.get(0).razonSocialDestinatario;
                                    Task<String> tMail=new Task<>(){ protected String call(){ return grService.enviarCorreoAutorizacion(c.trim(), nombre, res.numComprobante, res.rutaPDF, res.rutaXML)?null:"Error correo"; }};
                                    tMail.setOnSucceeded(ev-> { if (tMail.getValue()==null) new Alert(Alert.AlertType.INFORMATION, "Correo enviado a "+c).showAndWait(); else logDAO.guardar("GuiaRemisionController","correoGR", tMail.getValue()); });
                                    new Thread(tMail, "Hilo-Correo-GR").start();
                                }
                            } catch (Exception ignore) { logDAO.guardar("GuiaRemisionController","correoGR", ignore.getMessage(), ignore instanceof Exception ? (Exception)ignore : new Exception(ignore)); }
                        }
                        destinatarios.clear(); detallesActual.clear(); txtRazonTransportista.clear(); txtRucTransportista.clear(); txtPlaca.clear();
                        actualizarSecuencial();
                    });
                }, "Hilo-Finalizar-GR").start();
            });
            tarea.setOnFailed(e-> { prog.close(); Throwable ex = tarea.getException(); logDAO.guardar("GuiaRemisionController","SRI-Tarea", ex!=null?ex.getMessage():"desconocido", ex instanceof Exception ? (Exception)ex : new Exception(ex)); new Alert(Alert.AlertType.ERROR, "Error SRI: "+ (ex!=null?ex.getMessage():"desconocido")+"\nRevisa tabla logs y ~/vendex_errors").showAndWait(); });
            new Thread(tarea, "Hilo-SRI-GR").start(); prog.show();
        } catch (Exception ex) { logDAO.guardar("GuiaRemisionController","emitirGuiaRemision", ex.getMessage(), ex); new Alert(Alert.AlertType.ERROR, "Error: "+ex.getMessage()).showAndWait(); }
    }

    private File obtenerDirEscritorio(){ File h=new File(System.getProperty("user.home")); for(String n: new String[]{AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT, AppConstants.DIRECTORIO_ESCRITORIO_ALT}){ File d=new File(h,n); if(d.exists()&&d.isDirectory()) return d; } File d=new File(h, AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT); d.mkdirs(); return d; }
}
