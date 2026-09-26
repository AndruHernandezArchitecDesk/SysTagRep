package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.ComprobanteTemp;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ComprobanteTempDAO {

    int insertar(int proformaId, String codigo, String descripcion, int cantidad, java.math.BigDecimal precioUnitario, java.math.BigDecimal precioTotal);

    List<ComprobanteTemp> listarPorProforma(int proformaId);

    void eliminar(int id);

    void limpiarPorProforma(int proformaId);

}
