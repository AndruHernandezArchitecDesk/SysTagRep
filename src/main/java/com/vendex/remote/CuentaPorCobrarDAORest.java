package com.vendex.remote;

import com.vendex.dao.CuentaPorCobrarDAO;
import com.vendex.model.CuentaPorCobrar;
import com.vendex.util.SesionActual;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class CuentaPorCobrarDAORest extends RestDao<CuentaPorCobrar> implements CuentaPorCobrarDAO {

    public CuentaPorCobrarDAORest(RestClient rest) {
        super(rest, "/cuentas-por-cobrar", CuentaPorCobrar.class);
    }

    @Override
    public List<Object[]> listarCreditosActivos() {
        try {
            return rest.get("/api/cuenta-por-cobrar/creditos-activos", new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Object[]> listarPorCliente(int clienteId) {
        try {
            return rest.get("/api/cuenta-por-cobrar/cliente/" + clienteId, new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<String[]> obtenerDetallesVenta(int notaVentaId) {
        try {
            return rest.get("/api/cuenta-por-cobrar/detalles-venta/" + notaVentaId, new com.fasterxml.jackson.core.type.TypeReference<List<String[]>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void insertar(CuentaPorCobrar cpc) {
        try {
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("clienteId", cpc.getClienteId());
            if (cpc.getNotaVentaId() != null) body.put("notaVentaId", cpc.getNotaVentaId());
            if (cpc.getFacturaRegistroId() != null) body.put("facturaRegistroId", cpc.getFacturaRegistroId());
            body.put("monto", cpc.getTotal() != null ? cpc.getTotal().toString() : "0");
            body.put("saldo", cpc.getTotal() != null ? cpc.getTotal().toString() : "0");
            body.put("sucursalId", SesionActual.getSucursalId());
            rest.post("/api/cuenta-por-cobrar", body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error insertando cuenta por cobrar vía REST", e);
        }
    }

    @Override
    public void insertar(Connection con, CuentaPorCobrar cpc) {
        insertar(cpc);
    }

    @Override
    public void registrarAdelanto(int cpcId, BigDecimal nuevoAdelanto) {
        throw new UnsupportedOperationException("CuentaPorCobrar.registrarAdelanto rest");
    }

    @Override
    public void marcarPagado(int cpcId) {
        try {
            rest.post("/api/cuenta-por-cobrar/" + cpcId + "/pagar", Map.of(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error marcando pagado cuenta por cobrar vía REST", e);
        }
    }
}
