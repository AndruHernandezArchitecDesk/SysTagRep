package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.SecuenciaDocumentoDAO;
import com.tag.sysTagRep.model.SecuenciaDocumento;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class NumeracionController implements Initializable {

    @FXML private Label lblSiguienteProforma;
    @FXML private TextField txtEstabProforma;
    @FXML private TextField txtPtoProforma;
    @FXML private TextField txtInicioProforma;

    @FXML private Label lblSiguienteFactura;
    @FXML private TextField txtEstabFactura;
    @FXML private TextField txtPtoFactura;
    @FXML private TextField txtInicioFactura;

    private final SecuenciaDocumentoDAO secuenciaDAO = new SecuenciaDocumentoDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarSecuencias();
    }

    private void cargarSecuencias() {
        SecuenciaDocumento proforma = secuenciaDAO.obtener("PROFORMA");
        SecuenciaDocumento factura = secuenciaDAO.obtener("FACTURA");
        lblSiguienteProforma.setText(proforma.getProximoCodigo());
        lblSiguienteFactura.setText(factura.getProximoCodigo());
        txtEstabProforma.setText(proforma.getEstablecimiento());
        txtPtoProforma.setText(proforma.getPuntoEmision());
        txtEstabFactura.setText(factura.getEstablecimiento());
        txtPtoFactura.setText(factura.getPuntoEmision());
    }

    @FXML
    private void establecerProforma() {
        establecer("PROFORMA", txtInicioProforma, txtEstabProforma, txtPtoProforma, lblSiguienteProforma, false);
    }

    @FXML
    private void establecerFactura() {
        establecer("FACTURA", txtInicioFactura, txtEstabFactura, txtPtoFactura, lblSiguienteFactura, true);
    }

    private void establecer(String tipo, TextField txt, TextField txtEstab, TextField txtPto,
                            Label lbl, boolean esFactura) {
        try {
            String estab = txtEstab.getText().trim();
            String pto = txtPto.getText().trim();
            if (!estab.matches("\\d{1,3}")) {
                new Alert(Alert.AlertType.WARNING, "El código de establecimiento debe ser numérico (ej: 001).").showAndWait();
                return;
            }
            if (!pto.matches("\\d{1,3}")) {
                new Alert(Alert.AlertType.WARNING, "El punto de emisión debe ser numérico (ej: 001).").showAndWait();
                return;
            }
            int numero = Integer.parseInt(txt.getText().trim());
            if (numero < 1) {
                new Alert(Alert.AlertType.WARNING, "El secuencial debe ser mayor o igual a 1.").showAndWait();
                return;
            }
            SecuenciaDocumento sec = secuenciaDAO.obtener(tipo);
            String codigo = estab + "-" + pto + "-" + String.format("%09d", numero);
            boolean existe = esFactura
                    ? secuenciaDAO.existeCodigoFactura(codigo)
                    : secuenciaDAO.existeCodigoNotaVenta(codigo);
            if (existe) {
                new Alert(Alert.AlertType.ERROR, "El número " + codigo + " ya existe en la base de datos, no se puede repetir.").showAndWait();
                return;
            }
            secuenciaDAO.actualizarConfiguracion(tipo, estab, pto);
            if (!secuenciaDAO.iniciarEn(tipo, numero)) {
                new Alert(Alert.AlertType.ERROR, "No se puede iniciar en un número menor o igual al actual (" + sec.getProximoCodigo() + ").").showAndWait();
                cargarSecuencias();
                return;
            }
            new Alert(Alert.AlertType.INFORMATION, "Numeración actualizada: la próxima " + (esFactura ? "factura" : "proforma") + " será " + codigo + ".").showAndWait();
            cargarSecuencias();
            txt.clear();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Ingrese un número válido.").showAndWait();
        }
    }
}
