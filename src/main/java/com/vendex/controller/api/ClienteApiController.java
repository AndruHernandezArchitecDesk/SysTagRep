package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.ClienteDAO;
import com.vendex.model.Cliente;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClienteApiController {

    private final ClienteDAO dao;

    public ClienteApiController() {
        this(AppContext.getInstance());
    }

    public ClienteApiController(AppContext ctx) {
        this.dao = ctx.clienteDAO;
    }

    public void listar(Context ctx) {
        String q = ctx.queryParam("q");
        try {
            List<Cliente> base = dao.listar();
            if (q != null && !q.isBlank()) {
                String f = q.toLowerCase();
                base = base.stream().filter(c ->
                        (c.getNombre() != null && c.getNombre().toLowerCase().contains(f)) ||
                        (c.getIdentificacion() != null && c.getIdentificacion().toLowerCase().contains(f))
                ).collect(Collectors.toList());
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (Cliente c : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("nombre", c.getNombre());
                m.put("identificacion", c.getIdentificacion());
                m.put("direccion", c.getDireccion());
                m.put("correo", c.getCorreo());
                m.put("telefono", c.getTelefono());
                out.add(m);
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Cliente c = dao.obtenerPorId(id);
        if (c == null) ctx.status(404).json(Map.of("error", "Cliente no encontrado"));
        else ctx.json(Map.of(
                "id", c.getId(),
                "nombre", c.getNombre(),
                "identificacion", c.getIdentificacion(),
                "direccion", c.getDireccion(),
                "correo", c.getCorreo(),
                "telefono", c.getTelefono()
        ));
    }

    public void crear(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Cliente c = new Cliente();
            c.setNombre((String) body.get("nombre"));
            c.setIdentificacion((String) body.get("identificacion"));
            c.setDireccion((String) body.get("direccion"));
            c.setCorreo((String) body.get("correo"));
            c.setTelefono((String) body.get("telefono"));
            dao.guardar(c);
            ctx.json(Map.of("id", c.getId(), "nombre", c.getNombre()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void actualizar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int id = ((Number) body.get("id")).intValue();
            Cliente c = dao.obtenerPorId(id);
            if (c == null) { ctx.status(404).json(Map.of("error", "Cliente no encontrado")); return; }
            c.setNombre((String) body.get("nombre"));
            c.setIdentificacion((String) body.get("identificacion"));
            c.setDireccion((String) body.get("direccion"));
            c.setCorreo((String) body.get("correo"));
            c.setTelefono((String) body.get("telefono"));
            dao.actualizar(c);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void eliminar(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        try {
            dao.eliminar(id);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}
