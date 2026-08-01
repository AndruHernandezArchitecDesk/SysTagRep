package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ComprobanteDAO;
import com.tag.sysTagRep.dao.FacturaRegistroDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.model.FacturaRegistro;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import com.tag.sysTagRep.util.SRIWebService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class HistorialFacturaController implements Initializable {

    @FXML private TextField txtBuscar;
    @FXML private TableView<FacturaRegistro> tblFacturas;
    @FXML private TableColumn<FacturaRegistro, String> colCodigo;
    @FXML private TableColumn<FacturaRegistro, String> colNumComprobante;
    @FXML private TableColumn<FacturaRegistro, LocalDateTime> colFecha;
    @FXML private TableColumn<FacturaRegistro, String> colCliente;
    @FXML private TableColumn<FacturaRegistro, String> colFormaPago;
    @FXML private TableColumn<FacturaRegistro, BigDecimal> colSubtotal;
    @FXML private TableColumn<FacturaRegistro, BigDecimal> colIva;
    @FXML private TableColumn<FacturaRegistro, BigDecimal> colTotal;
    @FXML private TableColumn<FacturaRegistro, String> colEstado;
    @FXML private TableColumn<FacturaRegistro, String> colMensaje;

    @FXML private Label lblPaginaInfo;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnConsultarSri;
    @FXML private ComboBox<Integer> cmbPageSize;

    private int currentPage = 1;
    private int pageSize = 25;
    private int totalPages = 1;
    private int totalCount = 0;
    private final FacturaRegistroDAO dao = new FacturaRegistroDAO();
    private final LogDAO logDAO = new LogDAO();
    private final ObservableList<FacturaRegistro> listaFacturas = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNumComprobante.setCellValueFactory(new PropertyValueFactory<>("numComprobante"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoSri"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                FacturaRegistro fr = getTableRow().getItem();
                String texto = formatearEstado(estado, fr.getMensajeSri());
                setText(texto);
                setStyle("-fx-text-fill: " + colorEstado(estado, fr.getMensajeSri()) + "; -fx-font-weight: bold;");
                String detalle = fr.getMensajeSri();
                setTooltip(detalle != null && !detalle.trim().isEmpty()
                        ? new Tooltip(detalle) : null);
            }
        });
        colMensaje.setCellValueFactory(new PropertyValueFactory<>("mensajeSri"));
        colMensaje.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String mensaje, boolean empty) {
                super.updateItem(mensaje, empty);
                setText(empty || mensaje == null ? null : mensaje);
                setTooltip(mensaje != null && !mensaje.isEmpty() ? new Tooltip(mensaje) : null);
            }
        });

        tblFacturas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblFacturas);

        iniciarPageSize();
        txtBuscar.textProperty().addListener((obs, old, val) -> { currentPage = 1; cargarDatos(); });

        cargarDatos();
    }

    private void cargarDatos() {
        String filtro = txtBuscar.getText();
        totalCount = dao.contar(filtro);
        totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        listaFacturas.setAll(dao.listarPaginado(currentPage, pageSize, filtro));
        tblFacturas.setItems(listaFacturas);
        actualizarPaginaInfo();
    }

    private void iniciarPageSize() {
        ComboFilter.habilitarEnteros(cmbPageSize, FXCollections.observableArrayList(25, 50, 100));
        cmbPageSize.setValue(25);
        cmbPageSize.setOnAction(e -> {
            Integer valor = cmbPageSize.getValue();
            if (valor == null) {
                try { valor = Integer.parseInt(cmbPageSize.getEditor().getText().trim()); }
                catch (NumberFormatException ignored) {}
            }
            if (valor != null && valor > 0) {
                pageSize = valor;
                currentPage = 1;
                cargarDatos();
            }
        });
    }

    @FXML private void irPaginaAnterior() {
        if (currentPage > 1) { currentPage--; cargarDatos(); }
    }

    @FXML private void irPaginaSiguiente() {
        if (currentPage < totalPages) { currentPage++; cargarDatos(); }
    }

    @FXML
    private void consultarSri() {
        List<FacturaRegistro> pendientes = dao.listarPendientesSri();
        if (pendientes.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay facturas pendientes por consultar con el SRI.").showAndWait();
            return;
        }
        btnConsultarSri.setDisable(true);
        Task<String> tarea = new Task<>() {
            @Override
            protected String call() {
                int autorizadas = 0, rechazadas = 0, pendientesN = 0, errores = 0;
                ComprobanteDAO ceDAO = new ComprobanteDAO();
                for (FacturaRegistro f : pendientes) {
                    String clave = f.getClaveAcceso();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    try {
                        String ambiente = f.getAmbienteSri() == null || f.getAmbienteSri().trim().isEmpty()
                                ? "PRUEBAS" : f.getAmbienteSri();
                        SRIWebService.SRIResponse r = new SRIWebService(ambiente).consultarAutorizacion(clave);
                        String estado = r.getEstado();
                        if ("AUTORIZADO".equals(estado) || "RECHAZADA".equals(estado) || "DEVUELTA".equals(estado)) {
                            ceDAO.actualizarEstado(clave, estado, r.getMensaje(), null, r.getNumeroAutorizacion());
                            dao.actualizarEstado(clave, estado);
                            if ("AUTORIZADO".equals(estado)) autorizadas++; else rechazadas++;
                        } else if ("ERROR".equals(estado)) {
                            errores++;
                        } else {
                            pendientesN++;
                        }
                    } catch (Exception e) {
                        errores++;
                        logDAO.guardar("HistorialFacturaController", "consultarSri", "Error consultando " + clave + ": " + e.getMessage(), e);
                    }
                }
                return "Autorizadas: " + autorizadas + "\nRechazadas/Devueltas: " + rechazadas
                        + "\nSiguen pendientes: " + pendientesN + "\nCon error: " + errores;
            }
        };
        tarea.setOnSucceeded(e -> {
            btnConsultarSri.setDisable(false);
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Consulta al SRI finalizada.\n\n" + tarea.getValue()).showAndWait();
        });
        tarea.setOnFailed(e -> {
            btnConsultarSri.setDisable(false);
            Throwable ex = tarea.getException();
            logDAO.guardar("HistorialFacturaController", "consultarSri", String.valueOf(ex));
            new Alert(Alert.AlertType.ERROR, "Error al consultar el SRI: "
                    + (ex != null ? ex.getMessage() : "desconocido")).showAndWait();
        });
        new Thread(tarea, "Hilo-ConsultarSRI").start();
    }

    private void actualizarPaginaInfo() {
        lblPaginaInfo.setText("Página " + currentPage + " de " + totalPages + " (" + totalCount + " registros)");
        btnAnterior.setDisable(currentPage <= 1);
        btnSiguiente.setDisable(currentPage >= totalPages);
    }

    private String formatearEstado(String estado, String mensaje) {
        String e = estado == null ? "" : estado.trim().toUpperCase();
        String m = mensaje == null ? "" : mensaje.toLowerCase();
        if (m.contains("firma no configurada") || m.contains("no se configuró")) return "NO ENVIADO";
        if (m.contains("error de conexión")) return "ERROR DE CONEXIÓN";
        switch (e) {
            case "AUTORIZADO": return "AUTORIZADA";
            case "RECHAZADA": return "RECHAZADA";
            case "DEVUELTA": return "DEVUELTA";
            case "PENDIENTE": return m.contains("recibido") ? "RECIBIDO SRI (pendiente)" : "PENDIENTE";
            default: return e.isEmpty() ? "NO ENVIADO" : e;
        }
    }

    private String colorEstado(String estado, String mensaje) {
        String e = estado == null ? "" : estado.trim().toUpperCase();
        String m = mensaje == null ? "" : mensaje.toLowerCase();
        if (m.contains("firma no configurada") || m.contains("no se configuró") || e.isEmpty()) {
            return "#6c757d";
        }
        switch (e) {
            case "AUTORIZADO": return "#198754";
            case "RECHAZADA":
            case "DEVUELTA": return "#dc3545";
            case "PENDIENTE": return "#fd7e14";
            default: return "#0d6efd";
        }
    }
}
