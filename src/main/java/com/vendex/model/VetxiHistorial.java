package com.vendex.model;

import java.util.List;
import java.util.UUID;

/** Datos del historial; fechas ISO como texto para compatibilidad con el cliente REST. */
public final class VetxiHistorial {
    private VetxiHistorial() {}

    public record Ambito(int usuarioId, int sucursalId) {
        public Ambito {
            if (usuarioId <= 0 || sucursalId <= 0) throw new IllegalArgumentException("Usuario y sucursal obligatorios");
        }
    }

    public record Conversacion(String id, String titulo, String actualizada) {}
    public record Fuente(String titulo, String uri) {}

    public record Mensaje(String id, String conversacionId, String titulo, boolean usuario, String texto,
                          boolean sinIA, String rolIA, List<String> rolesBloqueados, List<Fuente> fuentes, String fecha) {
        public Mensaje {
            UUID.fromString(id);
            UUID.fromString(conversacionId);
            if (titulo == null || titulo.isBlank() || titulo.length() > 120)
                throw new IllegalArgumentException("Título de conversación inválido");
            if (texto == null || texto.isBlank() || texto.length() > 32000)
                throw new IllegalArgumentException("Mensaje vacío o demasiado largo");
            if (rolIA != null && !List.of("VENTAS", "INVENTARIO", "FINANCIERO").contains(rolIA))
                throw new IllegalArgumentException("Rol IA inválido");
            rolesBloqueados = rolesBloqueados == null ? List.of() : List.copyOf(rolesBloqueados);
            if (rolesBloqueados.size() > 3 || !List.of("VENTAS", "INVENTARIO", "FINANCIERO").containsAll(rolesBloqueados))
                throw new IllegalArgumentException("Roles bloqueados inválidos");
            fuentes = fuentes == null ? List.of() : List.copyOf(fuentes);
            if (fuentes.size() > 20) throw new IllegalArgumentException("Demasiadas fuentes");
            for (Fuente fuente : fuentes) {
                if (fuente.titulo() != null && fuente.titulo().length() > 2000
                        || fuente.uri() != null && fuente.uri().length() > 4000)
                    throw new IllegalArgumentException("Fuente demasiado larga");
            }
        }
    }
}
