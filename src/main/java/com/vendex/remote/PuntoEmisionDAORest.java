package com.vendex.remote;

import com.vendex.dao.PuntoEmisionDAO;
import com.vendex.model.PuntoEmision;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.lang.String;

public class PuntoEmisionDAORest extends RestDao<PuntoEmision> implements PuntoEmisionDAO {
    public PuntoEmisionDAORest(RestClient rest) { super(rest, "/punto-emision", PuntoEmision.class); }
    public List<PuntoEmision> listarPorSucursal(int sucursalId) { throw new UnsupportedOperationException("PuntoEmision.listarPorSucursal rest"); }
    public Optional<PuntoEmision> obtenerPorId(int id) { throw new UnsupportedOperationException("PuntoEmision.obtenerPorId rest"); }
    public Optional<PuntoEmision> obtenerPorCodigoSucursal(int sucursalId, String codigo) { throw new UnsupportedOperationException("PuntoEmision.obtenerPorCodigoSucursal rest"); }
    public int guardar(PuntoEmision p) { throw new UnsupportedOperationException("PuntoEmision.guardar rest"); }
    public void actualizar(PuntoEmision p) { throw new UnsupportedOperationException("PuntoEmision.actualizar rest"); }
    public void desactivar(int id) { throw new UnsupportedOperationException("PuntoEmision.desactivar rest"); }
    public int guardar(Connection con, PuntoEmision p) { throw new UnsupportedOperationException("PuntoEmision.guardar rest"); }
    public void actualizar(Connection con, PuntoEmision p) { throw new UnsupportedOperationException("PuntoEmision.actualizar rest"); }
}