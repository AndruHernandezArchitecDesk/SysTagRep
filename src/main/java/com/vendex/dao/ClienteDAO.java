package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ClienteDAO {

    List<Cliente> obtenerListaClientes();

    List<Cliente> listar();

    void guardar(Cliente cliente);

    void actualizar(Cliente cliente);

    Cliente obtenerPorId(int id);

    void eliminar(int id);

}
