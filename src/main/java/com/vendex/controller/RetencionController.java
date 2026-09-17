package com.vendex.controller;

import com.vendex.dao.*;
import com.vendex.model.Empresa;
import com.vendex.model.Proveedor;
import com.vendex.service.RetencionService;
import com.vendex.util.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class RetencionController implements Initializable {

    @FXML private TextField txtRazonSujeto, txtIdentSujeto, txtPeriodoFiscal, txtNumDocSustento, txtTotalSinImpuestos, txtBase, txtPorcentaje, txtValor;
    @FXML private ComboBox<String> cmbTipoIdSujeto, cmbAmbiente, cmbCodSustento, cmbCodDocSustento, cmbCodigo, cmbCodigoRetencion;
    @FXML private DatePicker dpFechaDocSustento;
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private Label lblSecuencial;
    @FXML private TableView<RetencionService.DocSustentoInput> tblDocs;
    @FXML private TableColumn<RetencionService.DocSustentoInput, String> colDocNum, colDocFecha, colDocTotal, colDocRetCount;
    @FXML private TableView<RetencionService.RetencionLineaInput> tblRetenciones;
    @FXML private TableColumn<RetencionService.RetencionLineaInput, String> colRetCodigo, colRetCodigoRet, colRetBase, colRetPorc, colRetValor;

    private final RetencionService retService = new RetencionService();
    private final SecuenciaDocumentoDAO secDAO = new SecuenciaDocumentoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final LogDAO logDAO = new LogDAO();
    private final TablaRetencionDAO tablaDAO = new TablaRetencionDAO();

    private final ObservableList<RetencionService.DocSustentoInput> docs = FXCollections.observableArrayList();
    private final ObservableList<RetencionService.RetencionLineaInput> retsActual = FXCollections.observableArrayList();
    private RetencionService.DocSustentoInput docSeleccionado;
    private String rutaP12 = "", claveP12 = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ComboFilter.habilitar(cmbTipoIdSujeto, FXCollections.observableArrayList("04", "05", "06"));
        cmbTipoIdSujeto.setValue("04");
        ComboFilter.habilitar(cmbAmbiente, FXCollections.observableArrayList(AppConstants.AMBIENTE_PRUEBAS, AppConstants.AMBIENTE_PRODUCCION));
        cmbAmbiente.setValue(ConfigAmbiente.cargar());
        cmbAmbiente.valueProperty().addListener((o, old, v) -> ConfigAmbiente.guardar(v));
        String[] f = ConfigFirma.cargar(); rutaP12 = f[0]; claveP12 = f[1];
        ComboFilter.habilitar(cmbCodSustento, FXCollections.observableArrayList("01", "02", "03"));
        cmbCodSustento.setValue("01");
        ComboFilter.habilitar(cmbCodDocSustento, FXCollections.observableArrayList("01", "04", "05"));
        cmbCodDocSustento.setValue("01");
        ComboFilter.habilitar(cmbCodigo, FXCollections.observableArrayList("1", "2"));
        cmbCodigo.setValue("1");
        // tabla retenciones códigos desde tabla_retencion
        try {
            var vigentes = tablaDAO.listarVigentes();
            ObservableList<String> codigos = FXCollections.observableArrayList();
            for (var t : vigentes) codigos.add(t.getCodigoRetencion() + " - " + t.getDescripcion() + " (" + t.getPorcentaje() + "%)");
            // simplificado: solo códigos
            ObservableList<String> soloCodigos = FXCollections.observableArrayList();
            for (var t : vigentes) soloCodigos.add(t.getCodigoRetencion());
            ComboFilter.habilitar(cmbCodigoRetencion, soloCodigos);
            cmbCodigoRetencion.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
                if (sel != null) {
                    var porc = tablaDAO.obtenerPorcentajeVigente(sel);
                    if (porc != null) txtPorcentaje.setText(porc.toPlainString());
                    // autocompletar codigo tipo según tabla
                    for (var t : vigentes) if (t.getCodigoRetencion().equals(sel)) { cmbCodigo.setValue(t.getTipo()); break; }
                }
            });
        } catch (Exception e) { ComboFilter.habilitar(cmbCodigoRetencion, FXCollections.observableArrayList("303","312","725","723","322")); }

        // periodo fiscal por defecto MM/YYYY actual
        txtPeriodoFiscal.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));
        dpFechaDocSustento.setValue(LocalDate.now());

        cargarProveedores();
        iniciarTablas();
        actualizarSecuencial();
        // autocompletar sujeto desde proveedor
        cmbProveedor.valueProperty().addListener((o, old, sel) -> {
            if (sel != null) {
                txtRazonSujeto.setText(sel.getNombre());
                txtIdentSujeto.setText(sel.getIdentificacion());
                String id = sel.getIdentificacion();
                if (id != null && id.length() == 13) cmbTipoIdSujeto.setValue("04");
                else if (id != null && id.length() == 10) cmbTipoIdSujeto.setValue("05");
            }
        });
        txtBase.textProperty().addListener((o, old, v) -> recalcularValor());
        txtPorcentaje.textProperty().addListener((o, old, v) -> recalcularValor());
    }

    private void cargarProveedores() {
        try {
            var provs = proveedorDAO.listar();
            ComboFilter.habilitar(cmbProveedor, FXCollections.observableArrayList(provs), new StringConverter<>() {
                public String toString(Proveedor p){ return p==null?"": p.getNombre() + " (" + p.getIdentificacion() + ")"; }
                public Proveedor fromString(String s){ return null; }
            });
            cmbProveedor.setPromptText("Seleccione proveedor (opcional) o llene manual");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void iniciarTablas() {
        colDocNum.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().numDocSustento));
        colDocFecha.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().fechaEmisionDocSustento != null ? cd.getValue().fechaEmisionDocSustento.toString() : ""));
        colDocTotal.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().totalSinImpuestos != null ? cd.getValue().totalSinImpuestos.toPlainString() : ""));
        colDocRetCount.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(String.valueOf(cd.getValue().retenciones.size())));
        tblDocs.setItems(docs);
        tblDocs.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> onDocSeleccionado(sel));

        colRetCodigo.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().codigo));
        colRetCodigoRet.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().codigoRetencion));
        colRetBase.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().baseImponible.toPlainString()));
        colRetPorc.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().porcentajeRetener.toPlainString()));
        colRetValor.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().valorRetenido.toPlainString()));
        tblRetenciones.setItems(retsActual);
    }

    private void onDocSeleccionado(RetencionService.DocSustentoInput sel) {
        docSeleccionado = sel;
        retsActual.clear();
        if (sel != null) retsActual.addAll(sel.retenciones);
    }

    private void recalcularValor() {
        try {
            BigDecimal base = new BigDecimal(txtBase.getText().trim());
            BigDecimal porc = new BigDecimal(txtPorcentaje.getText().trim());
            BigDecimal val = base.multiply(porc).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            txtValor.setText(val.toPlainString());
        } catch (Exception e) { /* ignore */ }
    }

    @FXML
    private void agregarDocSustento() {
        String num = txtNumDocSustento.getText();
        LocalDate fecha = dpFechaDocSustento.getValue();
        String totalStr = txtTotalSinImpuestos.getText();
        if (num==null||num.trim().isEmpty()||fecha==null||totalStr==null||totalStr.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Complete número, fecha y total del documento sustento.").showAndWait(); return;
        }
        try {
            BigDecimal total = new BigDecimal(totalStr.trim());
            if (total.compareTo(BigDecimal.ZERO) <= 0) { new Alert(Alert.AlertType.WARNING, "Total > 0.").showAndWait(); return; }
            RetencionService.DocSustentoInput d = new RetencionService.DocSustentoInput(num.trim(), fecha, total);
            d.codSustento = cmbCodSustento.getValue()!=null?cmbCodSustento.getValue():"01";
            d.codDocSustento = cmbCodDocSustento.getValue()!=null?cmbCodDocSustento.getValue():"01";
            docs.add(d);
            tblDocs.getSelectionModel().select(d);
            onDocSeleccionado(d);
            txtNumDocSustento.clear(); txtTotalSinImpuestos.clear();
        } catch (NumberFormatException e) { new Alert(Alert.AlertType.WARNING, "Total inválido.").showAndWait(); }
    }

    @FXML
    private void quitarDocSustento() {
        var sel = tblDocs.getSelectionModel().getSelectedItem();
        if (sel != null) {
            docs.remove(sel);
            if (sel == docSeleccionado) { docSeleccionado = null; retsActual.clear(); }
        }
    }

    @FXML
    private void agregarRetencion() {
        if (docSeleccionado == null) { new Alert(Alert.AlertType.WARNING, "Seleccione un documento sustento primero.").showAndWait(); return; }
        String cod = cmbCodigo.getValue();
        String codRet = cmbCodigoRetencion.getValue();
        String baseStr = txtBase.getText();
        String porcStr = txtPorcentaje.getText();
        if (cod==null||codRet==null||baseStr==null||porcStr==null||baseStr.trim().isEmpty()||porcStr.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Complete código, código retención, base y porcentaje.").showAndWait(); return;
        }
        try {
            BigDecimal base = new BigDecimal(baseStr.trim());
            BigDecimal porc = new BigDecimal(porcStr.trim());
            if (base.compareTo(BigDecimal.ZERO)<=0 || porc.compareTo(BigDecimal.ZERO)<=0) { new Alert(Alert.AlertType.WARNING, "Base y porcentaje > 0.").showAndWait(); return; }
            RetencionService.RetencionLineaInput linea = new RetencionService.RetencionLineaInput(cod, codRet, base, porc);
            // validar que no exceda total del documento? opcional
            docSeleccionado.retenciones.add(linea);
            retsActual.add(linea);
            txtBase.clear(); txtPorcentaje.clear(); txtValor.clear(); cmbCodigoRetencion.setValue(null);
        } catch (NumberFormatException e) { new Alert(Alert.AlertType.WARNING, "Base/porcentaje inválidos.").showAndWait(); }
    }

    @FXML
    private void quitarRetencion() {
        var sel = tblRetenciones.getSelectionModel().getSelectedItem();
        if (sel != null && docSeleccionado != null) {
            docSeleccionado.retenciones.remove(sel);
            retsActual.remove(sel);
        }
    }

    private void actualizarSecuencial() {
        try { lblSecuencial.setText(secDAO.obtener("RETENCION").getProximoCodigo()); } catch (Exception ignore) {}
    }

    @FXML
    private void emitirRetencion() {
        try {
            if (docs.isEmpty()) throw new IllegalArgumentException("Agregue al menos un documento sustento.");
            for (var d : docs) if (d.retenciones.isEmpty()) throw new IllegalArgumentException("Documento "+d.numDocSustento+" sin retenciones.");
            String periodo = txtPeriodoFiscal.getText();
            String tipoId = cmbTipoIdSujeto.getValue();
            String razon = txtRazonSujeto.getText();
            String ident = txtIdentSujeto.getText();
            Integer provId = cmbProveedor.getValue()!=null ? cmbProveedor.getValue().getId() : null;
            String ambiente = cmbAmbiente.getValue()!=null? cmbAmbiente.getValue(): AppConstants.AMBIENTE_PRUEBAS;
            File dir = obtenerDirEscritorio();
            Empresa emp = empresaDAO.listar().isEmpty()?null:empresaDAO.listar().get(0);
            if (emp==null) throw new IllegalStateException("Empresa no configurada");
            int usuarioId = LoginController.usuarioAutenticado!=null? LoginController.usuarioAutenticado.getId():1;

            java.util.List<RetencionService.DocSustentoInput> copia = new java.util.ArrayList<>();
            for (RetencionService.DocSustentoInput d : docs) {
                RetencionService.DocSustentoInput c = new RetencionService.DocSustentoInput(d.numDocSustento, d.fechaEmisionDocSustento, d.totalSinImpuestos);
                c.codSustento = d.codSustento; c.codDocSustento = d.codDocSustento;
                for (RetencionService.RetencionLineaInput r : d.retenciones) c.retenciones.add(new RetencionService.RetencionLineaInput(r.codigo, r.codigoRetencion, r.baseImponible, r.porcentajeRetener));
                copia.add(c);
            }

            RetencionService.ResultadoRetencion res = retService.emitirRetencion(copia, periodo, tipoId, razon, ident, provId, ambiente, rutaP12, claveP12, dir, usuarioId);

            Task<SRIWebService.SRIResponse> tarea = new Task<>(){ protected SRIWebService.SRIResponse call(){ return retService.enviarYSolicitarAutorizacion(ambiente, res.xmlFirmado, res.claveAcceso); }};
            Alert prog = new Alert(Alert.AlertType.INFORMATION); prog.setTitle("Retención"); prog.setHeaderText("Consultando al SRI..."); prog.setContentText("Enviando RET "+res.numComprobante+" al SRI\nNo cierre la ventana."); prog.getButtonTypes().setAll(new ButtonType("Minimizar", ButtonBar.ButtonData.CANCEL_CLOSE));
            tarea.setOnSucceeded(e-> {
                SRIWebService.SRIResponse resp = tarea.getValue();
                new Thread(() -> {
                    try { retService.finalizarEnvioSRI(resp, res, dir); } catch (Exception ex) { logDAO.guardar("RetencionController","finalizarEnvioSRI", ex.getMessage(), ex); }
                    javafx.application.Platform.runLater(() -> {
                        prog.close();
                        String estado = resp.getEstado();
                        String msg = resp.getMensaje();
                        if (AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado) || "NO AUTORIZADO".equals(estado)) {
                            String detalle = "SRI "+estado+": "+msg+"\nClave: "+res.claveAcceso+"\nNum: "+res.numComprobante;
                            volcarErrorSRI(detalle, resp, res);
                            mostrarAlertaCopiable(Alert.AlertType.ERROR, "SRI - Retención", "SRI devuelta", detalle+"\nRevisa tabla logs y ~/vendex_errors");
                        } else {
                            mostrarAlertaCopiable(Alert.AlertType.INFORMATION, "Retención", "RET registrada", "RET "+res.numComprobante+" registrada. Estado: "+estado+(msg!=null&&!msg.isEmpty()?"\n"+msg:"")+"\nClave: "+res.claveAcceso+"\nPDF: "+res.rutaPDF);
                        }
                        new Thread(() -> { try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(res.rutaPDF)); } catch (Exception ignore) {} }, "Hilo-Abrir-PDF-RET").start();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado) && ident != null) {
                            try {
                                String correo = null;
                                if (provId != null) {
                                    for (Proveedor prov : proveedorDAO.listar()) if (prov.getId() == provId) { correo = prov.getCorreo(); break; }
                                }
                                if (correo == null) {
                                    for (Proveedor p : proveedorDAO.listar()) if (ident.equals(p.getIdentificacion())) { correo = p.getCorreo(); break; }
                                }
                                if (correo!=null && correo.contains("@") && ElectronicoUtil.debeEnviarNotificacion(estado, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion())) {
                                    String c = correo;
                                    Task<String> tMail=new Task<>(){ protected String call(){ return retService.enviarCorreoAutorizacion(c.trim(), razon, res.numComprobante, res.rutaPDF, res.rutaXML)?null:"Error correo"; }};
                                    tMail.setOnSucceeded(ev-> { if (tMail.getValue()==null) new Alert(Alert.AlertType.INFORMATION, "Correo enviado a "+c).showAndWait(); });
                                    new Thread(tMail, "Hilo-Correo-RET").start();
                                }
                            } catch (Exception ignore) {}
                        }
                        docs.clear(); retsActual.clear(); actualizarSecuencial();
                    });
                }, "Hilo-Finalizar-RET").start();
            });
            tarea.setOnFailed(ev-> { prog.close(); Throwable ex = tarea.getException(); logDAO.guardar("RetencionController","SRI-Tarea", ex!=null?ex.getMessage():"desconocido", ex instanceof Exception ? (Exception)ex : new Exception(ex)); mostrarAlertaCopiable(Alert.AlertType.ERROR, "SRI Error", "Error al consultar SRI", "Error SRI: "+(ex!=null?ex.getMessage():"desconocido")); });
            new Thread(tarea, "Hilo-SRI-RET").start(); prog.show();
        } catch (Exception ex) { logDAO.guardar("RetencionController","emitirRetencion", ex.getMessage(), ex); mostrarAlertaCopiable(Alert.AlertType.ERROR, "Error", "Error al emitir", ex.getMessage()); }
    }

    private void mostrarAlertaCopiable(Alert.AlertType tipo, String titulo, String header, String contenido) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        TextArea ta = new TextArea(contenido != null ? contenido : "");
        ta.setWrapText(true); ta.setEditable(false); ta.setPrefHeight(260); ta.setPrefWidth(540);
        VBox box = new VBox(ta); VBox.setVgrow(ta, Priority.ALWAYS);
        alert.getDialogPane().setContent(box); alert.getDialogPane().setPrefSize(600, 400); alert.setResizable(true);
        ta.requestFocus(); ta.selectAll();
        alert.showAndWait();
    }

    private void volcarErrorSRI(String detalle, SRIWebService.SRIResponse resp, RetencionService.ResultadoRetencion res) {
        try {
            String home = System.getProperty("user.home");
            java.io.File dir = new java.io.File(home, "vendex_errors");
            if (!dir.exists()) dir.mkdirs();
            String base = "RET_ERROR_" + (res!=null?res.numComprobante.replace("-",""):"") + "_" + System.currentTimeMillis();
            String contenido = detalle + "\n\n--- Recepción ---\n" + (resp!=null&&resp.getRespuestaRecepcionXml()!=null?resp.getRespuestaRecepcionXml():"") + "\n\n--- Autorización ---\n" + (resp!=null&&resp.getRespuestaAutorizacionXml()!=null?resp.getRespuestaAutorizacionXml():"") + "\n\n--- XML ---\n" + (res!=null&&res.xmlFirmado!=null?res.xmlFirmado:"");
            java.nio.file.Files.write(java.nio.file.Paths.get(new java.io.File(dir, base + ".txt").getAbsolutePath()), contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            logDAO.guardar("RetencionController","SRI-ERROR-TXT", detalle);
        } catch (Exception e) { logDAO.guardar("RetencionController","volcarErrorSRI", e.getMessage(), e instanceof Exception ? (Exception)e : new Exception(e)); }
    }

    private File obtenerDirEscritorio(){ File h=new File(System.getProperty("user.home")); for(String n: new String[]{AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT, AppConstants.DIRECTORIO_ESCRITORIO_ALT}){ File d=new File(h,n); if(d.exists()&&d.isDirectory()) return d; } File d=new File(h, AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT); d.mkdirs(); return d; }
}
