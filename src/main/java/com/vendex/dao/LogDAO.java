package com.vendex.dao;

import com.vendex.config.DatabaseConnection;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogDAO {

    private static final Logger LOGGER = Logger.getLogger(LogDAO.class.getName());

    public void guardar(String controlador, String metodo, String mensaje, Exception ex) {
        String stacktrace = "";
        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            stacktrace = sw.toString();
        }
        // Volcar a carpeta vendex_errors además de BD
        volcarAArchivo(controlador, metodo, mensaje, stacktrace);
        String sql = "INSERT INTO logs(controlador, metodo, mensaje, stacktrace, fecha) VALUES (?, ?, ?, ?, NOW())";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, controlador);
            ps.setString(2, metodo);
            ps.setString(3, mensaje);
            ps.setString(4, stacktrace);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar log en base de datos", e);
        }
    }

    private void volcarAArchivo(String controlador, String metodo, String mensaje, String stacktrace) {
        try {
            String home = System.getProperty("user.home");
            java.io.File dir = new java.io.File(home, "vendex_errors");
            // fallback a Escritorio si existe
            java.io.File escritorio = new java.io.File(home, "Escritorio");
            if (escritorio.exists() && escritorio.isDirectory()) {
                java.io.File alt = new java.io.File(escritorio, "vendex_errors");
                // usa Escritorio/vendex_errors si ya existe, sino ~/vendex_errors
                if (alt.exists()) dir = alt;
            }
            if (!dir.exists()) dir.mkdirs();
            String fecha = java.time.LocalDate.now().toString();
            java.io.File archivo = new java.io.File(dir, "vendex_" + fecha + ".log");
            String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String linea = String.format("[%s] %s.%s - %s%n%s%n", ts, controlador, metodo, mensaje != null ? mensaje : "", stacktrace != null ? stacktrace : "");
            try (java.io.FileWriter fw = new java.io.FileWriter(archivo, true)) {
                fw.write(linea);
                fw.write("--------------------------------------------------\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo volcar log a vendex_errors", e);
        }
    }

    public void guardar(String controlador, String metodo, String mensaje) {
        guardar(controlador, metodo, mensaje, null);
    }
}
