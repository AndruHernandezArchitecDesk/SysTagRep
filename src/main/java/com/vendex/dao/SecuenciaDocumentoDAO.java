package com.vendex.dao;

import com.vendex.model.SecuenciaDocumento;

import java.sql.Connection;
import java.sql.SQLException;

public interface SecuenciaDocumentoDAO {

    SecuenciaDocumento obtener(int puntoEmisionId, String tipo);

    boolean establecer(int puntoEmisionId, String tipo, String prefijo, String establecimiento, String puntoEmision, int numero);

    int marcarUsado(int puntoEmisionId, String tipo);

    int marcarUsado(Connection con, int puntoEmisionId, String tipo) throws SQLException;

    SecuenciaDocumento obtener(Connection con, int puntoEmisionId, String tipo) throws SQLException;

    boolean existeCodigoNotaVenta(String codigo);

    boolean existeCodigoFactura(String codigo);

    boolean existeCodigoNotaCredito(String codigo);
}
