package com.vendex.remote;

import com.vendex.dao.CuentaPorPagarDAO;
import com.vendex.model.CuentaPorPagar;
import java.math.BigDecimal;
import java.util.List;

public class CuentaPorPagarDAORest extends RestDao<CuentaPorPagar> implements CuentaPorPagarDAO {
    public CuentaPorPagarDAORest(RestClient rest) { super(rest, "/cuentas-por-pagar", CuentaPorPagar.class); }
    public List<Object[]> listarCreditosActivos() { try { return rest.get("/cuentas-por-pagar/creditos-activos", new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>(){}); } catch(Exception e) { throw new RuntimeException(e); } }
    public List<String[]> obtenerDetallesInventario(int inventarioId) { try { return rest.get("/cuentas-por-pagar/detalles-inventario/"+inventarioId, new com.fasterxml.jackson.core.type.TypeReference<List<String[]>>(){}); } catch(Exception e) { throw new RuntimeException(e); } }
    public void insertar(CuentaPorPagar cpp) { throw new UnsupportedOperationException("CuentaPorPagar.insertar rest"); }
    public void registrarAdelanto(int cppId, BigDecimal nuevoAdelanto) { throw new UnsupportedOperationException("CuentaPorPagar.registrarAdelanto rest"); }
    public void marcarPagado(int cppId) { throw new UnsupportedOperationException("CuentaPorPagar.marcarPagado rest"); }
    public void eliminarPorInventarios(List inventarioIds) { throw new UnsupportedOperationException("CuentaPorPagar.eliminarPorInventarios rest"); }
}