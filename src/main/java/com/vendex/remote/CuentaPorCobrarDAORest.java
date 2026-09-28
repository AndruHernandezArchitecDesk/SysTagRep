package com.vendex.remote;

import com.vendex.dao.CuentaPorCobrarDAO;
import com.vendex.model.CuentaPorCobrar;
import java.util.List;
import java.math.BigDecimal;
import java.sql.Connection;

public class CuentaPorCobrarDAORest extends RestDao<CuentaPorCobrar> implements CuentaPorCobrarDAO {
    public CuentaPorCobrarDAORest(RestClient rest) { super(rest, "/cuentas-por-cobrar", CuentaPorCobrar.class); }
    public List<Object[]> listarCreditosActivos() { try { return rest.get("/cuentas-por-cobrar/creditos-activos", new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>(){}); } catch(Exception e) { throw new RuntimeException(e); } }
    public List<Object[]> listarPorCliente(int clienteId) { try { return rest.get("/cuentas-por-cobrar/cliente/"+clienteId, new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>(){}); } catch(Exception e) { throw new RuntimeException(e); } }
    public List<String[]> obtenerDetallesVenta(int notaVentaId) { try { return rest.get("/cuentas-por-cobrar/detalles-venta/"+notaVentaId, new com.fasterxml.jackson.core.type.TypeReference<List<String[]>>(){}); } catch(Exception e) { throw new RuntimeException(e); } }
    public void insertar(CuentaPorCobrar cpc) { throw new UnsupportedOperationException("CuentaPorCobrar.insertar rest"); }
    public void insertar(Connection con, CuentaPorCobrar cpc) { throw new UnsupportedOperationException("CuentaPorCobrar.insertar rest"); }
    public void registrarAdelanto(int cpcId, BigDecimal nuevoAdelanto) { throw new UnsupportedOperationException("CuentaPorCobrar.registrarAdelanto rest"); }
    public void marcarPagado(int cpcId) { throw new UnsupportedOperationException("CuentaPorCobrar.marcarPagado rest"); }
}