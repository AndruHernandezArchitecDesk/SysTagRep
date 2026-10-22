package com.vendex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.config.GeminiConfig;
import com.vendex.model.Inventario;
import com.vendex.util.LicenseManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fase 2 Vendexi AI — asistencia contextual en facturación y proformas.
 *
 * - Verificaciones locales del documento (sin IA): cliente, líneas, precios, stock.
 * - Venta cruzada por IA al agregar un producto (Gemini), con motivo explicable.
 * - Gate de licencia: sin Vendexi AI habilitada no se llama al proveedor; el ERP sigue operativo.
 * - Nunca modifica el documento: emite sugerencias que el usuario acepta/descarta.
 */
public class VendexiSugerenciaService {

    private static final Logger LOGGER = Logger.getLogger(VendexiSugerenciaService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_SUGERENCIAS = 3;
    private static final int MAX_CANDIDATOS = 40;
    private static final String MODELO = "gemini-3.1-flash-lite";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final VendexiConsumoService consumo;

    public VendexiSugerenciaService() {
        this(null);
    }

    public VendexiSugerenciaService(VendexiConsumoService consumo) {
        this.consumo = consumo;
    }

    private VendexiConsumoService consumoService() {
        if (consumo != null) return consumo;
        try {
            return com.vendex.config.AppContext.getInstance().vendexiConsumoService;
        } catch (Exception e) {
            return new VendexiConsumoService(new com.vendex.dao.VendexiConsumoDAOPostgres());
        }
    }

    // ===== Tipos =====

    public enum Tipo { VERIFICACION, COMPLEMENTARIO }

    /** Nivel de evidencia de la recomendación (spec §6): diferenciar asociación de compatibilidad. */
    public static final String EVIDENCIA_CONFIRMADA = "CONFIRMADA";
    public static final String EVIDENCIA_PROBABLE = "PROBABLE";
    public static final String EVIDENCIA_ASOCIACION = "ASOCIACION";

    public static final class Sugerencia {
        public final Tipo tipo;
        public final String titulo;
        public final String motivo;
        /** CONFIRMADA (compatibilidad técnica) | PROBABLE (coincidencia) | ASOCIACION (ventas). */
        public final String evidencia;
        public final Inventario producto;

        public Sugerencia(Tipo tipo, String titulo, String motivo, Inventario producto) {
            this(tipo, titulo, motivo, EVIDENCIA_ASOCIACION, producto);
        }

        public Sugerencia(Tipo tipo, String titulo, String motivo, String evidencia, Inventario producto) {
            this.tipo = tipo;
            this.titulo = titulo;
            this.motivo = motivo;
            this.evidencia = normalizarEvidencia(evidencia);
            this.producto = producto;
        }
    }

    public static String normalizarEvidencia(String e) {
        if (e == null) return EVIDENCIA_ASOCIACION;
        String u = e.trim().toUpperCase(Locale.ROOT);
        if (u.contains("CONFIRM")) return EVIDENCIA_CONFIRMADA;
        if (u.contains("PROBAB") || u.contains("COINC")) return EVIDENCIA_PROBABLE;
        return EVIDENCIA_ASOCIACION;
    }

    public static final class Linea {
        public final int inventarioId;
        public final String codigo;
        public final String descripcion;
        public final int cantidad;
        public final java.math.BigDecimal precio;
        public final int stockTotal;

        public Linea(int inventarioId, String codigo, String descripcion,
                     int cantidad, java.math.BigDecimal precio, int stockTotal) {
            this.inventarioId = inventarioId;
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.precio = precio;
            this.stockTotal = stockTotal;
        }
    }

    public static final class Contexto {
        public final String modulo;
        public final boolean clienteSeleccionado;
        public final List<Linea> lineas;

        public Contexto(String modulo, boolean clienteSeleccionado, List<Linea> lineas) {
            this.modulo = modulo;
            this.clienteSeleccionado = clienteSeleccionado;
            this.lineas = lineas != null ? lineas : List.of();
        }
    }

    public boolean esIaDisponible() {
        return LicenseManager.licenciaIncluyeIA() && GeminiConfig.tieneApiKey();
    }

    // ===== Verificaciones locales (siempre disponibles, sin IA) =====

    public List<Sugerencia> verificarDocumento(Contexto ctx) {
        List<Sugerencia> out = new ArrayList<>();
        if (ctx == null) return out;
        if (!ctx.clienteSeleccionado) {
            out.add(new Sugerencia(Tipo.VERIFICACION, "Cliente sin seleccionar",
                    "Un documento de " + ctx.modulo + " debe indicar el cliente. "
                    + "Fuente: validación local del formulario (sin IA).", null));
        }
        if (ctx.lineas.isEmpty()) {
            out.add(new Sugerencia(Tipo.VERIFICACION, "Sin productos en el documento",
                    "Agrega al menos un producto antes de emitir. "
                    + "Fuente: validación local del formulario (sin IA).", null));
        }
        for (Linea l : ctx.lineas) {
            if (l.precio == null || l.precio.signum() <= 0) {
                out.add(new Sugerencia(Tipo.VERIFICACION,
                        "Precio en cero: " + (l.codigo != null ? l.codigo : l.descripcion),
                        "La línea tiene precio unitario en cero; el comprobante saldría sin valor. "
                        + "Fuente: detalle del documento (sin IA).", null));
            }
            if (!"nota_credito".equals(ctx.modulo) && l.stockTotal >= 0 && l.cantidad > l.stockTotal) {
                out.add(new Sugerencia(Tipo.VERIFICACION,
                        "Stock insuficiente: " + (l.codigo != null ? l.codigo : l.descripcion),
                        "Cantidad solicitada (" + l.cantidad + ") mayor al stock actual ("
                        + l.stockTotal + "). Fuente: inventario en el momento de la consulta (sin IA).", null));
            }
        }
        return out;
    }

    // ===== Candidatos para venta cruzada (lógica pura) =====

    /**
     * Selecciona candidatos con stock, excluyendo el producto agregado y los ya en el documento.
     * Prioriza misma marca o grupo y coincidencia de tokens en la descripción.
     */
    public List<Inventario> seleccionarCandidatos(Inventario base, List<Inventario> todos,
                                                  Set<Integer> idsEnDocumento, int max) {
        List<Inventario> out = new ArrayList<>();
        if (base == null || todos == null) return out;
        Set<Integer> excl = new LinkedHashSet<>(idsEnDocumento != null ? idsEnDocumento : Set.of());
        excl.add(base.getId());
        Set<String> tokensBase = tokenizar(base.getDescripcion());

        record Puntaje(Inventario inv, int puntos) {}
        List<Puntaje> puntuados = new ArrayList<>();
        for (Inventario i : todos) {
            if (i == null || excl.contains(i.getId())) continue;
            if (i.getCantidad() <= 0) continue;
            int p = 0;
            if (base.getMarca() != null && base.getMarca().equalsIgnoreCase(i.getMarca())) p += 2;
            if (base.getGrupo() != null && base.getGrupo().equalsIgnoreCase(i.getGrupo())) p += 3;
            Set<String> tokI = tokenizar(i.getDescripcion());
            long overlap = tokI.stream().filter(tokensBase::contains).count();
            p += (int) overlap;
            if (p > 0) puntuados.add(new Puntaje(i, p));
        }
        puntuados.sort((a, b) -> Integer.compare(b.puntos(), a.puntos()));
        for (Puntaje p : puntuados) {
            if (out.size() >= max) break;
            out.add(p.inv());
        }
        return out;
    }

    private Set<String> tokenizar(String texto) {
        Set<String> out = new LinkedHashSet<>();
        if (texto == null) return out;
        for (String t : texto.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (t.length() >= 3) out.add(t);
        }
        return out;
    }

    // ===== Venta cruzada con IA =====

    /**
     * Consulta a la IA (si licencia + key lo permiten) para sugerir complementarios.
     * Devuelve lista vacía si IA no está disponible o falla — el ERP no se ve afectado.
     * Nunca modifica el documento; el usuario decide con Aceptar/Descartar.
     */
    public List<Sugerencia> sugerenciasComplementarias(Inventario agregado, List<Inventario> inventario,
                                                       Set<Integer> idsEnDocumento, String contextoVehiculo) {
        if (Thread.currentThread().isInterrupted()) return List.of();
        if (agregado == null || !esIaDisponible()) return List.of();
        // Fase 3: gate de capacidad asignada — sin capacidad, no se llama al proveedor.
        if (!consumoService().hayCapacidadDisponible()) return List.of();
        List<Inventario> candidatos = seleccionarCandidatos(agregado, inventario, idsEnDocumento, MAX_CANDIDATOS);
        if (candidatos.isEmpty()) return List.of();
        String apiKey = obtenerApiKey();
        if (apiKey == null || apiKey.isBlank()) return List.of();
        try {
            if (Thread.currentThread().isInterrupted()) return List.of();
            String prompt = construirPrompt(agregado, candidatos, contextoVehiculo);
            String json = llamarGemini(prompt, apiKey);
            if (Thread.currentThread().isInterrupted()) return List.of();
            return parsearSugerenciasIA(json, indexarPorCodigo(candidatos));
        } catch (InterruptedException e) {
            // Cambiar el documento cancela la consulta HTTP: es un final esperado, no un fallo de IA.
            Thread.currentThread().interrupt();
            LOGGER.log(Level.FINE, "Vetxi: consulta de complementos cancelada");
            return List.of();
        } catch (java.util.concurrent.CancellationException e) {
            LOGGER.log(Level.FINE, "Vetxi: consulta de complementos cancelada");
            return List.of();
        } catch (Exception e) {
            String detalle = e.getMessage();
            LOGGER.log(Level.WARNING, "Vetxi: venta cruzada IA no disponible (" + e.getClass().getSimpleName() + ")"
                    + (detalle == null || detalle.isBlank() ? "" : ": " + detalle), e);
            return List.of();
        }
    }

    public String construirPrompt(Inventario base, List<Inventario> candidatos, String contextoVehiculo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres Vetxi AI, asistente de ventas de repuestos automotrices en el ERP Vendex.\n");
        sb.append("Producto recién agregado al documento: \"").append(limpiar(base.getDescripcion())).append('"');
        if (base.getMarca() != null) sb.append(" (marca ").append(limpiar(base.getMarca())).append(')');
        sb.append(".\n");
        if (contextoVehiculo != null && !contextoVehiculo.isBlank()) {
            sb.append("Filtro de vehículo del usuario: ").append(limpiar(contextoVehiculo)).append(".\n");
        }
        sb.append("Candidatos del inventario (codigo|descripcion|stock|precio):\n");
        for (Inventario c : candidatos) {
            sb.append(limpiar(c.getCodigo())).append('|').append(limpiar(c.getDescripcion()))
              .append('|').append(c.getCantidad()).append('|').append(c.getPrecioVenta()).append('\n');
        }
        sb.append("Elige maximo ").append(MAX_SUGERENCIAS).append(" productos que suelen comprarse juntos o ser relevantes. ");
        sb.append("Reglas: diferencia asociacion estadistica de compatibilidad tecnica; NO afirmes que es obligatorio o compatible sin evidencia; ");
        sb.append("usa los codigos EXACTOS de la lista; no inventes productos ni precios.\n");
        sb.append("Para cada sugerencia incluye evidencia: \"CONFIRMADA\" solo si la compatibilidad tecnica esta confirmada por fuente fiable; ");
        sb.append("\"PROBABLE\" si es coincidencia razonable no confirmada; \"ASOCIACION\" si es solo por ventas/uso habitual.\n");
        sb.append("Responde SOLO JSON valido sin markdown:\n");
        sb.append("{\"sugerencias\":[{\"codigo\":\"CODIGO\",\"evidencia\":\"ASOCIACION\",\"motivo\":\"motivo breve en espanol\"}]}\n");
        sb.append("Si no hay complementarios razonables responde {\"sugerencias\":[]}.");
        return sb.toString();
    }

    /** Parsea la respuesta JSON de la IA y la mapea a productos reales del inventario. */
    public List<Sugerencia> parsearSugerenciasIA(String json, Map<String, Inventario> porCodigo) {
        List<Sugerencia> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode arr = root.path("sugerencias");
            if (!arr.isArray()) return out;
            for (JsonNode n : arr) {
                if (out.size() >= MAX_SUGERENCIAS) break;
                String codigo = n.path("codigo").asText("").trim();
                String motivo = n.path("motivo").asText("").trim();
                String evidencia = normalizarEvidencia(n.path("evidencia").asText(""));
                if (codigo.isEmpty()) continue;
                Inventario inv = porCodigo.get(codigo.toLowerCase(Locale.ROOT));
                if (inv == null) continue; // la IA inventó un código: se descarta (regla: no inventar)
                if (motivo.isEmpty()) {
                    motivo = "Complemento asociado por la IA a \""
                            + (inv.getDescripcion() != null ? inv.getDescripcion() : codigo)
                            + "\". Verifica compatibilidad antes de facturar.";
                }
                out.add(new Sugerencia(Tipo.COMPLEMENTARIO, inv.getDescripcion(), motivo, evidencia, inv));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Vetxi: JSON de sugerencias inválido", e);
        }
        return out;
    }

    public Map<String, Inventario> indexarPorCodigo(List<Inventario> inventario) {
        Map<String, Inventario> out = new LinkedHashMap<>();
        if (inventario == null) return out;
        for (Inventario i : inventario) {
            if (i == null || i.getCodigo() == null) continue;
            out.put(i.getCodigo().trim().toLowerCase(Locale.ROOT), i);
        }
        return out;
    }

    private String limpiar(String s) {
        if (s == null) return "";
        return s.replace('"', '\'').replace("\n", " ").trim();
    }

    /** Permite probar el proveedor sin leer ni modificar la clave real de la instalación. */
    protected String obtenerApiKey() { return GeminiConfig.obtenerApiKey(); }

    /** Protected para probar cancelaciones y fallos del proveedor sin red. */
    protected String llamarGemini(String prompt, String apiKey) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODELO
                + ":generateContent?key=" + apiKey;
        com.fasterxml.jackson.databind.node.ObjectNode root = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
        com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
        content.put("role", "user");
        com.fasterxml.jackson.databind.node.ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(root)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        JsonNode respRoot = MAPPER.readTree(resp.body());
        // Fase 3: medición de tokens (best-effort)
        try {
            JsonNode usage = respRoot.path("usageMetadata");
            long tin = usage.path("promptTokenCount").asLong(0);
            long tout = usage.path("candidatesTokenCount").asLong(0);
            consumoService().registrarTokens("contextual", "venta_cruzada", MODELO, tin, tout);
        } catch (Exception ignored) {}
        JsonNode candidates = respRoot.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            String text = candidates.get(0).path("content").path("parts")
                    .get(0).path("text").asText("").trim();
            if (text.startsWith("```")) {
                int first = text.indexOf('\n');
                int last = text.lastIndexOf("```");
                if (first >= 0 && last > first) text = text.substring(first + 1, last).trim();
            }
            return text;
        }
        throw new IllegalStateException("Respuesta sin candidates");
    }
}
