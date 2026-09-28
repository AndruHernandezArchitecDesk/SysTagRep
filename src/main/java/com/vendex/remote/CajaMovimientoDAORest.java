package com.vendex.remote;

import com.vendex.dao.CajaMovimientoDAO;
import com.vendex.model.CajaMovimiento;
import java.math.BigDecimal;
import java.util.List;
import java.lang.String;

public class CajaMovimientoDAORest extends RestDao<CajaMovimiento> implements CajaMovimientoDAO {
    public CajaMovimientoDAORest(RestClient rest) { super(rest, "/caja-movimiento", CajaMovimiento.class); }
    public int insertar(CajaMovimiento m) { throw new UnsupportedOperationException("CajaMovimiento.insertar rest"); }
    public List<CajaMovimiento> listarPorSesion(int sesionId) { throw new UnsupportedOperationException("CajaMovimiento.listarPorSesion rest"); }
    public List<CajaMovimiento> listarPorFecha(java.time.LocalDate desde, java.time.LocalDate hasta) { throw new UnsupportedOperationException("CajaMovimiento.listarPorFecha rest"); }
    public BigDecimal totalPorTipo(int sesionId, String tipo) { throw new UnsupportedOperationException("CajaMovimiento.totalPorTipo rest"); }
}