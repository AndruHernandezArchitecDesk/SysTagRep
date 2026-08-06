package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;

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

    public void guardar(String controlador, String metodo, String mensaje) {
        guardar(controlador, metodo, mensaje, null);
    }
}
