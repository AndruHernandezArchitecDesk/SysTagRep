package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Proveedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ProveedorDAO {

    List<Proveedor> listar();

    void guardar(Proveedor proveedor);

    void actualizar(Proveedor proveedor);

    void eliminar(int id);

}
