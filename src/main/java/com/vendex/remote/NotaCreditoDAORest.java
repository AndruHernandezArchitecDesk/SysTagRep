package com.vendex.remote;

import com.vendex.dao.NotaCreditoRegistroDAO;
import com.vendex.model.NotaCreditoRegistro;
import com.vendex.util.SesionActual;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class NotaCreditoDAORest extends RestDao<NotaCreditoRegistro> implements NotaCreditoRegistroDAO {

    public NotaCreditoDAORest(RestClient rest) {
        super(rest, "/nota-credito", NotaCreditoRegistro.class);
    }

    @Override
    public int insertar(NotaCreditoRegistro nc) {
        try {
            Map<String, Object> body = Map.of(
                    "facturaRegistroId", nc.getFacturaRegistroId(),
                    "claveAcceso", nc.getClaveAcceso(),
                    "establecimiento", nc.getEstablecimiento(),
                    "puntoEmision", nc.getPuntoEmision(),
                    "secuencial", nc.getSecuencial(),
                    "fechaEmision", nc.getFechaEmision() != null ? nc.getFechaEmision().toString() : null,
                    "motivo", nc.getMotivo(),
                    "tipoMotivo", nc.getTipoMotivo(),
                    "estadoSri", nc.getEstadoSri(),
                    "sucursalId", SesionActual.getSucursalId()
            );
            Map<String, Object> resp = rest.post("/api/nota-credito", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error insertando nota crédito vía REST", e);
        }
    }

    @Override
    public int insertar(Connection con, NotaCreditoRegistro nc) {
        return insertar(nc);
    }

    @Override
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        actualizarEstado(claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion);
    }

    @Override
    public NotaCreditoRegistro obtenerPorClave(String claveAcceso) {
        try {
            String path = "/api/nota-credito/clave?claveAcceso=" + URLEncoder.encode(claveAcceso, StandardCharsets.UTF_8);
            return rest.get(path, NotaCreditoRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public NotaCreditoRegistro obtenerPorId(int id) {
        try {
            return rest.get("/api/nota-credito/" + id, NotaCreditoRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<NotaCreditoRegistro> listarPorFactura(int facturaRegistroId) {
        try {
            return rest.get("/api/nota-credito/por-factura/" + facturaRegistroId, new com.fasterxml.jackson.core.type.TypeReference<List<NotaCreditoRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public java.math.BigDecimal sumarValorModificacionPorFactura(int facturaRegistroId, String estadoFiltro) {
        throw new UnsupportedOperationException("NotaCreditoDAORest.sumarValorModificacionPorFactura");
    }

    @Override
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        try {
            rest.put("/api/nota-credito/estado", Map.of(
                    "claveAcceso", claveAcceso,
                    "estado", estado,
                    "mensaje", mensaje,
                    "numeroAutorizacion", numeroAutorizacion,
                    "fechaAutorizacion", fechaAutorizacion
            ), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando estado NC vía REST", e);
        }
    }

    @Override
    public void actualizarXmlFirmado(String claveAcceso, String xmlFirmado) {
        try {
            rest.put("/api/nota-credito/xml", Map.of("claveAcceso", claveAcceso, "xmlFirmado", xmlFirmado), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando XML NC vía REST", e);
        }
    }

    @Override
    public List<NotaCreditoRegistro> listarPendientesSri() {
        try {
            return rest.get("/api/nota-credito/pendientes-sri", new com.fasterxml.jackson.core.type.TypeReference<List<NotaCreditoRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<NotaCreditoRegistro> listarPaginado(int page, int pageSize, String filtro) {
        try {
            String path = "/api/nota-credito?page=" + page + "&size=" + pageSize + (filtro != null && !filtro.isBlank() ? "&q=" + URLEncoder.encode(filtro, StandardCharsets.UTF_8) : "");
            return rest.get(path, new com.fasterxml.jackson.core.type.TypeReference<List<NotaCreditoRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public int contar(String filtro) {
        try {
            String path = filtro != null && !filtro.isBlank()
                    ? "/api/nota-credito/contar?filtro=" + URLEncoder.encode(filtro, StandardCharsets.UTF_8)
                    : "/api/nota-credito/contar";
            Map<String, Object> resp = rest.get(path, Map.class);
            if (resp != null && resp.get("total") instanceof Number n) return n.intValue();
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<NotaCreditoRegistro> listarTodas() {
        try {
            return rest.get("/api/nota-credito/todas", new com.fasterxml.jackson.core.type.TypeReference<List<NotaCreditoRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
