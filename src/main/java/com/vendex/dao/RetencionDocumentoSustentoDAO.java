package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.RetencionDocumentoSustento;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface RetencionDocumentoSustentoDAO {

    void insertarDocumentos(int retencionId, List<RetencionDocumentoSustento> lista);

    void insertarDocumentos(Connection con, int retencionId, List<RetencionDocumentoSustento> lista) throws SQLException;

    List<RetencionDocumentoSustento> listarPorRetencionId(int retencionId);

}
