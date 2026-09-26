package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.RetencionDetalle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface RetencionDetalleDAO {

    void insertarDetalles(int docSustentoId, List<RetencionDetalle> lista);

    void insertarDetalles(Connection con, int docSustentoId, List<RetencionDetalle> lista) throws SQLException;

    List<RetencionDetalle> listarPorDocumentoId(int docId);

}
