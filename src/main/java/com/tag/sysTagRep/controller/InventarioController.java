package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.UbicacionDetalleDAO;
import com.tag.sysTagRep.model.Empresa;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.UbicacionDetalle;
import com.tag.sysTagRep.util.EtiquetaUtil;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Pantalla de Inventario: tabla con todo el inventario, búsqueda, paginación,
 * y acciones de editar (abre Ingreso de Mercadería) y eliminar.
 */
public class InventarioController implements Initializable {

    @FXML private TableView<Inventario> tblInventario;
    @FXML private TableColumn<Inventario, String> colDescripcion;
    @FXML private TableColumn<Inventario, String> colGrupo;
    @FXML private TableColumn<Inventario, String> colMarca;
    @FXML private TableColumn<Inventario, BigDecimal> colCostoSinIva;
    @FXML private TableColumn<Inventario, Integer> colCantidad;
    @FXML private TableColumn<Inventario, String> colUbicacionPercha;
    @FXML private TableColumn<Inventario, BigDecimal> colPrecioVenta;
    @FXML private TableColumn<Inventario, LocalDateTime> colFechaIngreso;
    @FXML private TableColumn<Inventario, Boolean> colEstado;
    @FXML private TableColumn<Inventario, String> colCodigo;
    @FXML private TableColumn<Inventario, String> colCodigoManual;
    @FXML private TableColumn<Inventario, String> colNumeroFactura;
    @FXML private TableColumn<Inventario, String> colProveedor;
    @FXML private TableColumn<Inventario, Void> colAcciones;

    @FXML private TextField txtBuscar;
    @FXML private TextField txtBuscarFactura;
    @FXML private Label lblPaginaInfo;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private ComboBox<Integer> cmbPageSize;

    private int currentPage = 1;
    private int pageSize = 25;
    private int totalPages = 1;
    private int totalCount = 0;
    private final InventarioDAO dao = new InventarioDAO();
    private final com.tag.sysTagRep.dao.LogDAO logDAO = new com.tag.sysTagRep.dao.LogDAO();
    private final EmpresaDAO daoEmpresa = new EmpresaDAO();
    private final UbicacionDetalleDAO daoUbicacion = new UbicacionDetalleDAO();
    private final ObservableList<Inventario> listaInventario = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        iniciarTablaContenido();
        cargarAcciones();
        iniciarPageSize();
        cargarDatos();
        txtBuscar.textProperty().addListener((obs, old, val) -> { currentPage = 1; cargarDatos(); });
        txtBuscarFactura.textProperty().addListener((obs, old, val) -> { currentPage = 1; cargarDatos(); });
    }

    private void cargarDatos() {
        String filtro = txtBuscar.getText();
        String numeroFactura = txtBuscarFactura.getText();
        totalCount = dao.contar(filtro, numeroFactura);
        totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        listaInventario.setAll(dao.listarPaginado(currentPage, pageSize, filtro, numeroFactura));
        tblInventario.setItems(listaInventario);
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

    private void actualizarPaginaInfo() {
        lblPaginaInfo.setText("Página " + currentPage + " de " + totalPages + " (" + totalCount + " registros)");
        btnAnterior.setDisable(currentPage <= 1);
        btnSiguiente.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void nuevoRegistro() {
        abrirModalEdicion(null);
    }

    private void abrirModalEdicion(Inventario i) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/IngresoMercaderiaView.fxml"));
            Parent vista = loader.load();
            IngresoMercaderiaController controller = loader.getController();
            controller.setCerrarAlGuardar(true);
            if (i != null) {
                controller.cargarParaEdicion(i);
            }
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(i != null ? "Editar Producto" : "Ingreso de Factura");
            modal.setScene(new Scene(vista, 820, 720));
            modal.showAndWait();
            cargarDatos();
        } catch (Exception e) {
            logDAO.guardar("InventarioController", "abrirModalEdicion", e.getMessage(), e);
        }
    }

    private void eliminar(Inventario i) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el artículo seleccionado?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            dao.eliminar(i.getId());
            cargarDatos();
        }
    }

    private void iniciarTablaContenido(){
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCostoSinIva.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colUbicacionPercha.setCellValueFactory(new PropertyValueFactory<>("ubicacionPercha"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colFechaIngreso.setCellValueFactory(new PropertyValueFactory<>("fecha_ingreso"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("tagCodigo"));
        colCodigoManual.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNumeroFactura.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        SortTable.agregarBotones(tblInventario);
    }

    private void cargarAcciones(){
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnActualizar = new Button();
            private final Button btnEliminar = new Button();
            private final Button btnEtiqueta = new Button();
            private final FontIcon iconEtiqueta = new FontIcon(FontAwesomeSolid.TAG);
            private final HBox hbox = new HBox(10);
            {
                FontIcon iconEdit = new FontIcon(FontAwesomeSolid.EDIT); iconEdit.setIconSize(16); iconEdit.setIconColor(Color.DODGERBLUE);
                FontIcon iconTrash = new FontIcon(FontAwesomeSolid.TRASH); iconTrash.setIconSize(16); iconTrash.setIconColor(Color.RED);
                iconEtiqueta.setIconSize(16); iconEtiqueta.setIconColor(Color.MEDIUMSEAGREEN);
                btnActualizar.setGraphic(iconEdit); btnEliminar.setGraphic(iconTrash); btnEtiqueta.setGraphic(iconEtiqueta);
                btnActualizar.setStyle("-fx-background-color: transparent;"); btnEliminar.setStyle("-fx-background-color: transparent;"); btnEtiqueta.setStyle("-fx-background-color: transparent;");
                hbox.setAlignment(Pos.CENTER); hbox.getChildren().addAll(btnActualizar, btnEliminar, btnEtiqueta);
                btnActualizar.setOnAction(e -> abrirModalEdicion(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminar(getTableView().getItems().get(getIndex())));
                btnEtiqueta.setOnAction(e -> generarEtiqueta(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        Inventario inv = getTableView().getItems().get(idx);
                        boolean ubicado = inv.getUbicacionPercha() != null && !inv.getUbicacionPercha().trim().isEmpty();
                        iconEtiqueta.setIconColor(ubicado ? Color.MEDIUMSEAGREEN : Color.RED);
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    private void generarEtiqueta(Inventario item) {
        List<UbicacionDetalle> ubicaciones = daoUbicacion.listarPorProducto(item.getId());
        if (ubicaciones.isEmpty()) {
            generarEtiquetasBulk(item, null);
            return;
        }
        if (ubicaciones.size() == 1) {
            UbicacionDetalle u = ubicaciones.get(0);
            int total = u.getStockAsignado() != null ? u.getStockAsignado() : 1;
            if (total <= 1) { generarEtiquetaDirecta(item, u); return; }
            // 1 ubicacion con N stock -> dialogo con opcion N copias
            mostrarDialogoUbicaciones(item, ubicaciones);
            return;
        }
        mostrarDialogoUbicaciones(item, ubicaciones);
    }

    private void mostrarDialogoUbicaciones(Inventario item, List<UbicacionDetalle> ubicaciones) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Etiquetas por ubicación - " + item.getCodigo());
        dialog.setHeaderText(item.getDescripcion() + " (" + item.getCantidad() + " uds) en " + ubicaciones.size() + " ubicaciones. Genere 1 o N copias 1/2/3... por stock.");
        ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(btnCerrar);
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setPadding(new javafx.geometry.Insets(12));
        content.setPrefWidth(560);
        Label lblInfo = new Label("Código: " + item.getCodigo() + " | Mismo barcode en todas. Cada copia diferenciada con 1/2/3 arriba a la derecha. Cada etiqueta va pegada a 1 producto.");
        lblInfo.setWrapText(true);
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        content.getChildren().add(lblInfo);
        for (UbicacionDetalle u : ubicaciones) {
            int stock = u.getStockAsignado() != null ? u.getStockAsignado() : 0;
            javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(10);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-padding: 8;");
            javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(2);
            Label lblUbi = new Label(u.getCodigoUbicacion() + "  (Stock: " + stock + "/" + item.getCantidad() + ")");
            lblUbi.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label lblDet = new Label("Perchero " + u.getNombrePerchero() + " - Sec " + u.getSeccion() + (u.getMarcaNombre()!=null?" | "+u.getMarcaNombre():""));
            lblDet.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
            info.getChildren().addAll(lblUbi, lblDet);
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            Button btn1 = new Button("1 etiqueta");
            btn1.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btn1.setOnAction(e -> generarEtiquetaDirecta(item, u));
            Button btnN = new Button(stock + " etiquetas (1.."+stock+")");
            btnN.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnN.setDisable(stock <= 0);
            btnN.setOnAction(e -> generarEtiquetasBulk(item, u));
            fila.getChildren().addAll(info, spacer, btn1, btnN);
            content.getChildren().add(fila);
        }
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(Math.min(420, 90 + ubicaciones.size()*78));
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void generarEtiquetasBulk(Inventario item, UbicacionDetalle ubicacion) {
        new Thread(() -> {
            try {
                List<Empresa> empresas = daoEmpresa.listar();
                String razonSocial = empresas.isEmpty() ? "Tag Repuestos" : empresas.get(0).getRazonSocial();
                List<File> archivos;
                if (ubicacion == null) {
                    // sin ubicacion: repetir por item.cantidad
                    int total = Math.max(1, item.getCantidad());
                    archivos = new java.util.ArrayList<>();
                    for (int i=1;i<=total;i++) {
                        // crear UbicacionDetalle ficticio para diferenciar numeracion visual 1/Total
                        UbicacionDetalle fake = new UbicacionDetalle();
                        fake.setCodigoUbicacion(item.getUbicacionPercha()!=null && !item.getUbicacionPercha().isBlank()? item.getUbicacionPercha().split(",")[0].trim() : "SIN-UBIC");
                        fake.setStockAsignado(total);
                        // usar metodo con indice para generar etiqueta numerada
                        File f = EtiquetaUtil.generarEtiquetasPorUbicacion(item, fake, "/img/logoTag.jpeg", razonSocial).get(i-1);
                        archivos.add(f);
                        if (archivos.size() >= total) break;
                    }
                    // fallback si la logica anterior dupla: si no hay ubicacion usa generarEtiqueta simple repetida con indice
                    if (archivos.size()!=total) {
                        archivos.clear();
                        for(int i=1;i<=total;i++){
                            UbicacionDetalle fu = new UbicacionDetalle();
                            fu.setCodigoUbicacion("SIN-UBIC");
                            fu.setStockAsignado(total);
                            archivos.addAll(EtiquetaUtil.generarEtiquetasPorUbicacion(item, fu, "/img/logoTag.jpeg", razonSocial).subList(i-1,i));
                            if(archivos.size()>=total) break;
                        }
                    }
                } else {
                    archivos = EtiquetaUtil.generarEtiquetasPorUbicacion(item, ubicacion, "/img/logoTag.jpeg", razonSocial);
                }
                if (archivos != null && !archivos.isEmpty()) {
                    String msg = archivos.size() + " etiqueta(s) generada(s) en:\n" + archivos.get(0).getParentFile().getAbsolutePath() + "\n" + archivos.get(0).getName() + (archivos.size()>1 ? " ... (+"+(archivos.size()-1)+")":"");
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(archivos.size() + " etiquetas generadas");
                        alert.setHeaderText(null);
                        alert.setContentText(msg);
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logDAO.guardar("InventarioController", "generarEtiquetasBulk", e.getMessage(), e);
            }
        }).start();
    }

    private void generarEtiquetaDirecta(Inventario item, UbicacionDetalle ubicacion) {
        new Thread(() -> {
            try {
                List<Empresa> empresas = daoEmpresa.listar();
                String razonSocial = empresas.isEmpty() ? "Tag Repuestos" : empresas.get(0).getRazonSocial();
                File etiqueta = (ubicacion == null)
                        ? EtiquetaUtil.generarEtiqueta(item, "/img/logoTag.jpeg", razonSocial)
                        : EtiquetaUtil.generarEtiquetaUbicacion(item, ubicacion, "/img/logoTag.jpeg", razonSocial);
                if (etiqueta != null && etiqueta.exists()) {
                    String msg = "Etiqueta generada en:\n" + etiqueta.getAbsolutePath();
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Etiqueta generada");
                        alert.setHeaderText(null);
                        alert.setContentText(msg);
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logDAO.guardar("InventarioController", "generarEtiquetaDirecta", e.getMessage(), e);
            }
        }).start();
    }
}
