package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.SecuenciaDocumento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface SecuenciaDocumentoDAO {

    SecuenciaDocumento obtener(String tipo);

    boolean establecer(String tipo, String establecimiento, String puntoEmision, int numero);

    int marcarUsado(String tipo);

    int marcarUsado(Connection con, String tipo) throws SQLException;

    SecuenciaDocumento obtener(Connection con, String tipo) throws SQLException;

    boolean existeCodigoNotaVenta(String codigo);

    boolean existeCodigoFactura(String codigo);

    boolean existeCodigoNotaCredito(String codigo);

}
