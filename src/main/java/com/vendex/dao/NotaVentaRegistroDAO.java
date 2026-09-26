package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaVentaRegistro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface NotaVentaRegistroDAO {

    List<NotaVentaRegistro> obtenerNumNotaVenta();

    int insertar(NotaVentaRegistro nvr);

}
