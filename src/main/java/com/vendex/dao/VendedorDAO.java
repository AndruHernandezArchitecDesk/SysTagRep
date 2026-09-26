package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Vendedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface VendedorDAO {

    List<Vendedor> listar();

    void guardar(Vendedor vendedor);

    void actualizar(Vendedor vendedor);

    void eliminar(int id);

}
