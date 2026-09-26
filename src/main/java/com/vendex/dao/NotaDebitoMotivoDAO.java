package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaDebitoMotivo;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface NotaDebitoMotivoDAO {

    void insertarMotivos(int notaDebitoId, List<NotaDebitoMotivo> motivos);

    void insertarMotivos(Connection con, int notaDebitoId, List<NotaDebitoMotivo> motivos) throws SQLException;

    List<NotaDebitoMotivo> listarPorNotaDebitoId(int notaDebitoId);

}
