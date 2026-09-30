package com.vendex.remote;

import com.vendex.dao.FacturaRegistroDAO;
import com.vendex.model.FacturaRegistro;
import com.vendex.util.SesionActual;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class FacturaRegistroDAORest extends RestDao<FacturaRegistro> implements FacturaRegistroDAO {

    public FacturaRegistroDAORest(RestClient rest) {
        super(rest, "/facturas", FacturaRegistro.class);
    }

    @Override
    public List<FacturaRegistro> obtenerNumFactura() {
        throw new UnsupportedOperationException("FacturaRegistro.obtenerNumFactura rest");
    }

    @Override
    public int insertar(FacturaRegistro fr) {
        try {
            Map<String, Object> body = Map.of(
                    "codigo", fr.getCodigo(),
                    "clienteId", fr.getClienteId(),
                    "empresaId", fr.getEmpresaId(),
                    "fecha", fr.getFecha() != null ? fr.getFecha().toString() : null,
                    "total", fr.getTotal() != null ? fr.getTotal().toString() : "0",
                    "estadoSri", fr.getEstadoSri(),
                    "claveAcceso", fr.getClaveAcceso(),
                    "numComprobante", fr.getNumComprobante(),
                    "sucursalId", SesionActual.getSucursalId()
            );
            Map<String, Object> resp = rest.post("/facturas/registro", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error insertando factura vía REST", e);
        }
    }

    @Override
    public int insertar(Connection con, FacturaRegistro fr) {
        return insertar(fr);
    }

    @Override
    public void actualizarEstado(Connection con, String claveAcceso, String estado) {
        actualizarEstado(claveAcceso, estado);
    }

    @Override
    public FacturaRegistro obtenerPorId(int id) {
        try {
            return rest.get("/facturas/" + id, FacturaRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public FacturaRegistro obtenerPorClaveAcceso(String claveAcceso) {
        try {
            String path = "/facturas/clave?claveAcceso=" + URLEncoder.encode(claveAcceso, StandardCharsets.UTF_8);
            return rest.get(path, FacturaRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void actualizarEstado(String claveAcceso, String estado) {
        try {
            rest.put("/facturas/estado", Map.of("claveAcceso", claveAcceso, "estado", estado), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando estado factura vía REST", e);
        }
    }

    @Override
    public List<FacturaRegistro> listarPendientesSri() {
        try {
            return rest.get("/facturas/pendientes-sri", new com.fasterxml.jackson.core.type.TypeReference<List<FacturaRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public int contar(String filtro) {
        try {
            String path = filtro != null && !filtro.isBlank()
                    ? "/facturas/contar?filtro=" + URLEncoder.encode(filtro, StandardCharsets.UTF_8)
                    : "/facturas/contar";
            Map<String, Object> resp = rest.get(path, Map.class);
            if (resp != null && resp.get("total") instanceof Number n) return n.intValue();
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<FacturaRegistro> listarPaginado(int page, int pageSize, String filtro) {
        try {
            String path = "/facturas?page=" + page + "&size=" + pageSize + (filtro != null && !filtro.isBlank() ? "&q=" + URLEncoder.encode(filtro, StandardCharsets.UTF_8) : "");
            return rest.get(path, new com.fasterxml.jackson.core.type.TypeReference<List<FacturaRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
