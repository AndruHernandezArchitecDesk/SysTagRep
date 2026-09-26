package com.vendex.dao;

import com.vendex.chatbot.GeminiChatbotService;
import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO para chatbot: consulta repuestos directamente en tabla inventario existente.
 * La compatibilidad vehículo está embedida en inventario.descripcion
 * ej: "TAPA RADIADOR CHEV AVEO/EMOTION/SPART" -> ILIKE %AVEO% %CHEV%
 * No crea tablas nuevas.
 */
public class RepuestoChatbotDAOPostgres implements RepuestoChatbotDAO {

    private static final Logger LOGGER = Logger.getLogger(RepuestoChatbotDAO.class.getName());

    @Override
    public GeminiChatbotService.ResultadoBusqueda buscar(String descripcion, String marca, String modelo, Integer anio) {
        if (descripcion == null || descripcion.isBlank()) {
            return GeminiChatbotService.ResultadoBusqueda.noEncontrado();
        }
        // Intentar búsqueda por compatibilidad normalizada (vehiculo) primero, si hay marca/modelo/anio
        if ((marca != null && !marca.isBlank()) || (modelo != null && !modelo.isBlank()) || anio != null) {
            try {
                var vehs = new VehiculoDAOPostgres().buscar(marca, modelo, anio);
                if (!vehs.isEmpty()) {
                    String placeholders = vehs.stream().map(v -> "?").collect(java.util.stream.Collectors.joining(","));
                    StringBuilder sqlVeh = new StringBuilder(
                            "SELECT i.codigo, i.descripcion AS nombre, i.precio_venta AS precio, i.cantidad AS stock " +
                            "FROM inventario i JOIN inventario_vehiculo iv ON iv.inventario_id=i.id " +
                            "WHERE iv.vehiculo_id IN (" + placeholders + ") AND i.estado=true ");
                    List<Object> paramsVeh = new ArrayList<>();
                    for (var v : vehs) paramsVeh.add(v.getId());
                    // Keywords de la descripcion (excluyendo marca/modelo) para acotar dentro del vehículo
                    for (String kw : tokenizar(descripcion)) {
                        if (kw.equalsIgnoreCase(marca) || kw.equalsIgnoreCase(modelo)) continue;
                        sqlVeh.append(" AND LOWER(i.descripcion) LIKE ? ");
                        paramsVeh.add("%" + kw.toLowerCase() + "%");
                    }
                    sqlVeh.append(" ORDER BY i.cantidad DESC, i.descripcion LIMIT 5");
                    try (Connection con = DatabaseConnection.getConnection();
                         PreparedStatement ps = con.prepareStatement(sqlVeh.toString())) {
                        for (int i = 0; i < paramsVeh.size(); i++) ps.setObject(i + 1, paramsVeh.get(i));
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) return mapearResultado(rs);
                        }
                    }
                    // vehículos matchean pero ningún repuesto asociado: probar sin keywords de desc
                    String sqlVehSinDesc = "SELECT i.codigo, i.descripcion AS nombre, i.precio_venta AS precio, i.cantidad AS stock " +
                            "FROM inventario i JOIN inventario_vehiculo iv ON iv.inventario_id=i.id " +
                            "WHERE iv.vehiculo_id IN (" + placeholders + ") AND i.estado=true " +
                            "ORDER BY i.cantidad DESC, i.descripcion LIMIT 5";
                    try (Connection con = DatabaseConnection.getConnection();
                         PreparedStatement ps = con.prepareStatement(sqlVehSinDesc)) {
                        for (int i = 0; i < vehs.size(); i++) ps.setInt(i + 1, vehs.get(i).getId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) return mapearResultado(rs);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Vehiculo compat search fallback to LIKE", e);
            }
        }

        // Tokenizar descripcion enriquecida (ej: "tapa radiador CHEV AVEO" -> cada token debe estar en descripcion/codigo/marca)
        List<String> keywords = tokenizar(descripcion);
        if (keywords.isEmpty()) keywords.add(descripcion.toLowerCase());

        // Construir WHERE dinámico: cada keyword debe aparecer en descripcion o codigo o marca
        StringBuilder sql = new StringBuilder("""
                SELECT i.codigo, i.descripcion AS nombre, i.precio_venta AS precio, i.cantidad AS stock
                FROM inventario i
                LEFT JOIN marca m ON m.id = i.marca_id
                LEFT JOIN grupo g ON g.id = i.grupo_id
                WHERE i.estado = true
                """);

        List<String> params = new ArrayList<>();
        for (String kw : keywords) {
            sql.append(" AND (LOWER(i.descripcion) LIKE ? OR LOWER(i.codigo) LIKE ? OR LOWER(m.nombre) LIKE ? OR LOWER(g.nombre) LIKE ?)");
            String like = "%" + kw + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        // Filtros adicionales marca/modelo/anio si vienen de Gemini: refuerzan sobre descripcion
        if (marca != null && !marca.isBlank() && !keywords.contains(marca.toLowerCase())) {
            sql.append(" AND LOWER(i.descripcion) LIKE ?");
            params.add("%" + marca.toLowerCase() + "%");
        }
        if (modelo != null && !modelo.isBlank() && !keywords.contains(modelo.toLowerCase())) {
            sql.append(" AND LOWER(i.descripcion) LIKE ?");
            params.add("%" + modelo.toLowerCase() + "%");
        }
        // anio no se filtra contra tabla (no hay columna anio), pero si está en descripcion como "2015" lo capturará keywords

        sql.append(" ORDER BY i.cantidad DESC, i.descripcion LIMIT 5");

        // Intentar con ILIKE primero (PostgreSQL), fallback a LOWER LIKE para HSQLDB tests
        try {
            return ejecutar(sql.toString(), params, true);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("ilike")) {
                // reintentar ya está en LOWER LIKE, pero si el SQL original usaba ILIKE, convertir
                String fallback = sql.toString().replace("ILIKE", "LIKE");
                try {
                    return ejecutar(fallback, params, false);
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Error fallback HSQLDB", ex);
                }
            } else {
                LOGGER.log(Level.WARNING, "Error consultando repuesto", e);
            }
        }
        return GeminiChatbotService.ResultadoBusqueda.noEncontrado();
    }

    private List<String> tokenizar(String texto) {
        List<String> keywords = new ArrayList<>();
        if (texto == null) return keywords;
        for (String t : texto.trim().split("\\s+")) {
            String clean = t.replaceAll("[^\\p{L}\\p{N}]", "").trim();
            if (clean.length() >= 2) keywords.add(clean.toLowerCase());
        }
        return keywords;
    }

    private GeminiChatbotService.ResultadoBusqueda mapearResultado(ResultSet rs) throws SQLException {
        String codigo = rs.getString("codigo");
        String nombre = rs.getString("nombre");
        int stock = rs.getInt("stock");
        Double precio = null;
        try {
            var bd = rs.getBigDecimal("precio");
            if (bd != null) precio = bd.doubleValue();
        } catch (SQLException ignored) {}
        return new GeminiChatbotService.ResultadoBusqueda(true, codigo, nombre, stock, precio);
    }

    private GeminiChatbotService.ResultadoBusqueda ejecutar(String sql, List<String> params, boolean useIlike) throws SQLException {
        // Si useIlike, reemplazar LOWER LIKE por ILIKE (más eficiente en PG con índices)
        String finalSql = useIlike ? sql.replace("LOWER(i.descripcion) LIKE ?", "i.descripcion ILIKE ?")
                .replace("LOWER(i.codigo) LIKE ?", "i.codigo ILIKE ?")
                .replace("LOWER(m.nombre) LIKE ?", "m.nombre ILIKE ?")
                .replace("LOWER(g.nombre) LIKE ?", "g.nombre ILIKE ?") : sql;

        // Para ILIKE, los params deben seguir siendo %kw% pero sin lower (PG es case-insensitive)
        // Simplificamos: mantener LOWER LIKE para compatibilidad total — funciona en PG y HSQLDB
        // Así que ignoramos useIlike y usamos LOWER LIKE siempre
        finalSql = sql;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(finalSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String codigo = rs.getString("codigo");
                    String nombre = rs.getString("nombre");
                    int stock = rs.getInt("stock");
                    Double precio = null;
                    try {
                        var bd = rs.getBigDecimal("precio");
                        if (bd != null) precio = bd.doubleValue();
                    } catch (SQLException ignored) {}
                    return new GeminiChatbotService.ResultadoBusqueda(true, codigo, nombre, stock, precio);
                }
            }
        }
        return GeminiChatbotService.ResultadoBusqueda.noEncontrado();
    }
}
