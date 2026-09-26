package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Empresa;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface EmpresaDAO {

    Empresa obtenerPorId(int id);

    List<Empresa> listar();

    boolean actualizar(Empresa empresa);

}
