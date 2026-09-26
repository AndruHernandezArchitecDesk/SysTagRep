package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Alerta;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface AlertaDAO {

    int insertar(Alerta a);

    List<Alerta> listarTodas();

    List<Alerta> listarNoLeidas();

    int contarNoLeidas();

    void marcarComoLeida(int id);

    void marcarTodasComoLeidas();

    void eliminar(int id);

    void eliminarLeidas();

    void limpiar();

}
