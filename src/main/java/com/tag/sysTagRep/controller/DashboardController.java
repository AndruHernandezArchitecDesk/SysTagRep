package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.DashboardDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.util.ScrambleText;
import com.tag.sysTagRep.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    private static final int UMBRAL_STOCK_BAJO = 5;

    @FXML
    private Label lblVentasHoy;

    @FXML
    private Label lblTitulo;

    @FXML
    private Label lblFacturasEmitidas;

    @FXML
    private Label lblProductosVendidos;

    @FXML
    private Label lblClientesAtendidos;

    @FXML
    private BarChart<String, Number> chartVentas;

    @FXML
    private VBox listaStockBajo;

    private MainController mainController;

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final LogDAO logDAO = new LogDAO();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        new ScrambleText(lblTitulo, "SYSTAG Repuestos Automotrices").repeat(true).play();
        cargarKpis();
        cargarGraficoVentas();
        cargarStockBajo();
        if (lblTitulo.getScene() != null) {
            ThemeManager.aplicarTemaGuardado(lblTitulo.getScene());
        }
    }

    private void cargarKpis() {
        try {
            LocalDate hoy = LocalDate.now();
            double ventas = dashboardDAO.ventasDelDia(hoy);
            int facturas = dashboardDAO.facturasEmitidasDelDia(hoy);
            int productos = dashboardDAO.productosVendidosDelDia(hoy);
            int clientes = dashboardDAO.clientesAtendidosDelDia(hoy);

            lblVentasHoy.setText(String.format(java.util.Locale.US, "$%,.2f", ventas));
            lblFacturasEmitidas.setText(String.valueOf(facturas));
            lblProductosVendidos.setText(String.valueOf(productos));
            lblClientesAtendidos.setText(String.valueOf(clientes));
        } catch (Exception e) {
            logDAO.guardar("DashboardController", "cargarKpis", e.getMessage(), e);
        }
    }

    private void cargarGraficoVentas() {
        try {
            Map<String, Double> ventasNV = dashboardDAO.ventasPorDia(30);
            Map<String, Double> ventasFact = dashboardDAO.facturasPorDia(30);

            java.util.TreeSet<String> todosDias = new java.util.TreeSet<>();
            todosDias.addAll(ventasNV.keySet());
            todosDias.addAll(ventasFact.keySet());

            XYChart.Series<String, Number> serieNV = new XYChart.Series<>();
            serieNV.setName("Proforma");
            XYChart.Series<String, Number> serieFact = new XYChart.Series<>();
            serieFact.setName("Factura Electrónica");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            LinkedHashMap<String, String> labelPorDia = new LinkedHashMap<>();

            for (String dia : todosDias) {
                String label = LocalDate.parse(dia).format(fmt);
                labelPorDia.put(dia, label);
                serieNV.getData().add(new XYChart.Data<>(label, ventasNV.getOrDefault(dia, 0.0)));
                serieFact.getData().add(new XYChart.Data<>(label, ventasFact.getOrDefault(dia, 0.0)));
            }

            chartVentas.getData().clear();
            chartVentas.getData().addAll(serieNV, serieFact);

            Platform.runLater(() -> {
                if (serieNV.getNode() != null) {
                    serieNV.getNode().setStyle("-fx-bar-fill: " + coloresNV()[0] + ";");
                }
                if (serieFact.getNode() != null) {
                    serieFact.getNode().setStyle("-fx-bar-fill: " + coloresFact()[0] + ";");
                }
            });
        } catch (Exception e) {
            logDAO.guardar("DashboardController", "cargarGraficoVentas", e.getMessage(), e);
        }
    }

    private String[] coloresNV() {
        return ThemeManager.esDarkMode()
                ? new String[]{"#8be9fd", "#74d4f7", "#5cbde0", "#42a5c9", "#298fb2"}
                : new String[]{"#3498db", "#2980b9", "#1f6dad", "#174f83", "#0f3859"};
    }

    private String[] coloresFact() {
        return ThemeManager.esDarkMode()
                ? new String[]{"#ffb86c", "#f0a050", "#e08840", "#d07030", "#b85820"}
                : new String[]{"#e67e22", "#d35400", "#ba4a00", "#a04000", "#873600"};
    }

    private void cargarStockBajo() {
        try {
            List<Inventario> bajos = inventarioDAO.listarStockBajo(UMBRAL_STOCK_BAJO);
            listaStockBajo.getChildren().clear();
            if (bajos.isEmpty()) {
                Label vacio = new Label("No hay productos con stock bajo.");
                vacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
                listaStockBajo.getChildren().add(vacio);
                return;
            }
            int maxMostrar = 15;
            int mostrar = Math.min(bajos.size(), maxMostrar);
            for (int i = 0; i < mostrar; i++) {
                Inventario inv = bajos.get(i);
                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.setSpacing(8);
                Label punto = new Label("•");
                punto.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-font-weight: bold;");
                Label nombre = new Label(inv.getDescripcion());
                nombre.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                Region region = new Region();
                HBox.setHgrow(region, Priority.ALWAYS);
                Label cantidad = new Label(String.valueOf(inv.getCantidad()));
                cantidad.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                row.getChildren().addAll(punto, nombre, region, cantidad);
                listaStockBajo.getChildren().add(row);
            }
            if (bajos.size() > maxMostrar) {
                Label mas = new Label("... y " + (bajos.size() - maxMostrar) + " más");
                mas.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
                listaStockBajo.getChildren().add(mas);
            }
        } catch (Exception e) {
            logDAO.guardar("DashboardController", "cargarStockBajo", e.getMessage(), e);
        }
    }

    @FXML
    private void irDashboard2() {
        if (mainController != null) {
            mainController.abrirDashboard2();
        }
    }
}
