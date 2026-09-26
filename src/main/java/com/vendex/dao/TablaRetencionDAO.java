package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.TablaRetencion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface TablaRetencionDAO {

    List<TablaRetencion> listarVigentes();

    List<TablaRetencion> listarTodas();

    java.math.BigDecimal obtenerPorcentajeVigente(String codigoRetencion);

}
