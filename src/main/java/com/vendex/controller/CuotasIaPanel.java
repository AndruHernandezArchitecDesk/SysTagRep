package com.vendex.controller;

import com.vendex.dao.CuotaIADAO;
import com.vendex.model.CuotaIA.*;
import com.vendex.service.GeminiCuotas;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/** Vista administrativa; todo acceso JDBC/REST se ejecuta fuera del hilo JavaFX. */
public class CuotasIaPanel extends VBox {
    private final CuotaIADAO dao = GeminiCuotas.dao();
    private final TableView<Estado> modelos = new TableView<>();
    private final TableView<Intento> intentos = new TableView<>();
    private final ComboBox<String> nivel = new ComboBox<>();
    private final TextField rpm = new TextField(), tpm = new TextField(), rpd = new TextField(), salida = new TextField();
    private final TextField precioEntrada = new TextField(), precioSalida = new TextField();
    private final CheckBox habilitado = new CheckBox("Habilitado"), ahorro = new CheckBox("Ahorrar redacción");
    private final Label estado = new Label();
    private final Button guardar = new Button("Guardar modelo"), actualizar = new Button("Actualizar cuotas");
    private final ComboBox<String> filtroModelo = new ComboBox<>();
    private final CheckBox soloErrores = new CheckBox("Solo fallos");
    private final DatePicker desde = new DatePicker(), hasta = new DatePicker();
    private List<Intento> registros = List.of();
    private boolean ocupado;

    public CuotasIaPanel() {
        setSpacing(8);
        Label nota = new Label("Control compartido entre equipos de la misma base/servidor (un proyecto Gemini). "
                + "Uso observado por Vendex, no saldo oficial de Google. 0 = límite sin configurar. "
                + "TPM usa una estimación conservadora de entrada por bytes UTF-8. RPD reinicia a medianoche de Los Ángeles.");
        nota.setWrapText(true);
        Hyperlink google = new Hyperlink("Ver cuotas oficiales: " + URL_CUOTAS);
        google.setOnAction(e -> abrirCuotasOficiales(google));
        Button copiar = new Button("Copiar enlace");
        copiar.setOnAction(e -> {
            // Alternativa si el navegador no responde: nunca depende de la integración del SO.
            var contenido = new javafx.scene.input.ClipboardContent();
            contenido.putString(URL_CUOTAS);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(contenido);
            estado.setText("Enlace copiado: " + URL_CUOTAS);
        });
        columna(modelos, "Modelo", e -> e.config().modelo(), 195);
        columna(modelos, "Nivel", e -> e.config().nivel(), 75);
        columna(modelos, "RPM uso / límite", e -> e.minuto() + " / " + limite(e.config().rpm()), 115);
        columna(modelos, "RPD uso / límite", e -> e.dia() + " / " + limite(e.config().rpd()), 115);
        columna(modelos, "TPM estim. / límite", e -> e.tokensMinuto() + " / " + limite(e.config().tpm()), 140);
        columna(modelos, "Estado / pausa hasta", e -> !e.config().habilitado() ? "Deshabilitado"
                : e.pausaHasta() > System.currentTimeMillis() ? "Pausa: " + fecha(e.pausaHasta()) : "Disponible", 200);
        modelos.setPrefHeight(165);
        // Altura mínima: sin ella el VBox puede comprimir la tabla hasta mostrar solo encabezado.
        modelos.setMinHeight(130);
        modelos.setPlaceholder(new Label("Sin modelos disponibles. Verifica la migración V41 en el servidor."));
        modelos.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> editar(b));
        nivel.getItems().setAll("Gratuito", "Pago");
        for (TextField campo : List.of(rpm, tpm, rpd, salida, precioEntrada, precioSalida)) campo.setPrefColumnCount(6);
        precioEntrada.setPromptText("Sin tarifa"); precioSalida.setPromptText("Sin tarifa");
        FlowPane editor = new FlowPane(8, 6, new Label("Modelo seleccionado:"), nivel, habilitado, ahorro,
                new Label("RPM"), rpm, new Label("TPM"), tpm, new Label("RPD"), rpd, new Label("Máx. salida"), salida,
                new Label("USD/1M entrada"), precioEntrada, new Label("USD/1M salida + razonamiento"), precioSalida, guardar);
        guardar.setOnAction(e -> guardar()); actualizar.setOnAction(e -> actualizar());
        filtroModelo.getItems().add("Todos los modelos"); filtroModelo.getItems().addAll(com.vendex.model.CuotaIA.MODELOS);
        filtroModelo.getSelectionModel().selectFirst();
        filtroModelo.setOnAction(e -> filtrar()); soloErrores.setOnAction(e -> filtrar());
        desde.setPromptText("Desde"); hasta.setPromptText("Hasta");
        desde.setOnAction(e -> filtrar()); hasta.setOnAction(e -> filtrar());
        columna(intentos, "Fecha", i -> fecha(i.fecha()), 145);
        columna(intentos, "Modelo", Intento::modelo, 180);
        columna(intentos, "Operación", Intento::operacion, 145);
        columna(intentos, "HTTP", i -> i.http() == -1 ? "Pendiente / sin cierre" : i.http() == 0 ? "Sin respuesta" : "" + i.http(), 130);
        columna(intentos, "ms", i -> "" + i.duracion(), 65);
        columna(intentos, "Entrada", i -> "" + i.entrada(), 70);
        columna(intentos, "Respuesta", i -> "" + i.salida(), 75);
        columna(intentos, "Razonamiento", i -> "" + i.pensamiento(), 100);
        columna(intentos, "Total", i -> "" + i.total(), 70);
        columna(intentos, "USD estimado*", i -> i.costo() == null ? "Sin tarifa" : i.costo().toPlainString(), 115);
        columna(intentos, "Detalle", Intento::detalle, 300);
        intentos.setPlaceholder(new Label("Sin solicitudes con estos filtros."));
        intentos.setMinHeight(140);
        VBox contenido = new VBox(8, nota, new HBox(8, actualizar, google, copiar), estado,
                modelos, editor,
                new Label("Últimas 200 solicitudes compartidas. *Costo por modelo: excluye búsqueda web y descuentos de caché; tarifas manuales."),
                new FlowPane(8, 6, filtroModelo, soloErrores, desde, hasta), intentos);
        // En ventanas bajas el panel se desplaza en lugar de comprimir las filas.
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        actualizar();
    }
    private static final String URL_CUOTAS = "https://ai.dev/rate-limit";

    /**
     * La apertura se delega a {@link Navegador}, que trabaja fuera del hilo JavaFX,
     * con tiempo de espera y cierre del proceso si el navegador no responde.
     */
    private void abrirCuotasOficiales(Hyperlink enlace) {
        enlace.setDisable(true);
        estado.setText("Abriendo " + URL_CUOTAS + "…");
        boolean iniciado = com.vendex.util.Navegador.abrir(URL_CUOTAS, mensaje ->
                Platform.runLater(() -> {
                    enlace.setDisable(false);
                    estado.setText(mensaje);
                }));
        if (!iniciado) enlace.setDisable(false);
    }

    private static String cortar(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static String limite(long n) { return n == 0 ? "—" : Long.toString(n); }
    private static String fecha(long ms) {
        return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    private static <T> void columna(TableView<T> tabla, String nombre, Function<T, String> valor, double ancho) {
        TableColumn<T, String> col = new TableColumn<>(nombre);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue()))); col.setPrefWidth(ancho);
        tabla.getColumns().add(col);
    }
    private void editar(Estado e) {
        if (e == null) return;
        Config c = e.config(); nivel.setValue(c.nivel()); habilitado.setSelected(c.habilitado()); ahorro.setSelected(c.ahorro());
        rpm.setText("" + c.rpm()); tpm.setText("" + c.tpm()); rpd.setText("" + c.rpd()); salida.setText("" + c.maxSalida());
        precioEntrada.setText(c.precioEntrada() == null ? "" : c.precioEntrada().toPlainString());
        precioSalida.setText(c.precioSalida() == null ? "" : c.precioSalida().toPlainString());
    }
    private void filtrar() {
        intentos.getItems().setAll(registros.stream().filter(i -> filtroModelo.getSelectionModel().getSelectedIndex() == 0
                || i.modelo().equals(filtroModelo.getValue())).filter(i -> !soloErrores.isSelected() || i.http() != 200)
                .filter(i -> desde.getValue() == null || !dia(i.fecha()).isBefore(desde.getValue()))
                .filter(i -> hasta.getValue() == null || !dia(i.fecha()).isAfter(hasta.getValue())).toList());
    }
    private static LocalDate dia(long ms) { return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate(); }
    private void guardar() {
        if (!com.vendex.util.SesionActual.tienePermiso("USUARIO_GESTIONAR")) { estado.setText("Se requiere permiso de administrador."); return; }
        Estado elegido = modelos.getSelectionModel().getSelectedItem(); if (elegido == null || ocupado) return;
        try {
            Config cfg = new Config(elegido.config().modelo(), nivel.getValue(), habilitado.isSelected(),
                    Long.parseLong(rpm.getText().trim()), Long.parseLong(tpm.getText().trim()), Long.parseLong(rpd.getText().trim()),
                    Integer.parseInt(salida.getText().trim()), ahorro.isSelected(), precio(precioEntrada), precio(precioSalida));
            cfg.validar(); ejecutar(() -> { dao.guardar(cfg); return dao.panel(); }, "Configuración guardada para todos los equipos.");
        } catch (IllegalArgumentException ex) { estado.setText("Revisa los límites (enteros >= 0) y salida (256–8192)."); }
    }
    public void actualizar() { if (!ocupado) ejecutar(dao::panel, "Actualizado. Las pausas no se eliminan al guardar ni al renovar la licencia."); }
    private static java.math.BigDecimal precio(TextField campo) {
        return campo.getText().isBlank() ? null : new java.math.BigDecimal(campo.getText().trim());
    }
    private interface Carga { Panel cargar() throws Exception; }
    private void ejecutar(Carga carga, String mensaje) {
        ocupado = true; guardar.setDisable(true); actualizar.setDisable(true); estado.setText("Consultando servidor…");
        String seleccionado = modelos.getSelectionModel().getSelectedItem() == null ? "" : modelos.getSelectionModel().getSelectedItem().config().modelo();
        Thread hilo = new Thread(() -> {
            try {
                Panel p = carga.cargar();
                Platform.runLater(() -> {
                    modelos.getItems().setAll(p.modelos());
                    registros = p.intentos();
                    filtrar();
                    if (p.modelos().isEmpty()) {
                        estado.setText("No hay modelos configurados. Ejecuta la migración V41 en la base compartida.");
                    } else {
                        p.modelos().stream().filter(e -> e.config().modelo().equals(seleccionado)).findFirst()
                                .ifPresentOrElse(modelos.getSelectionModel()::select,
                                        () -> modelos.getSelectionModel().selectFirst());
                        estado.setText(mensaje);
                    }
                    terminar();
                });
            } catch (Exception ex) {
                // Conserva la causa real (migración, permisos o conexión) junto al aviso visible.
                String causa = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                java.util.logging.Logger.getLogger(CuotasIaPanel.class.getName())
                        .log(java.util.logging.Level.WARNING, "No se pudo cargar control de cuotas IA", ex);
                Platform.runLater(() -> {
                    estado.setText("Control central no disponible: " + cortar(causa, 140)
                            + " — revisa conexión, permisos y migración V41 en el servidor.");
                    terminar();
                });
            }
        }, "cuotas-ia-panel"); hilo.setDaemon(true); hilo.start();
    }
    private void terminar() { ocupado = false; guardar.setDisable(false); actualizar.setDisable(false); }
}
