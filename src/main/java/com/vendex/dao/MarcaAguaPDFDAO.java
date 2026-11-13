package com.vendex.dao;

import com.vendex.model.MarcaAguaPDFConfig;

import java.sql.SQLException;

public interface MarcaAguaPDFDAO {
    MarcaAguaPDFConfig obtener() throws SQLException;
    void guardar(MarcaAguaPDFConfig configuracion) throws SQLException;
}
