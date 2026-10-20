package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.config.VendexiConfig;
import com.vendex.model.VendexiConsumo;
import com.vendex.service.VendexiConsumoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel administrativo de medición y capacidad de Vendexi AI (spec §10.1/§10.3).
 * Solo lectura de consumo + renovación de periodo (acción administrativa).
 */
public class ConsumoIaController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int LIMITE_FILA = 200;

    private final VendexiConsumoService servicio;

    @FXML private Label lblCapacidad;
    @FXML private Label lblConsumido;
    @FXML private Label lblRestante;
    @FXML private Label lblCosto;
    @FXML private Label lblInicio;
    @FXML private Label lblPrecios;
    @FXML private Button btnRenovar;
    @FXML private Button btnActualizar;
    @FXML private TableView<VendexiConsumo> tblConsumo;
    @FXML private TableColumn<VendexiConsumo, String> colFecha;
    @FXML private TableColumn<VendexiConsumo, String> colModulo;
    @FXML private TableColumn<VendexiConsumo, String> colOperacion;
    @FXML private TableColumn<VendexiConsumo, String> colModelo;
    @FXML private TableColumn<VendexiConsumo, Number> colTokensIn;
    @FXML private TableColumn<VendexiConsumo, Number> colTokensOut;
    @FXML private TableColumn<VendexiConsumo, Number> colCosto;

    private final ObservableList<VendexiConsumo> datos = FXCollections.observableArrayList();

    public ConsumoIaController(AppContext ctx) {
        this.servicio = ctx.vendexiConsumoService;
    }

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(c -> {
            LocalDateTime f = c.getValue().getFecha();
            return new javafx.beans.property.SimpleStringProperty(f != null ? f.format(FMT) : "");
        });
        colModulo.setCellValueFactory(new PropertyValueFactory<>("modulo"));
        colOperacion.setCellValueFactory(new PropertyValueFactory<>("operacion"));
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colTokensIn.setCellValueFactory(new PropertyValueFactory<>("tokensEntrada"));
        colTokensOut.setCellValueFactory(new PropertyValueFactory<>("tokensSalida"));
        colCosto.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(
                c.getValue().getCostoEstimado() != null ? c.getValue().getCostoEstimado() : BigDecimal.ZERO));
        tblConsumo.setItems(datos);

        boolean esAdmin = com.vendex.util.SesionActual.tienePermiso("USUARIO_GESTIONAR");
        btnRenovar.setVisible(esAdmin);
        btnRenovar.setManaged(esAdmin);

        recargar();
    }

    @FXML
    private void onActualizar() {
        recargar();
    }

    @FXML
    private void onRenovarPeriodo() {
        if (!com.vendex.util.SesionActual.tienePermiso("USUARIO_GESTIONAR")) {
            new Alert(Alert.AlertType.WARNING, "Se requiere permiso de administrador.", ButtonType.OK).showAndWait();
            return;
        }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Renovar el periodo de capacidad de IA?\nEl contador de tokens consumidos vuelve a 0.",
                ButtonType.OK, ButtonType.CANCEL);
        conf.setHeaderText("Renovar periodo de capacidad");
        if (!ButtonType.OK.equals(conf.showAndWait().orElse(ButtonType.CANCEL))) return;
        servicio.renovarPeriodo();
        recargar();
    }

    private void recargar() {
        try {
            VendexiConsumoService.EstadoCapacidad e = servicio.estado();
            lblCapacidad.setText(e.capacidad > 0
                    ? fmtTokens(e.capacidad) + " tokens por periodo"
                    : "sin límite definido (0 en licencia)");
            lblConsumido.setText(fmtTokens(e.consumido) + " tokens"
                    + (e.agotada() ? "  — CAPACIDAD AGOTADA" : ""));
            lblRestante.setText(e.restante() < 0 ? "sin límite" : fmtTokens(e.restante()) + " tokens");
            lblCosto.setText("$ " + (e.costoPeriodo != null ? e.costoPeriodo.setScale(4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO));
            lblInicio.setText(e.inicioPeriodo != null ? e.inicioPeriodo.format(FMT) : "—");
            lblPrecios.setText("entrada $" + VendexiConfig.costoUsdPor1mEntrada() + " / 1M · salida $"
                    + VendexiConfig.costoUsdPor1mSalida() + " / 1M (estimado, configurable en "
                    + VendexiConfig.getArchivo().getAbsolutePath() + ")");
            datos.setAll((List<VendexiConsumo>) servicio.listarReciente(LIMITE_FILA));
        } catch (Exception ex) {
            lblCapacidad.setText("no disponible");
        }
    }

    private static String fmtTokens(long n) {
        if (n >= 1_000_000) return String.format("%.2f M", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1f k", n / 1_000.0);
        return String.valueOf(n);
    }
}
