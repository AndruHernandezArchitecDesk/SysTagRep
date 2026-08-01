package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.util.SortTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.util.converter.BigDecimalStringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GestionStockController implements Initializable {

    @FXML private TextField txtBuscar;
    @FXML private Accordion accordionProductos;

    private final InventarioDAO dao = new InventarioDAO();
    private List<Inventario> todos;
    private Map<String, List<Inventario>> agrupados;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarDatos();
        construirVista();
        txtBuscar.textProperty().addListener((obs, old, val) -> construirVista());
    }

    private void cargarDatos() {
        todos = dao.listar().stream()
                .sorted(Comparator.comparing(Inventario::getDescripcion, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Inventario::getFecha_ingreso, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private void construirVista() {
        String filtro = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";

        agrupados = new LinkedHashMap<>();
        for (Inventario inv : todos) {
            if (!filtro.isEmpty() && !inv.getDescripcion().toLowerCase().contains(filtro)
                    && !inv.getProveedor().toLowerCase().contains(filtro)
                    && !inv.getFormaPago().toLowerCase().contains(filtro)
                    && !inv.getCodigo().toLowerCase().contains(filtro))
                continue;
            agrupados.computeIfAbsent(inv.getDescripcion(), k -> new ArrayList<>()).add(inv);
        }

        accordionProductos.getPanes().clear();
        for (Map.Entry<String, List<Inventario>> entry : agrupados.entrySet()) {
            String desc = entry.getKey();
            List<Inventario> items = entry.getValue();
            long totalCant = items.stream().mapToLong(Inventario::getCantidad).sum();

            TableView<Inventario> table = crearTimelineTable(items);
            TitledPane pane = new TitledPane(desc + "  (" + items.size() + " compras, " + totalCant + " uds.)", table);
            pane.setAnimated(false);
            accordionProductos.getPanes().add(pane);
        }

        if (!accordionProductos.getPanes().isEmpty())
            accordionProductos.setExpandedPane(accordionProductos.getPanes().get(0));
    }

    private TableView<Inventario> crearTimelineTable(List<Inventario> items) {
        ObservableList<Inventario> data = FXCollections.observableArrayList(items);

        TableColumn<Inventario, LocalDateTime> colFecha = new TableColumn<>("Fecha");
        colFecha.setPrefWidth(120);
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha_ingreso"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(fmt));
            }
        });

        TableColumn<Inventario, String> colProveedor = new TableColumn<>("Proveedor");
        colProveedor.setPrefWidth(120);
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));

        TableColumn<Inventario, BigDecimal> colCosto = new TableColumn<>("Costo sin IVA");
        colCosto.setPrefWidth(100);
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colCosto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : "$ " + item.setScale(2, java.math.RoundingMode.HALF_UP).toString());
            }
        });

        TableColumn<Inventario, BigDecimal> colPrecio = new TableColumn<>("Precio Venta");
        colPrecio.setPrefWidth(100);
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colPrecio.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        colPrecio.setOnEditCommit(event -> {
            Inventario item = event.getRowValue();
            BigDecimal nuevo = event.getNewValue();
            if (nuevo != null && nuevo.compareTo(BigDecimal.ZERO) >= 0) {
                item.setPrecioVenta(nuevo);
                dao.actualizarPrecioVenta(item.getId(), nuevo);
            }
        });

        TableColumn<Inventario, Integer> colCant = new TableColumn<>("Cantidad");
        colCant.setPrefWidth(70);
        colCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        TableColumn<Inventario, String> colForma = new TableColumn<>("Forma de Pago");
        colForma.setPrefWidth(110);
        colForma.setCellValueFactory(new PropertyValueFactory<>("formaPago"));

        TableView<Inventario> table = new TableView<>(data);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(Math.min(items.size() * 30 + 30, 200));
        table.getColumns().addAll(colFecha, colProveedor, colCosto, colPrecio, colCant, colForma);
        SortTable.agregarBotones(table);

        return table;
    }
}
