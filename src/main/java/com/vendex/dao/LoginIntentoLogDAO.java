package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface LoginIntentoLogDAO {

    void registrar(String usuarioInput, boolean exitoso, String equipo);

    int contarFallosRecientes(String usuarioInput, int ventanaMinutos);

}
