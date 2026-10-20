package com.vendex.service;

import com.vendex.config.GeminiConfig;
import com.vendex.dao.DashboardDAO;
import com.vendex.dao.HistorialProductoDAO;
import com.vendex.dao.InventarioDAO;
import com.vendex.model.HistorialProducto;
import com.vendex.model.Inventario;
import com.vendex.util.LicenseManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fase 5 Vendexi — análisis empresarial (spec §7): rotación, agotamiento,
 * márgenes y tendencias.
 *
 * El modelo NO tiene acceso irrestricto a la BD: se le envían SOLO agregados
 * autorizados y limitados construidos aquí (§7.3). Todo análisis generado debe
 * indicar periodo, datos, supuestos y limitaciones; nunca garantiza ganancias
 * ni presenta predicciones como hechos.
 */
public class AnalisisEmpresarialService {

    private static final Logger LOGGER = Logger.getLogger(AnalisisEmpresarialService.class.getName());
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(75);
    private static final String MODELO = "gemini-3.1-flash-lite";
    private static final String MODELO_FALLBACK = "gemini-flash-latest";
    private static final int UMBRAL_STOCK_BAJO = 5;
    private static final int TOPE_LISTAS = 12;

    public enum Tipo { ROTACION, AGOTAMIENTO, MARGENES, TENDENCIAS, GENERAL }

    public enum Estado { OK, SIN_LICENCIA, SIN_CAPACIDAD, SIN_API_KEY, SIN_CONEXION, ERROR }

    public static final class Resultado {
        public final Estado estado;
        public final String texto;
        public final String datosUtilizados;
        public Resultado(Estado estado, String texto, String datosUtilizados) {
            this.estado = estado;
            this.texto = texto != null ? texto : "";
            this.datosUtilizados = datosUtilizados != null ? datosUtilizados : "";
        }
        public boolean esOk() { return estado == Estado.OK; }
    }

    private final InventarioDAO inventarioDAO;
    private final DashboardDAO dashboardDAO;
    private final HistorialProductoDAO historialDAO;
    private final VendexiConsumoService consumo;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public AnalisisEmpresarialService(InventarioDAO inventarioDAO, DashboardDAO dashboardDAO,
                                      HistorialProductoDAO historialDAO, VendexiConsumoService consumo) {
        this.inventarioDAO = inventarioDAO;
        this.dashboardDAO = dashboardDAO;
        this.historialDAO = historialDAO;
        this.consumo = consumo;
    }

    // ===== agregados autorizados (§7.3: sin acceso irrestricto al modelo) =====

    /** Resumen compacto de datos (lo único que ve el modelo). */
    public String construirDatosAutorizados(Tipo tipo, int dias) {
        StringBuilder sb = new StringBuilder();
        sb.append("PERIODO: últimos ").append(dias).append(" días (hasta ")
          .append(LocalDateTime.now().withNano(0)).append(").\n");
        try {
            switch (tipo) {
                case ROTACION -> agregarRotacion(sb, dias);
                case AGOTAMIENTO -> agregarAgotamiento(sb);
                case MARGENES -> agregarMargenes(sb);
                case TENDENCIAS -> agregarTendencias(sb, dias);
                case GENERAL -> {
                    agregarTendencias(sb, dias);
                    agregarAgotamiento(sb);
                    agregarMargenes(sb);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudieron agregar datos para análisis", e);
            sb.append("(Error parcial recolectando datos: ").append(e.getMessage()).append(")\n");
        }
        sb.append("FIN DE DATOS.\n");
        return sb.toString();
    }

    private void agregarRotacion(StringBuilder sb, int dias) {
        sb.append("ROTACIÓN (unidades vendidas por producto en el periodo, desde historial interno):\n");
        List<HistorialProducto> hist = historialDAO.listar();
        LocalDateTime desde = LocalDateTime.now().minusDays(Math.max(1, dias));
        Map<String, int[]> unidades = new LinkedHashMap<>(); // codigo -> [unidades]
        Map<String, String> nombres = new LinkedHashMap<>();
        int registros = 0;
        for (HistorialProducto h : hist) {
            if (h == null || h.getFechaVenta() == null || h.getFechaVenta().isBefore(desde)) continue;
            String cod = h.getProductoCodigo() != null ? h.getProductoCodigo() : ("id:" + h.getProductoId());
            unidades.merge(cod, new int[]{Math.max(0, h.getCantidad())}, (a, b) -> new int[]{a[0] + b[0]});
            nombres.putIfAbsent(cod, h.getProductoDescripcion() != null ? h.getProductoDescripcion() : "");
            registros++;
        }
        sb.append("(registros de venta en periodo: ").append(registros).append(")\n");
        if (unidades.isEmpty()) {
            sb.append("(sin ventas registradas en el periodo)\n");
        } else {
            unidades.entrySet().stream()
                    .sorted(Map.Entry.<String, int[]>comparingByValue((a, b) -> Integer.compare(b[0], a[0])))
                    .limit(TOPE_LISTAS)
                    .forEach(e -> sb.append(e.getKey()).append(" | ")
                            .append(nombres.getOrDefault(e.getKey(), "")).append(" | unidades: ")
                            .append(e.getValue()[0]).append('\n'));
        }
        // capital inmovilizado: stock alto sin rotación en el periodo
        sb.append("CAPITAL INMOVILIZADO (con stock y 0 ventas en el periodo):\n");
        List<Inventario> activos = inventarioDAO.listar();
        int mostrados = 0;
        for (Inventario i : activos) {
            if (i == null || i.getCantidad() <= 0) continue;
            String cod = i.getCodigo() != null ? i.getCodigo() : ("id:" + i.getId());
            if (unidades.containsKey(cod)) continue;
            sb.append(cod).append(" | ").append(i.getDescripcion()).append(" | stock: ")
              .append(i.getCantidad()).append(" | costo u.: ").append(i.getCostoSinIVA()).append('\n');
            if (++mostrados >= TOPE_LISTAS) break;
        }
        if (mostrados == 0) sb.append("(sin productos con stock sin rotación)\n");
    }

    private void agregarAgotamiento(StringBuilder sb) {
        sb.append("RIESGO DE AGOTAMIENTO (stock <= ").append(UMBRAL_STOCK_BAJO).append("):\n");
        for (Inventario i : inventarioDAO.listarStockBajo(UMBRAL_STOCK_BAJO)) {
            if (i == null) continue;
            sb.append(i.getCodigo()).append(" | ").append(i.getDescripcion())
              .append(" | stock: ").append(i.getCantidad()).append('\n');
        }
        sb.append("STOCK NEGATIVO:\n");
        for (Inventario i : inventarioDAO.listarStockNegativo()) {
            if (i == null) continue;
            sb.append(i.getCodigo()).append(" | ").append(i.getDescripcion())
              .append(" | stock: ").append(i.getCantidad()).append('\n');
        }
    }

    private void agregarMargenes(StringBuilder sb) {
        sb.append("MÁRGENES ((precio venta - costo sin IVA) / precio venta), inventario activo:\n");
        List<Inventario> activos = inventarioDAO.listar();
        record Margen(String codigo, String desc, double pct, BigDecimal venta, BigDecimal costo) {}
        List<Margen> list = new ArrayList<>();
        for (Inventario i : activos) {
            if (i == null || i.getPrecioVenta() == null || i.getPrecioVenta().signum() <= 0) continue;
            if (i.getCostoSinIVA() == null) continue;
            double pct = i.getPrecioVenta().subtract(i.getCostoSinIVA())
                    .multiply(new BigDecimal("100"))
                    .divide(i.getPrecioVenta(), 1, RoundingMode.HALF_UP).doubleValue();
            list.add(new Margen(i.getCodigo() != null ? i.getCodigo() : ("id:" + i.getId()),
                    i.getDescripcion(), pct, i.getPrecioVenta(), i.getCostoSinIVA()));
        }
        list.sort(Comparator.comparingDouble(Margen::pct));
        sb.append("(con costo registrado: ").append(list.size()).append(" de ")
          .append(activos.size()).append(" productos)\n");
        sb.append("— márgenes más bajos:\n");
        for (int k = 0; k < Math.min(TOPE_LISTAS, list.size()); k++) {
            Margen m = list.get(k);
            sb.append(m.codigo()).append(" | ").append(m.desc()).append(" | margen: ")
              .append(m.pct()).append("% | venta: ").append(m.venta()).append(" | costo: ").append(m.costo()).append('\n');
        }
        sb.append("— márgenes más altos (muestra):\n");
        for (int k = Math.max(0, list.size() - 5); k < list.size(); k++) {
            Margen m = list.get(k);
            sb.append(m.codigo()).append(" | ").append(m.desc()).append(" | margen: ").append(m.pct()).append("%\n");
        }
    }

    private void agregarTendencias(StringBuilder sb, int dias) {
        sb.append("TENDENCIAS DE VENTA (total por día, moneda local):\n");
        Map<String, Double> ventas = dashboardDAO.ventasPorDia(dias);
        if (ventas.isEmpty()) sb.append("(sin ventas en el periodo)\n");
        ventas.forEach((d, v) -> sb.append(d).append(" | ").append(String.format(java.util.Locale.ROOT, "%.2f", v)).append('\n'));
        sb.append("COMPRAS por día:\n");
        Map<String, Double> compras = dashboardDAO.comprasPorDia(dias);
        if (compras.isEmpty()) sb.append("(sin compras en el periodo)\n");
        compras.forEach((d, v) -> sb.append(d).append(" | ").append(String.format(java.util.Locale.ROOT, "%.2f", v)).append('\n'));
        double totalV = ventas.values().stream().mapToDouble(Double::doubleValue).sum();
        sb.append("TOTAL venta periodo: ").append(String.format(java.util.Locale.ROOT, "%.2f", totalV)).append('\n');
    }

    // ===== análisis con IA (§7) =====

    public Resultado analizar(Tipo tipo, int dias) {
        int d = Math.max(1, Math.min(730, dias));
        if (!LicenseManager.licenciaIncluyeIA()) {
            return new Resultado(Estado.SIN_LICENCIA,
                    "La licencia no incluye Vendexi AI.", "");
        }
        if (consumo != null && !consumo.hayCapacidadDisponible()) {
            return new Resultado(Estado.SIN_CAPACIDAD, VendexiConsumoService.mensajeCapacidadAgotada(), "");
        }
        String apiKey = GeminiConfig.obtenerApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return new Resultado(Estado.SIN_API_KEY,
                    "Sin API key: configura Gemini en *Configurar IA* para análisis con IA.", "");
        }
        String datos = construirDatosAutorizados(tipo, d);
        String prompt = construirPrompt(tipo, d, datos);
        try {
            String texto = llamarGemini(prompt, apiKey, MODELO);
            return new Resultado(Estado.OK, texto, resumenCorto(datos));
        } catch (Exception e1) {
            LOGGER.log(Level.FINE, "Modelo principal falló en análisis: " + e1.getMessage());
            try {
                String texto = llamarGemini(prompt, apiKey, MODELO_FALLBACK);
                return new Resultado(Estado.OK, texto, resumenCorto(datos));
            } catch (Exception e2) {
                return new Resultado(Estado.SIN_CONEXION,
                        "⚠️ El análisis con IA no está disponible (" + cortar(e2.getMessage(), 120)
                                + "). Datos disponibles:\n\n" + resumenCorto(datos),
                        resumenCorto(datos));
            }
        }
    }

    public String construirPrompt(Tipo tipo, int dias, String datos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres Vendexi AI, analista empresarial del ERP Vendex (Ecuador, repuestos automotrices).\n");
        sb.append("Analiza el siguiente tema: ").append(tipo.name()).append(" (últimos ").append(dias).append(" días).\n\n");
        sb.append("DATOS INTERNOS AUTORIZADOS (solo estos; son hechos del ERP, no los inventes ni los contradigas):\n");
        sb.append(datos).append('\n');
        sb.append("Reglas obligatorias (spec Vendexi §7):\n");
        sb.append("1. Indica SIEMPRE: Periodo, Datos utilizados, Supuestos, Limitaciones e incertidumbre.\n");
        sb.append("2. No garantices ganancias ni rendimientos; no presentes predicciones como hechos (usa 'podría', 'según estos datos').\n");
        sb.append("3. No inventes cifras, productos ni relaciones que no estén en los datos.\n");
        sb.append("4. Máximo ~400 palabras, en español, con viñetas accionables.\n");
        sb.append("5. Si los datos son insuficientes, dilo claramente en Limitaciones.\n");
        return sb.toString();
    }

    private String llamarGemini(String prompt, String apiKey, String modelo) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo
                + ":generateContent?key=" + apiKey;
        com.fasterxml.jackson.databind.node.ObjectNode root = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
        com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", prompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url)).timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(root)))
                .build();
        HttpResponse<String> resp;
        try {
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IllegalStateException("Timeout: " + e.getMessage(), e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Sin conexión: " + e.getMessage(), e);
        }
        int code = resp.statusCode();
        String b = resp.body();
        if (code == 429) throw new IllegalStateException("429 quota: " + b);
        if (code == 401 || code == 403) throw new IllegalStateException("HTTP " + code + " key");
        if (code != 200) throw new IllegalStateException("HTTP " + code + ": " + cortar(b, 200));

        com.fasterxml.jackson.databind.JsonNode respRoot = MAPPER.readTree(b);
        try {
            com.fasterxml.jackson.databind.JsonNode usage = respRoot.path("usageMetadata");
            if (consumo != null) {
                consumo.registrarTokens("analisis", tipoOperacion(prompt), modelo,
                        usage.path("promptTokenCount").asLong(0),
                        usage.path("candidatesTokenCount").asLong(0));
            }
        } catch (Exception ignored) {}
        StringBuilder texto = new StringBuilder();
        for (com.fasterxml.jackson.databind.JsonNode part :
                respRoot.path("candidates").path(0).path("content").path("parts")) {
            texto.append(part.path("text").asText(""));
        }
        if (texto.toString().isBlank()) throw new IllegalStateException("Respuesta vacía");
        return texto.toString().trim();
    }

    private static String tipoOperacion(String prompt) {
        for (Tipo t : Tipo.values()) {
            if (prompt.startsWith("Eres Vendexi AI") && prompt.contains("tema: " + t.name())) {
                return t.name().toLowerCase();
            }
        }
        return "general";
    }

    /** Versión corta de los datos para mostrar en la UI junto al resultado. */
    public static String resumenCorto(String datos) {
        if (datos == null) return "";
        String[] lineas = datos.split("\n");
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String l : lineas) {
            if (l.isBlank()) continue;
            sb.append(l).append('\n');
            if (++n >= 25) {
                sb.append("… (datos completos enviados al modelo)\n");
                break;
            }
        }
        return sb.toString();
    }

    private static String cortar(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
