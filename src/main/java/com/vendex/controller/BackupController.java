package com.vendex.controller;

import com.vendex.backup.BackupConfig;
import com.vendex.backup.BackupPassphraseManager;
import com.vendex.backup.BackupResult;
import com.vendex.backup.BackupService;
import com.vendex.dao.LogDAO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.ResourceBundle;

public class BackupController implements Initializable {

    @FXML private TextField txtDirLocal;
    @FXML private TextField txtOffsite;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPgDump;
    @FXML private TextField txtGpg;
    @FXML private CheckBox chkCifrado;
    @FXML private TextField txtRetentionDaily;
    @FXML private TextField txtRetentionWeekly;
    @FXML private TextField txtRetentionMonthly;

    @FXML private Button btnGuardar;
    @FXML private Button btnRespaldarAhora;
    @FXML private Button btnGenerarPassphrase;
    @FXML private Label lblPassphraseEstado;
    @FXML private Label lblUltimoBackup;
    @FXML private TextArea txtLog;

    @FXML private TableView<FileRow> tblBackups;
    @FXML private TableColumn<FileRow, String> colNombre;
    @FXML private TableColumn<FileRow, String> colFecha;
    @FXML private TableColumn<FileRow, String> colTam;
    @FXML private TableColumn<FileRow, String> colEstado;

    private final LogDAO logDAO = new LogDAO();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static class FileRow {
        private final String nombre;
        private final String fecha;
        private final String tam;
        private final String estado;
        public FileRow(String n, String f, String t, String e) { this.nombre=n; this.fecha=f; this.tam=t; this.estado=e; }
        public String getNombre() { return nombre; }
        public String getFecha() { return fecha; }
        public String getTam() { return tam; }
        public String getEstado() { return estado; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabla();
        cargarConfig();
        actualizarEstadoPassphrase();
        cargarListaBackups();
        log("Listo. Configure ruta offsite \\\\OTRA-PC\\VendexBackups y destino " + txtEmail.getText());
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTam.setCellValueFactory(new PropertyValueFactory<>("tam"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        tblBackups.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarConfig() {
        Properties p = BackupConfig.cargar();
        txtDirLocal.setText(p.getProperty("backup.dir", ""));
        txtOffsite.setText(p.getProperty("backup.offsite.path", ""));
        txtEmail.setText(p.getProperty("backup.email.destino", "andresrockfull@gmail.com"));
        txtPgDump.setText(p.getProperty("backup.pg_dump.path", "pg_dump"));
        txtGpg.setText(p.getProperty("backup.gpg.path", "gpg"));
        chkCifrado.setSelected(Boolean.parseBoolean(p.getProperty("backup.cifrado", "true")));
        txtRetentionDaily.setText(p.getProperty("backup.retention.daily", "7"));
        txtRetentionWeekly.setText(p.getProperty("backup.retention.weekly", "4"));
        txtRetentionMonthly.setText(p.getProperty("backup.retention.monthly", "12"));
    }

    @FXML
    private void guardarConfig() {
        try {
            Properties p = BackupConfig.cargar();
            p.setProperty("backup.dir", txtDirLocal.getText().trim());
            p.setProperty("backup.offsite.path", txtOffsite.getText().trim());
            p.setProperty("backup.email.destino", txtEmail.getText().trim());
            p.setProperty("backup.pg_dump.path", txtPgDump.getText().trim().isEmpty() ? "pg_dump" : txtPgDump.getText().trim());
            p.setProperty("backup.gpg.path", txtGpg.getText().trim().isEmpty() ? "gpg" : txtGpg.getText().trim());
            p.setProperty("backup.cifrado", String.valueOf(chkCifrado.isSelected()));
            p.setProperty("backup.retention.daily", txtRetentionDaily.getText().trim());
            p.setProperty("backup.retention.weekly", txtRetentionWeekly.getText().trim());
            p.setProperty("backup.retention.monthly", txtRetentionMonthly.getText().trim());
            BackupConfig.guardar(p);
            log("Configuración guardada en ~/.vendex/backup.properties");
            new Alert(Alert.AlertType.INFORMATION, "Configuración guardada correctamente.").showAndWait();
            cargarListaBackups();
        } catch (Exception e) {
            log("Error guardando config: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void generarPassphrase() {
        try {
            String nueva = BackupPassphraseManager.generarYGuardar();
            log("Passphrase generada y guardada (Tier1 keyring/fallback). Longitud " + nueva.length());
            actualizarEstadoPassphrase();
            new Alert(Alert.AlertType.INFORMATION, "Passphrase de cifrado generada y guardada de forma segura (keyring).").showAndWait();
        } catch (Exception e) {
            log("Error generando passphrase: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    private void actualizarEstadoPassphrase() {
        boolean existe = BackupPassphraseManager.existe();
        lblPassphraseEstado.setText(existe ? "✓ Passphrase configurada (cifrado GPG activo)" : "✗ Sin passphrase — genere una para activar cifrado");
        lblPassphraseEstado.setStyle(existe ? "-fx-text-fill: green; -fx-font-weight: bold;" : "-fx-text-fill: #cc6600; -fx-font-weight: bold;");
    }

    @FXML
    private void respaldarAhora() {
        // guardar config implícitamente
        guardarConfigSilencioso();
        btnRespaldarAhora.setDisable(true);
        log("Iniciando respaldo manual...");
        Task<BackupResult> task = new Task<>() {
            @Override protected BackupResult call() {
                return new BackupService().ejecutar();
            }
        };
        task.setOnSucceeded(e -> {
            BackupResult r = task.getValue();
            btnRespaldarAhora.setDisable(false);
            if (r.exito) {
                log("✓ " + r.mensaje + " -> " + (r.archivoLocal != null ? r.archivoLocal.getAbsolutePath() : ""));
                lblUltimoBackup.setText("Último: " + r.archivoLocal.getName() + " (" + formatSize(r.tamanioBytes) + ") " + (r.copiadoOffsite ? " + offsite OK" : " offsite=" + r.errorOffsite));
                new Alert(Alert.AlertType.INFORMATION, "Respaldo completado:\n" + r.mensaje).showAndWait();
                // enviar notificación éxito si habilitado; falla ya notifica internamente pero también avisamos
                // si offsite falló, advertir
                if (!r.copiadoOffsite) {
                    new Alert(Alert.AlertType.WARNING, "Respaldo local OK pero copia offsite falló:\n" + r.errorOffsite + "\nVerifique \\\\OTRA-PC\\VendexBackups accesible.").showAndWait();
                }
            } else {
                log("✗ FALLÓ: " + r.mensaje);
                lblUltimoBackup.setText("FALLÓ: " + r.mensaje);
                new com.vendex.backup.BackupNotificationService().notificarFallaConResultado(r);
                new Alert(Alert.AlertType.ERROR, "Respaldo falló:\n" + r.mensaje).showAndWait();
            }
            cargarListaBackups();
        });
        task.setOnFailed(e -> {
            btnRespaldarAhora.setDisable(false);
            Throwable ex = task.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            log("✗ Excepción: " + msg);
            new com.vendex.backup.BackupNotificationService().notificarFalla("Respaldo manual falló", ex);
            new Alert(Alert.AlertType.ERROR, "Error: " + msg).showAndWait();
            logDAO.guardar("BackupController", "respaldarAhora", msg, ex instanceof Exception ? (Exception)ex : new Exception(ex));
        });
        Thread t = new Thread(task, "vendex-backup-manual");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void probarOffsite() {
        String offsite = txtOffsite.getText().trim();
        if (offsite.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Configure primero la ruta offsite (ej. \\\\OTRA-PC\\VendexBackups)").showAndWait();
            return;
        }
        File dir = new File(offsite);
        if (dir.exists() && dir.isDirectory() && dir.canWrite()) {
            new Alert(Alert.AlertType.INFORMATION, "Offsite accesible y escribible:\n" + dir.getAbsolutePath()).showAndWait();
            log("Offsite OK: " + dir.getAbsolutePath());
        } else {
            // intentar crear
            boolean mk = dir.mkdirs();
            if (dir.exists()) {
                new Alert(Alert.AlertType.INFORMATION, "Offsite creado/accesible:\n" + dir.getAbsolutePath()).showAndWait();
                log("Offsite creado: " + dir.getAbsolutePath());
            } else {
                new Alert(Alert.AlertType.ERROR, "No se pudo acceder a offsite:\n" + offsite + "\nVerifique permisos de red y que la PC esté encendida.").showAndWait();
                log("Offsite NO accesible: " + offsite + " mk=" + mk);
            }
        }
    }

    @FXML
    private void refrescarLista() {
        cargarListaBackups();
    }

    private void guardarConfigSilencioso() {
        try {
            Properties p = BackupConfig.cargar();
            p.setProperty("backup.dir", txtDirLocal.getText().trim());
            p.setProperty("backup.offsite.path", txtOffsite.getText().trim());
            p.setProperty("backup.email.destino", txtEmail.getText().trim());
            p.setProperty("backup.pg_dump.path", txtPgDump.getText().trim().isEmpty() ? "pg_dump" : txtPgDump.getText().trim());
            p.setProperty("backup.gpg.path", txtGpg.getText().trim().isEmpty() ? "gpg" : txtGpg.getText().trim());
            p.setProperty("backup.cifrado", String.valueOf(chkCifrado.isSelected()));
            p.setProperty("backup.retention.daily", txtRetentionDaily.getText().trim());
            p.setProperty("backup.retention.weekly", txtRetentionWeekly.getText().trim());
            p.setProperty("backup.retention.monthly", txtRetentionMonthly.getText().trim());
            BackupConfig.guardar(p);
        } catch (Exception ignored) {}
    }

    private void cargarListaBackups() {
        try {
            String dirStr = txtDirLocal.getText().trim();
            if (dirStr.isEmpty()) dirStr = BackupConfig.getBackupDir();
            File dir = new File(dirStr);
            ObservableList<FileRow> rows = FXCollections.observableArrayList();
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d,n) -> n.startsWith("vendex_"));
                if (files != null) {
                    java.util.Arrays.sort(files, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));
                    for (File f : files) {
                        String nombre = f.getName();
                        String fecha = fmt.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault()));
                        String tam = formatSize(f.length());
                        String estado = f.getName().endsWith(".gpg") ? "Cifrado" : "Sin cifrar";
                        if (f.length() < 10*1024) estado += " (pequeño!)";
                        rows.add(new FileRow(nombre, fecha, tam, estado));
                        if (rows.size() >= 20) break;
                    }
                }
            }
            tblBackups.setItems(rows);
            if (rows.isEmpty()) {
                lblUltimoBackup.setText("Sin respaldos en: " + dirStr);
            } else if (lblUltimoBackup.getText().isEmpty() || lblUltimoBackup.getText().startsWith("Sin ")) {
                lblUltimoBackup.setText("Último: " + rows.get(0).getNombre() + " " + rows.get(0).getTam());
            }
        } catch (Exception e) {
            log("Error listando backups: " + e.getMessage());
        }
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLog.appendText("[" + ts + "] " + msg + "\n");
        });
    }

    private String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024*1024) return String.format("%.1f KB", b/1024.0);
        return String.format("%.2f MB", b/(1024.0*1024));
    }
}
