package com.vendex.remote;

import com.vendex.dao.CajaSesionDAO;
import com.vendex.model.CajaSesion;
import java.math.BigDecimal;
import java.util.List;
import java.lang.String;

public class CajaSesionDAORest extends RestDao<CajaSesion> implements CajaSesionDAO {
    public CajaSesionDAORest(RestClient rest) { super(rest, "/caja-sesion", CajaSesion.class); }
    public int abrir(CajaSesion s) { throw new UnsupportedOperationException("CajaSesion.abrir rest"); }
    public CajaSesion obtenerAbierta() { throw new UnsupportedOperationException("CajaSesion.obtenerAbierta rest"); }
    public CajaSesion obtenerPorId(int id) { throw new UnsupportedOperationException("CajaSesion.obtenerPorId rest"); }
    public List<CajaSesion> listarPorFecha(java.time.LocalDate desde, java.time.LocalDate hasta) { throw new UnsupportedOperationException("CajaSesion.listarPorFecha rest"); }
    public boolean cerrar(int id, BigDecimal montoFisico, BigDecimal diferencia, String observaciones) { throw new UnsupportedOperationException("CajaSesion.cerrar rest"); }
    public BigDecimal calcularTotalMovimientos(int sesionId) { throw new UnsupportedOperationException("CajaSesion.calcularTotalMovimientos rest"); }
}