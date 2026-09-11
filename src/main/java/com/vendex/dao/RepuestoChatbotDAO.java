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
public class RepuestoChatbotDAO implements GeminiChatbotService.RepuestoDao {

    private static final Logger LOGGER = Logger.getLogger(RepuestoChatbotDAO.class.getName());

    @Override
    public GeminiChatbotService.ResultadoBusqueda buscar(String descripcion, String marca, String modelo, Integer anio) {
        if (descripcion == null || descripcion.isBlank()) {
            return GeminiChatbotService.ResultadoBusqueda.noEncontrado();
        }

        // Tokenizar descripcion enriquecida (ej: "tapa radiador CHEV AVEO" -> cada token debe estar en descripcion/codigo/marca)
        String[] tokens = descripcion.trim().split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String t : tokens) {
            String clean = t.replaceAll("[^\\p{L}\\p{N}]", "").trim();
            if (clean.length() >= 2) keywords.add(clean.toLowerCase());
        }
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
