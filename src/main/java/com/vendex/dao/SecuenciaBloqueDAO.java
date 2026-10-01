package com.vendex.dao;

import com.vendex.model.SecuenciaBloque;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface SecuenciaBloqueDAO {

    SecuenciaBloque obtenerBloqueDisponible(int puntoEmisionId, String tipo);

    SecuenciaBloque obtenerBloqueDisponible(Connection con, int puntoEmisionId, String tipo) throws SQLException;

    SecuenciaBloque reservarBloque(int puntoEmisionId, String tipo, int tamano);

    SecuenciaBloque reservarBloque(Connection con, int puntoEmisionId, String tipo, int tamano) throws SQLException;

    void marcarUsado(SecuenciaBloque bloque, int numeroUsado);

    void marcarUsado(Connection con, SecuenciaBloque bloque, int numeroUsado) throws SQLException;

    List<SecuenciaBloque> listarPorPuntoEmision(int puntoEmisionId);

    void liberarBloquesExpirados();
}
