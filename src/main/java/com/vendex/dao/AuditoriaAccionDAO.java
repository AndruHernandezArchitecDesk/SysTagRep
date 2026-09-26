package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface AuditoriaAccionDAO {

    void registrar(int usuarioId, String permisoCodigo, String resultado, String detalle);

}
