package com.vendex.remote;

import com.vendex.dao.NotaVentaRegistroDAO;
import com.vendex.model.NotaVentaRegistro;
import com.vendex.util.SesionActual;

import java.util.List;
import java.util.Map;

public class NotaVentaRegistroDAORest extends RestDao<NotaVentaRegistro> implements NotaVentaRegistroDAO {

    public NotaVentaRegistroDAORest(RestClient rest) {
        super(rest, "/notas-venta", NotaVentaRegistro.class);
    }

    @Override
    public List<NotaVentaRegistro> obtenerNumNotaVenta() {
        try {
            Map<String, Object> resp = rest.get("/api/nota-venta", Map.class);
            if (resp != null && resp.get("items") instanceof List<?> items) {
                return items.stream().map(o -> {
                    NotaVentaRegistro n = new NotaVentaRegistro();
                    if (o instanceof Map<?, ?> m) {
                        n.setId(((Number) m.get("id")).intValue());
                        n.setCodigo((String) m.get("codigo"));
                        n.setSucursalId(((Number) m.get("sucursalId")).intValue());
                    }
                    return n;
                }).collect(java.util.stream.Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public int insertar(NotaVentaRegistro nvr) {
        try {
            Map<String, Object> body = Map.of(
                    "clienteId", nvr.getClienteId(),
                    "formaPago", "Efectivo",
                    "items", List.of()
            );
            Map<String, Object> resp = rest.post("/api/nota-venta", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error insertando nota venta vía REST", e);
        }
    }
}
