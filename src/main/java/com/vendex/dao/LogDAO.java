package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface LogDAO {

    void guardar(String controlador, String metodo, String mensaje, Exception ex);

    void guardar(String controlador, String metodo, String mensaje);

}
