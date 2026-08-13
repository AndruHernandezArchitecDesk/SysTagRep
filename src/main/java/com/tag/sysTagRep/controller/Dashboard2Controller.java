package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.DashboardDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.model.Alerta;
import com.tag.sysTagRep.service.AlertaService;
import com.tag.sysTagRep.util.ScrambleText;
import com.tag.sysTagRep.util.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class Dashboard2Controller implements Initializable {

    @FXML
    private BarChart<String, Number> chartVentas;

    @FXML
    private BarChart<String, Number> chartCompras;

    @FXML
    private PieChart chartMarcas;

    @FXML
    private PieChart chartGrupos;

    @FXML
    private TableView<Alerta> tblAlertasHome;

    @FXML
    private Label lblTitulo;

    private MainController mainController;

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final LogDAO logDAO = new LogDAO();
    private final AlertaService alertaService = new AlertaService();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        new ScrambleText(lblTitulo, "SYSTAG Repuestos Automotrices").repeat(true).play();
        cargarGraficos();
        cargarAlertasHome();
        if (lblTitulo.getScene() != null) {
            ThemeManager.aplicarTemaGuardado(lblTitulo.getScene());
        }
    }

    private void cargarAlertasHome() {
        if (tblAlertasHome == null) return;
        List<Alerta> alertas = alertaService.obtenerTodas();
        ObservableList<Alerta> items = FXCollections.observableArrayList(alertas);
        tblAlertasHome.setItems(items);
    }

    private void cargarGraficos() {
        cargarGraficoVentas();
        cargarGraficoCompras();
        cargarGraficoMarcas();
        cargarGraficoGrupos();
    }

    private String[] coloresVentasNV() {
        return ThemeManager.esDarkMode()
                ? new String[]{"#8be9fd", "#74d4f7", "#5cbde0", "#42a5c9", "#298fb2"}
                : new String[]{"#3498db", "#2980b9", "#1f6dad", "#174f83", "#0f3859"};
    }

    private String[] coloresVentasFact() {
        return ThemeManager.esDarkMode()
                ? new String[]{"#ffb86c", "#f0a050", "#e08840", "#d07030", "#b85820"}
                : new String[]{"#e67e22", "#d35400", "#ba4a00", "#a04000", "#873600"};
    }

    private String[] coloresCompras() {
        return ThemeManager.esDarkMode()
                ? new String[]{"#50fa7b", "#40d86b", "#30b65b", "#20944b", "#10703b", "#40e0c0", "#30c8b0", "#20b0a0", "#109890", "#008080"}
                : new String[]{"#2ecc71", "#27ae60", "#1e8449", "#145a32", "#0b3d1f",
                "#1abc9c", "#16a085", "#0e6655", "#084a38", "#053122"};
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

            String[] coloresNV = coloresVentasNV();
            String[] coloresFact = coloresVentasFact();
            Platform.runLater(() -> {
                if (serieNV.getNode() != null) {
                    serieNV.getNode().setStyle("-fx-bar-fill: " + coloresNV[0] + ";");
                }
                if (serieFact.getNode() != null) {
                    serieFact.getNode().setStyle("-fx-bar-fill: " + coloresFact[0] + ";");
                }
                colorearEjeX(chartVentas, ventasNV, ventasFact);
            });

        } catch (Exception e) {
            logDAO.guardar("Dashboard2Controller", "cargarGraficoVentas", e.getMessage(), e);
        }
    }

    private void cargarGraficoCompras() {
        try {
            Map<String, Double> compras = dashboardDAO.comprasPorDia(30);

            XYChart.Series<String, Number> serieCompras = new XYChart.Series<>();
            serieCompras.setName("Compras (Inventario)");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

            for (Map.Entry<String, Double> entry : compras.entrySet()) {
                String label = LocalDate.parse(entry.getKey()).format(fmt);
                serieCompras.getData().add(new XYChart.Data<>(label, entry.getValue()));
            }

            chartCompras.getData().clear();
            chartCompras.getData().add(serieCompras);

            String[] coloresComp = coloresCompras();
            Platform.runLater(() -> {
                if (serieCompras.getNode() != null) {
                    serieCompras.getNode().setStyle("-fx-bar-fill: " + coloresComp[0] + ";");
                }
                colorearEjeXSimple(chartCompras);
            });

        } catch (Exception e) {
            logDAO.guardar("Dashboard2Controller", "cargarGraficoCompras", e.getMessage(), e);
        }
    }

    private final String[] COLORES_PIE = {
        "#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6",
        "#1abc9c", "#e67e22", "#34495e", "#16a085", "#c0392b"
    };

    private void cargarGraficoMarcas() {
        try {
            Map<String, Integer> datos = dashboardDAO.inventarioPorMarca();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            chartMarcas.setData(pieData);
            chartMarcas.setTitle("Inventario por Marca");
            chartMarcas.setLabelsVisible(true);

            Platform.runLater(() -> {
                int i = 0;
                for (PieChart.Data d : chartMarcas.getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setStyle("-fx-pie-color: " + COLORES_PIE[i % COLORES_PIE.length] + ";");
                    }
                    i++;
                }
                colorearLeyendaPie(chartMarcas);
            });
        } catch (Exception e) {
            logDAO.guardar("Dashboard2Controller", "cargarGraficoMarcas", e.getMessage(), e);
        }
    }

    private void cargarGraficoGrupos() {
        try {
            Map<String, Integer> datos = dashboardDAO.inventarioPorGrupo();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            chartGrupos.setData(pieData);
            chartGrupos.setTitle("Inventario por Grupo");
            chartGrupos.setLabelsVisible(true);

            Platform.runLater(() -> {
                int i = 0;
                for (PieChart.Data d : chartGrupos.getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setStyle("-fx-pie-color: " + COLORES_PIE[i % COLORES_PIE.length] + ";");
                    }
                    i++;
                }
                colorearLeyendaPie(chartGrupos);
            });
        } catch (Exception e) {
            logDAO.guardar("Dashboard2Controller", "cargarGraficoGrupos", e.getMessage(), e);
        }
    }

    private void colorearEjeX(BarChart<String, Number> chart,
                               Map<String, Double> datosNV, Map<String, Double> datosFact) {
        try {
            CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
            java.util.List<Text> textNodes = new java.util.ArrayList<>();
            buscarTextNodes(xAxis, textNodes);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            for (Text txt : textNodes) {
                String labelText = txt.getText();
                if (labelText == null || labelText.isEmpty()) continue;
                for (Map.Entry<String, Double> entry : datosNV.entrySet()) {
                    String dia = entry.getKey();
                    String labelEsperado = LocalDate.parse(dia).format(fmt);
                    if (labelText.equals(labelEsperado)) {
                        double valNV = datosNV.getOrDefault(dia, 0.0);
                        double valFact = datosFact.getOrDefault(dia, 0.0);
                        if (ThemeManager.esDarkMode()) {
                            txt.setFill(valFact >= valNV ? Color.web("#ffb86c") : Color.web("#8be9fd"));
                        } else {
                            txt.setFill(valFact >= valNV ? Color.web("#d35400") : Color.web("#2980b9"));
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void colorearEjeXSimple(BarChart<String, Number> chart) {
        try {
            CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
            java.util.List<Text> textNodes = new java.util.ArrayList<>();
            buscarTextNodes(xAxis, textNodes);
            for (Text txt : textNodes) {
                if (txt.getText() != null && !txt.getText().isEmpty()) {
                    txt.setFill(ThemeManager.esDarkMode() ? Color.web("#50fa7b") : Color.web("#27ae60"));
                }
            }
        } catch (Exception ignored) {}
    }

    private void buscarTextNodes(Node nodo, java.util.List<Text> resultado) {
        if (nodo instanceof Text) {
            resultado.add((Text) nodo);
        }
        if (nodo instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) nodo).getChildrenUnmodifiable()) {
                buscarTextNodes(child, resultado);
            }
        }
    }

    private void colorearLeyendaPie(PieChart chart) {
        try {
            java.util.List<Node> legendItems = new java.util.ArrayList<>(chart.lookupAll(".chart-legend-item"));
            int idx = 0;
            for (Node item : legendItems) {
                for (Node child : ((javafx.scene.Parent) item).getChildrenUnmodifiable()) {
                    if (child.getStyleClass().contains("chart-legend-item-color")) {
                        String color = COLORES_PIE[idx % COLORES_PIE.length];
                        child.setStyle("-fx-background-color: " + color + ";");
                    }
                }
                idx++;
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void irAlertas() {
        if (mainController != null) {
            mainController.abrirAlertas();
        }
    }

    @FXML
    private void irDashboard1() {
        if (mainController != null) {
            mainController.abrirDashboard1();
        }
    }
}
