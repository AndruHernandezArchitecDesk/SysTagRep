package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.dao.PercheroDAO;
import com.tag.sysTagRep.dao.UbicacionDetalleDAO;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.Perchero;
import com.tag.sysTagRep.model.UbicacionDetalle;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UbicacionPercheroController implements Initializable {

    @FXML private ComboBox<String> cmbPerchero;
    @FXML private VBox contenedorSecciones;
    @FXML private TextField txtUbicacionSeleccionada;
    @FXML private Label lblEstadoUbicacion;
    @FXML private ScrollPane scrollGrid;
    @FXML private Button btnNuevaSeccion;
    @FXML private Button btnNuevoPerchero;
    @FXML private Button btnEliminarPerchero;

    private final PercheroDAO percheroDAO = new PercheroDAO();
    private final UbicacionDetalleDAO ubicacionDAO = new UbicacionDetalleDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final LogDAO logDAO = new LogDAO();
    private final ObservableList<String> listaNombresPerchero = FXCollections.observableArrayList();
    private UbicacionDetalle ubicacionSeleccionada;
    private VBox bloqueSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ComboFilter.habilitar(cmbPerchero, listaNombresPerchero);
        cargarNombresPerchero();
    }

    private void cargarNombresPerchero() {
        listaNombresPerchero.setAll(percheroDAO.listarNombres());
        if (!listaNombresPerchero.isEmpty()) {
            cmbPerchero.getSelectionModel().selectFirst();
            seleccionarPerchero();
        }
    }

    private Perchero buscarPerchero(String nombre, String seccion) {
        List<Perchero> secciones = percheroDAO.listarPorNombre(nombre);
        for (Perchero p : secciones) {
            if (p.getSeccion().equals(seccion)) return p;
        }
        return null;
    }

    @FXML
    private void seleccionarPerchero() {
        String nombre = cmbPerchero.getValue();
        if (nombre == null || nombre.isEmpty()) {
            contenedorSecciones.getChildren().clear();
            btnNuevaSeccion.setDisable(true);
            btnEliminarPerchero.setDisable(true);
            txtUbicacionSeleccionada.clear();
            lblEstadoUbicacion.setText("");
            return;
        }
        btnNuevaSeccion.setDisable(false);
        btnEliminarPerchero.setDisable(false);
        cargarSecciones(nombre);
    }

    private void cargarSecciones(String nombrePerchero) {
        if (nombrePerchero == null || nombrePerchero.isEmpty()) return;
        contenedorSecciones.getChildren().clear();
        txtUbicacionSeleccionada.clear();
        lblEstadoUbicacion.setText("");
        ubicacionSeleccionada = null;
        bloqueSeleccionado = null;

        List<Perchero> secciones = percheroDAO.listarPorNombre(nombrePerchero);
        if (secciones.isEmpty()) return;

        for (Perchero seccion : secciones) {
            VBox filaSeccion = crearFilaSeccion(seccion);
            contenedorSecciones.getChildren().add(filaSeccion);
        }
    }

    @FXML
    private void eliminarPerchero() {
        String nombre = cmbPerchero.getValue();
        if (nombre == null || nombre.isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el perchero \"" + nombre + "\" con todas sus secciones y lugares?",
                ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                percheroDAO.eliminarPorNombre(nombre);
                cargarNombresPerchero();
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "eliminarPerchero", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al eliminar perchero: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void eliminarSeccion(Perchero seccion) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la sección " + seccion.getSeccion() + " del perchero " + seccion.getNombrePerchero()
                        + " con sus " + seccion.getCantidadLugares() + " lugares?",
                ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                percheroDAO.eliminar(seccion.getId());
                cargarSecciones(cmbPerchero.getValue());
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "eliminarSeccion", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al eliminar sección: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void eliminarLugar(UbicacionDetalle u) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el lugar " + u.getCodigoUbicacion() + "?",
                ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                ubicacionDAO.eliminarLugar(u.getId());
                cargarSecciones(cmbPerchero.getValue());
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "eliminarLugar", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al eliminar lugar: " + e.getMessage()).showAndWait();
            }
        }
    }

    private VBox crearFilaSeccion(Perchero seccion) {
        VBox contenedor = new VBox(8);
        contenedor.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 12; -fx-border-color: #d1d5db; -fx-border-radius: 8;");

        List<UbicacionDetalle> ubicaciones = ubicacionDAO.listarPorPerchero(seccion.getId());

        long ocupados = ubicaciones.stream().filter(u -> !u.isDisponible()).count();
        String grupos = ubicaciones.stream()
                .filter(u -> u.getGrupoNombre() != null)
                .map(UbicacionDetalle::getGrupoNombre)
                .distinct().reduce((a, b) -> a + ", " + b).orElse("");
        String marcas = ubicaciones.stream()
                .filter(u -> u.getMarcaNombre() != null)
                .map(UbicacionDetalle::getMarcaNombre)
                .distinct().reduce((a, b) -> a + ", " + b).orElse("");

        Label lblSeccion = new Label("Sección " + seccion.getSeccion() + " — " + seccion.getCantidadLugares() + " lugares  |  Ocupados: " + ocupados);
        lblSeccion.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblSeccion.setTextFill(javafx.scene.paint.Color.web("#2c3e50"));

        Button btnEliminarSeccion = new Button("✕");
        btnEliminarSeccion.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 8; -fx-cursor: hand;");
        btnEliminarSeccion.setTooltip(new Tooltip("Eliminar sección"));
        btnEliminarSeccion.setOnAction(e -> eliminarSeccion(seccion));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox cabecera = new HBox(10, lblSeccion, spacer, btnEliminarSeccion);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        HBox filaBloques = new HBox(10);
        filaBloques.setAlignment(Pos.CENTER_LEFT);

        for (UbicacionDetalle u : ubicaciones) {
            VBox bloque = crearBloque(u);
            filaBloques.getChildren().add(bloque);
        }

        contenedor.getChildren().addAll(cabecera, filaBloques);
        return contenedor;
    }

    private VBox crearBloque(UbicacionDetalle u) {
        VBox bloque = new VBox(3);
        bloque.setAlignment(Pos.CENTER);
        bloque.setPrefSize(100, 80);
        bloque.setMaxSize(100, 80);
        bloque.setPadding(new Insets(4));
        bloque.setUserData(u);

        boolean disponible = u.isDisponible();

        if (disponible) {
            Label lblLibre = new Label("LIBRE");
            lblLibre.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            lblLibre.setTextFill(javafx.scene.paint.Color.WHITE);
            bloque.getChildren().add(lblLibre);

            bloque.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #1e8449; -fx-border-width: 1; -fx-cursor: hand;");
            bloque.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    mostrarDialogoAsignarProducto(bloque, u);
                }
            });
            bloque.setOnMouseEntered(e -> {
                if (bloque != bloqueSeleccionado) {
                    bloque.setStyle("-fx-background-color: #2ecc71; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #27ae60; -fx-border-width: 2; -fx-cursor: hand;");
                }
            });
            bloque.setOnMouseExited(e -> {
                if (bloque != bloqueSeleccionado) {
                    bloque.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #1e8449; -fx-border-width: 1; -fx-cursor: hand;");
                }
            });
        } else {
            bloque.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #c0392b; -fx-border-width: 1; -fx-cursor: pointer;");
            if (u.getMarcaNombre() != null) {
                Label lblMarca = new Label(truncarTexto(u.getMarcaNombre(), 16));
                lblMarca.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                lblMarca.setTextFill(javafx.scene.paint.Color.WHITE);
                bloque.getChildren().add(lblMarca);
            }
            if (u.getProductoDescripcion() != null) {
                Label lblModelo = new Label(truncarTexto(u.getProductoDescripcion(), 16));
                lblModelo.setFont(Font.font("Arial", FontWeight.NORMAL, 8));
                lblModelo.setTextFill(javafx.scene.paint.Color.web("#f5b7b1"));
                bloque.getChildren().add(lblModelo);
            }
            if (u.getStockAsignado() != null) {
                Label lblStock = new Label("Stock: " + u.getStockAsignado());
                lblStock.setFont(Font.font("Arial", FontWeight.BOLD, 8));
                lblStock.setTextFill(javafx.scene.paint.Color.WHITE);
                bloque.getChildren().add(lblStock);
            }
            bloque.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    mostrarDialogoDetalleOcupado(u);
                }
            });
        }

        ContextMenu menu = new ContextMenu();
        MenuItem miEliminarLugar = new MenuItem("Eliminar lugar");
        miEliminarLugar.setOnAction(e -> eliminarLugar(u));
        menu.getItems().add(miEliminarLugar);
        bloque.setOnContextMenuRequested(e -> menu.show(bloque, e.getScreenX(), e.getScreenY()));

        Tooltip tt = new Tooltip(crearTooltipText(u));
        tt.setStyle("-fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: white; -fx-padding: 8;");
        Tooltip.install(bloque, tt);

        return bloque;
    }

    private String truncarTexto(String texto, int maxChars) {
        if (texto == null) return "";
        return texto.length() > maxChars ? texto.substring(0, maxChars) + "…" : texto;
    }

    private String crearTooltipText(UbicacionDetalle u) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ubicación: ").append(u.getCodigoUbicacion()).append("\n");
        sb.append("Estado: ").append(u.getEstado()).append("\n");
        sb.append("Perchero: ").append(u.getNombrePerchero()).append("\n");
        sb.append("Sección: ").append(u.getSeccion()).append("\n");
        if (u.getIdProducto() != null) {
            sb.append("Producto: ").append(u.getProductoDescripcion() != null ? u.getProductoDescripcion() : "-").append("\n");
            sb.append("Código: ").append(u.getProductoCodigo() != null ? u.getProductoCodigo() : "-").append("\n");
            sb.append("Grupo: ").append(u.getGrupoNombre() != null ? u.getGrupoNombre() : "-").append("\n");
            sb.append("Marca: ").append(u.getMarcaNombre() != null ? u.getMarcaNombre() : "-").append("\n");
            sb.append("Cantidad (inventario): ").append(u.getCantidad()).append("\n");
            sb.append("Stock en este lugar: ").append(u.getStockAsignado() != null ? u.getStockAsignado() : 0);
        }
        return sb.toString();
    }

    private void seleccionarBloque(VBox bloque, UbicacionDetalle u) {
        if (bloqueSeleccionado != null) {
            UbicacionDetalle prev = (UbicacionDetalle) bloqueSeleccionado.getUserData();
            if (prev.isDisponible()) {
                bloqueSeleccionado.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #1e8449; -fx-border-width: 1; -fx-cursor: hand;");
            }
        }
        bloqueSeleccionado = bloque;
        ubicacionSeleccionada = u;
        bloque.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #3498db; -fx-border-width: 3; -fx-cursor: hand;");
        txtUbicacionSeleccionada.setText(u.getCodigoUbicacion());
        lblEstadoUbicacion.setText("[" + u.getEstado() + "]");
        lblEstadoUbicacion.setTextFill(u.isDisponible() ? javafx.scene.paint.Color.web("#27ae60") : javafx.scene.paint.Color.web("#e74c3c"));
    }

    private void mostrarDialogoAsignarProducto(VBox bloque, UbicacionDetalle u) {
        List<Inventario> productos = ubicacionDAO.listarProductosDisponibles();
        if (productos.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay productos libres. Todos los productos ya tienen su stock asignado en algún lugar.").showAndWait();
            return;
        }

        Dialog<AsignacionProducto> dialog = new Dialog<>();
        dialog.setTitle("Asignar Producto");
        dialog.setHeaderText("Seleccione un producto libre para la ubicación " + u.getCodigoUbicacion());

        ButtonType btnAsignar = new ButtonType("Asignar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAsignar, ButtonType.CANCEL);

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por código, descripción, grupo o marca...");

        TableView<Inventario> tblProductos = new TableView<>();
        tblProductos.setPrefHeight(350);

        TableColumn<Inventario, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setPrefWidth(120);
        colCodigo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("codigo"));

        TableColumn<Inventario, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setPrefWidth(300);
        colDescripcion.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descripcion"));

        TableColumn<Inventario, Integer> colCantidad = new TableColumn<>("Stock libre");
        colCantidad.setPrefWidth(100);
        colCantidad.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cantidad"));

        tblProductos.getColumns().addAll(colCodigo, colDescripcion, colCantidad);

        ObservableList<Inventario> listaProductos = FXCollections.observableArrayList(productos);
        FilteredList<Inventario> productosFiltrados = new FilteredList<>(listaProductos, p -> true);
        tblProductos.setItems(productosFiltrados);

        Spinner<Integer> spnCantidad = new Spinner<>(1, 1, 1);
        spnCantidad.setEditable(true);
        spnCantidad.setPrefWidth(100);

        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                int max = Math.max(1, sel.getCantidad());
                spnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, Math.min(1, max)));
            }
        });
        if (!listaProductos.isEmpty()) {
            tblProductos.getSelectionModel().selectFirst();
        }

        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                productosFiltrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                productosFiltrados.setPredicate(inv -> {
                    String searchStr = (inv.getCodigo() + " " + inv.getDescripcion() + " " + inv.getGrupo() + " " + inv.getMarca()).toLowerCase();
                    return searchStr.contains(texto);
                });
            }
        });

        tblProductos.setRowFactory(tv -> {
            TableRow<Inventario> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Inventario sel = row.getItem();
                    int cant = spnCantidad.getValue() != null ? spnCantidad.getValue() : 1;
                    dialog.setResult(new AsignacionProducto(sel, cant));
                    dialog.close();
                }
            });
            return row;
        });

        HBox filaCantidad = new HBox(8, new Label("Stock a colocar:"), spnCantidad);
        filaCantidad.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, txtBuscar, tblProductos, filaCantidad);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAsignar) {
                Inventario sel = tblProductos.getSelectionModel().getSelectedItem();
                if (sel == null) return null;
                int cant = spnCantidad.getValue() != null ? spnCantidad.getValue() : 1;
                return new AsignacionProducto(sel, cant);
            }
            return null;
        });

        Optional<AsignacionProducto> result = dialog.showAndWait();
        result.ifPresent(a -> {
            try {
                ubicacionDAO.ocupar(u.getId(), a.producto.getId(), a.cantidad);
                cargarSecciones(cmbPerchero.getValue());
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "mostrarDialogoAsignarProducto", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al asignar producto: " + e.getMessage()).showAndWait();
            }
        });
    }

    private void mostrarDialogoDetalleOcupado(UbicacionDetalle u) {
        Inventario producto = inventarioDAO.obtenerPorId(u.getIdProducto());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ubicación Ocupada");
        alert.setHeaderText(u.getCodigoUbicacion() + " — " + (producto != null ? producto.getDescripcion() : u.getProductoDescripcion()));

        StringBuilder sb = new StringBuilder();
        sb.append("=== Ubicación ===\n");
        sb.append("Perchero: ").append(u.getNombrePerchero()).append("\n");
        sb.append("Sección: ").append(u.getSeccion()).append("\n");
        sb.append("Estado: OCUPADO\n\n");

        if (producto != null) {
            sb.append("=== Producto ===\n");
            sb.append("Código: ").append(nvl(producto.getCodigo())).append("\n");
            sb.append("Descripción: ").append(nvl(producto.getDescripcion())).append("\n");
            sb.append("Grupo: ").append(nvl(producto.getGrupo())).append("\n");
            sb.append("Marca: ").append(nvl(producto.getMarca())).append("\n");
            sb.append("Proveedor: ").append(nvl(producto.getProveedor())).append("\n");
            sb.append("Ubicación Percha: ").append(nvl(producto.getUbicacionPercha())).append("\n");
            sb.append("Costo sin IVA: $").append(producto.getCostoSinIVA() != null ? producto.getCostoSinIVA() : "0").append("\n");
            sb.append("Precio Venta: $").append(producto.getPrecioVenta() != null ? producto.getPrecioVenta() : "0").append("\n");
            sb.append("Stock: ").append(producto.getCantidad()).append("\n");
            sb.append("Stock en este lugar: ").append(u.getStockAsignado() != null ? u.getStockAsignado() : 0).append("\n");
            sb.append("Fecha Ingreso: ").append(producto.getFecha_ingreso() != null ? producto.getFecha_ingreso().toLocalDate() : "-").append("\n");
            sb.append("Forma Pago: ").append(nvl(producto.getFormaPago())).append("\n");
            sb.append("Estado: ").append(Boolean.TRUE.equals(producto.getEstado()) ? "Activo" : "Inactivo").append("\n");
            if ("TAG Crédito".equals(producto.getFormaPago())) {
                sb.append("Meses Plazo: ").append(producto.getMesesPlazo()).append("\n");
                sb.append("Interés: ").append(producto.getInteres() != null ? producto.getInteres() : "0").append("%\n");
            }
        } else {
            sb.append("Producto no encontrado en inventario (ID: ").append(u.getIdProducto()).append(")");
        }

        alert.setContentText(sb.toString());

        ButtonType btnLiberar = new ButtonType("Liberar Ubicación", ButtonBar.ButtonData.LEFT);
        ButtonType btnEliminarLugar = new ButtonType("Eliminar Lugar", ButtonBar.ButtonData.LEFT);
        ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnLiberar, btnEliminarLugar, btnCerrar);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            try {
                if (result.get() == btnLiberar) {
                    ubicacionDAO.liberar(u.getId());
                    cargarSecciones(cmbPerchero.getValue());
                } else if (result.get() == btnEliminarLugar) {
                    eliminarLugar(u);
                }
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "mostrarDialogoDetalleOcupado", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
            }
        }
    }

    private String nvl(String s) {
        return s != null && !s.isEmpty() ? s : "-";
    }

    @FXML
    private void mostrarFormNuevo() {
        Dialog<Perchero> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Perchero");
        dialog.setHeaderText("Crear un nuevo perchero");

        ButtonType btnGuardar = new ButtonType("Generar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: A, B, C, ...");
        TextField txtSeccion = new TextField();
        txtSeccion.setPromptText("Ej: A, B, 1, 2, ...");
        Spinner<Integer> spnLugares = new Spinner<>(1, 100, 15);

        grid.add(new Label("Nombre Perchero:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Sección:"), 0, 1);
        grid.add(txtSeccion, 1, 1);
        grid.add(new Label("Cant. Lugares:"), 0, 2);
        grid.add(spnLugares, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                if (txtNombre.getText().trim().isEmpty() || txtSeccion.getText().trim().isEmpty()) {
                    return null;
                }
                Perchero p = new Perchero();
                p.setNombrePerchero(txtNombre.getText().trim().toUpperCase());
                p.setSeccion(txtSeccion.getText().trim().toUpperCase());
                p.setCantidadLugares(spnLugares.getValue());
                p.setEstado(true);
                return p;
            }
            return null;
        });

        Optional<Perchero> result = dialog.showAndWait();
        result.ifPresent(p -> {
            try {
                percheroDAO.guardar(p);
                Perchero creado = buscarPerchero(p.getNombrePerchero(), p.getSeccion());
                if (creado != null) {
                    String prefijo = creado.getNombrePerchero() + creado.getSeccion();
                    ubicacionDAO.generarUbicaciones(creado.getId(), prefijo, creado.getCantidadLugares());
                }
                cargarNombresPerchero();
                cmbPerchero.getSelectionModel().select(p.getNombrePerchero());
                seleccionarPerchero();
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "mostrarFormNuevo", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al crear perchero: " + e.getMessage()).showAndWait();
            }
        });
    }

    @FXML
    private void mostrarFormNuevaSeccion() {
        String nombrePerchero = cmbPerchero.getValue();
        if (nombrePerchero == null) return;

        Dialog<Perchero> dialog = new Dialog<>();
        dialog.setTitle("Nueva Sección");
        dialog.setHeaderText("Agregar sección a Perchero " + nombrePerchero);

        ButtonType btnGuardar = new ButtonType("Generar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label lblNombre = new Label(nombrePerchero);
        lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        TextField txtSeccion = new TextField();
        txtSeccion.setPromptText("Ej: B, C, 2, 3, ...");
        Spinner<Integer> spnLugares = new Spinner<>(1, 100, 15);

        grid.add(new Label("Perchero:"), 0, 0);
        grid.add(lblNombre, 1, 0);
        grid.add(new Label("Sección:"), 0, 1);
        grid.add(txtSeccion, 1, 1);
        grid.add(new Label("Cant. Lugares:"), 0, 2);
        grid.add(spnLugares, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                if (txtSeccion.getText().trim().isEmpty()) return null;
                Perchero p = new Perchero();
                p.setNombrePerchero(nombrePerchero);
                p.setSeccion(txtSeccion.getText().trim().toUpperCase());
                p.setCantidadLugares(spnLugares.getValue());
                p.setEstado(true);
                return p;
            }
            return null;
        });

        Optional<Perchero> result = dialog.showAndWait();
        result.ifPresent(p -> {
            try {
                percheroDAO.guardar(p);
                Perchero creado = buscarPerchero(p.getNombrePerchero(), p.getSeccion());
                if (creado != null) {
                    String prefijo = creado.getNombrePerchero() + creado.getSeccion();
                    ubicacionDAO.generarUbicaciones(creado.getId(), prefijo, creado.getCantidadLugares());
                }
                cargarSecciones(nombrePerchero);
            } catch (Exception e) {
                logDAO.guardar("UbicacionPercheroController", "mostrarFormNuevaSeccion", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al crear sección: " + e.getMessage()).showAndWait();
            }
        });
    }

    public UbicacionDetalle getUbicacionSeleccionada() {
        return ubicacionSeleccionada;
    }

    private record AsignacionProducto(Inventario producto, int cantidad) {}
}
