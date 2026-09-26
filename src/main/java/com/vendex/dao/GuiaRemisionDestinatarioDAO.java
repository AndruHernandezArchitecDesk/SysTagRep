package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.GuiaRemisionDestinatario;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface GuiaRemisionDestinatarioDAO {

    void insertarDestinatarios(int guiaId, List<GuiaRemisionDestinatario> lista);

    void insertarDestinatarios(Connection con, int guiaId, List<GuiaRemisionDestinatario> lista) throws SQLException;

    List<GuiaRemisionDestinatario> listarPorGuiaId(int guiaId);

}
