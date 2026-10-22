package com.vendex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.config.GeminiConfig;
import com.vendex.util.LicenseManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Fase 4 Vendexi — investigación web de repuestos (spec §8).
 *
 * Usa Gemini con herramienta de búsqueda (google_search) para investigar códigos /
 * números de parte, exigiendo fuentes verificables y diferenciando compatibilidad
 * confirmada de coincidencias probables. Nunca inventa códigos, enlaces ni precios.
 */
public class InvestigacionWebService {

    private static final Logger LOGGER = Logger.getLogger(InvestigacionWebService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final String MODELO_PRINCIPAL = "gemini-2.5-flash";
    private static final String MODELO_FALLBACK = "gemini-flash-latest";

    // "investiga X", "busca en internet X", "google X" — no cualquier "busca" (eso es local)
    private static final Pattern PATRON_INTENCION = Pattern.compile(
            ".*(investig(a|ar|ue|aci[oó]n)|busca(r)?\\s+en\\s+(internet|la\\s+web|online)|b[uú]squ[eé]da\\s+(web|en\\s+internet)|google|en\\s+internet|consulta\\s+web).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PATRON_CODIGO = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9\\-/_\\.]{4,}$");

    public static final class Fuente {
        public final String titulo;
        public final String uri;
        public Fuente(String titulo, String uri) {
            this.titulo = titulo;
            this.uri = uri;
        }
        public boolean tieneUri() { return uri != null && uri.startsWith("http"); }
    }

    public enum Estado { OK, SIN_LICENCIA, SIN_CAPACIDAD, SIN_API_KEY, SIN_BUSQUEDA, SIN_CONEXION, ERROR }

    public static final class Resultado {
        public final Estado estado;
        public final String texto;
        public final List<Fuente> fuentes;
        public final boolean sinEvidencia;
        public Resultado(Estado estado, String texto, List<Fuente> fuentes, boolean sinEvidencia) {
            this.estado = estado;
            this.texto = texto != null ? texto : "";
            this.fuentes = fuentes != null ? fuentes : List.of();
            this.sinEvidencia = sinEvidencia;
        }
        public boolean esOk() { return estado == Estado.OK; }
    }

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final VendexiConsumoService consumo;

    public InvestigacionWebService() { this(null); }

    public InvestigacionWebService(VendexiConsumoService consumo) { this.consumo = consumo; }

    private VendexiConsumoService consumoService() {
        if (consumo != null) return consumo;
        try {
            return com.vendex.config.AppContext.getInstance().vendexiConsumoService;
        } catch (Exception e) {
            return new VendexiConsumoService(new com.vendex.dao.VendexiConsumoDAOPostgres());
        }
    }

    /**
     * true si el mensaje del usuario pide investigación web explícita o es
     * esencialmente un código de repuesto (número de parte).
     */
    public static boolean esConsultaInvestigacion(String texto) {
        if (texto == null || texto.isBlank()) return false;
        String t = texto.trim();
        if (PATRON_INTENCION.matcher(t).matches()) return true;
        // un "código puro" (número de parte con guion/barra y al menos un dígito) dispara investigación
        if (t.length() >= 5 && t.length() <= 40 && !t.contains(" ")
                && (t.contains("-") || t.contains("/"))
                && PATRON_CODIGO.matcher(t).matches() && t.matches(".*\\d.*")) {
            return true;
        }
        return false;
    }

    /**
     * Investiga en la web un código/consulta, comparando con el inventario local.
     *
     * @param resumenInventarioLocal resumen opcional (código|nombre|stock) del ERP; nunca inventa
     *                               existencias propias a partir de datos web.
     */
    public Resultado investigar(String consulta, String resumenInventarioLocal,
                                String vehMarca, String vehModelo, Integer vehAnio) {
        if (consulta == null || consulta.isBlank()) {
            return new Resultado(Estado.ERROR, "Indica el código o número de parte a investigar.", List.of(), true);
        }
        if (!LicenseManager.licenciaIncluyeIA()) {
            return new Resultado(Estado.SIN_LICENCIA,
                    "La licencia de esta instalación no incluye Vetxi AI: no se realiza investigación web.",
                    List.of(), true);
        }
        if (!consumoService().hayCapacidadDisponible()) {
            return new Resultado(Estado.SIN_CAPACIDAD,
                    VendexiConsumoService.mensajeCapacidadAgotada(), List.of(), true);
        }
        String apiKey = GeminiConfig.obtenerApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return new Resultado(Estado.SIN_API_KEY,
                    "Sin API key: la investigación web requiere configurar Gemini en *Configurar IA*.", List.of(), true);
        }
        String prompt = construirPrompt(consulta, resumenInventarioLocal, vehMarca, vehModelo, vehAnio);
        try {
            return llamarConBusqueda(prompt, apiKey);
        } catch (Exception e1) {
            LOGGER.log(Level.FINE, "google_search falló (" + e1.getMessage() + "), probando modelo alternativo");
            try {
                return llamarConBusqueda(prompt, apiKey, MODELO_FALLBACK);
            } catch (Exception e2) {
                LOGGER.log(Level.WARNING, "Investigación web no disponible", e2);
                return new Resultado(Estado.SIN_CONEXION,
                        "⚠️ La investigación web no está disponible en este momento (" + cortar(e2.getMessage(), 120)
                                + "). Intenta de nuevo o usa la búsqueda local de inventario.",
                        List.of(), true);
            }
        }
    }

    private Resultado llamarConBusqueda(String prompt, String apiKey) throws Exception {
        return llamarConBusqueda(prompt, apiKey, MODELO_PRINCIPAL);
    }

    private Resultado llamarConBusqueda(String prompt, String apiKey, String modelo) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo
                + ":generateContent?key=" + apiKey;
        com.fasterxml.jackson.databind.node.ObjectNode root = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
        com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", prompt);
        // Herramienta de búsqueda de Google (grounding) — spec §8
        root.withArray("tools").addObject().putObject("google_search");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
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
        if (code == 400 && (b.contains("google_search") || b.contains("tools"))) {
            throw new IllegalStateException("El modelo no admite google_search (HTTP 400)");
        }
        if (code == 429) throw new IllegalStateException("429 quota: " + b);
        if (code == 401 || code == 403) throw new IllegalStateException("HTTP " + code + " key: " + b);
        if (code != 200) throw new IllegalStateException("HTTP " + code + ": " + cortar(b, 200));

        JsonNode respRoot = MAPPER.readTree(b);
        try {
            JsonNode usage = respRoot.path("usageMetadata");
            long tin = usage.path("promptTokenCount").asLong(0);
            long tout = usage.path("candidatesTokenCount").asLong(0);
            consumoService().registrarTokens("web", "investigacion", modelo, tin, tout);
        } catch (Exception ignored) {}

        String texto = extraerTexto(respRoot);
        List<Fuente> fuentes = parsearFuentes(respRoot);
        boolean sinEvidencia = texto.toLowerCase(Locale.ROOT).contains("no encontr")
                || texto.toLowerCase(Locale.ROOT).contains("sin evidencia")
                || (fuentes.isEmpty() && texto.toLowerCase(Locale.ROOT).contains("evidencia"));
        return new Resultado(Estado.OK, texto, fuentes, sinEvidencia);
    }

    /** Extrae el texto de la respuesta Gemini (candidates[0].content.parts[*].text). */
    public static String extraerTexto(JsonNode respRoot) {
        if (respRoot == null) return "";
        JsonNode cand = respRoot.path("candidates");
        if (!cand.isArray() || cand.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : cand.get(0).path("content").path("parts")) {
            sb.append(part.path("text").asText(""));
        }
        return sb.toString().trim();
    }

    /**
     * Extrae fuentes del groundingMetadata (groundingChunks[].web.uri|title).
     * Solo agrega enlaces http(s) reales que vengan del proveedor: nada inventado.
     */
    public static List<Fuente> parsearFuentes(JsonNode respRoot) {
        Set<String> vistas = new LinkedHashSet<>();
        List<Fuente> out = new ArrayList<>();
        if (respRoot == null) return out;
        JsonNode chunks = respRoot.path("candidates");
        if (!chunks.isArray() || chunks.isEmpty()) return out;
        JsonNode grounding = chunks.get(0).path("groundingMetadata").path("groundingChunks");
        if (!grounding.isArray()) return out;
        for (JsonNode chunk : grounding) {
            JsonNode web = chunk.path("web");
            String uri = web.path("uri").asText("").trim();
            String titulo = web.path("title").asText("").trim();
            if (uri.isEmpty() || !uri.startsWith("http")) continue;
            if (!vistas.add(uri)) continue;
            out.add(new Fuente(titulo.isEmpty() ? uri : titulo, uri));
            if (out.size() >= 8) break;
        }
        return out;
    }

    /** Prompt de investigación (spec §8): fuentes, verificación, comparación con inventario. */
    public String construirPrompt(String consulta, String resumenInventarioLocal,
                                  String vehMarca, String vehModelo, Integer vehAnio) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres Vetxi AI, especialista en repuestos automotrices del ERP Vendex (Ecuador). ");
        sb.append("Tu tarea: investigar en internet el codigo o numero de parte solicitado usando la busqueda web.\n");
        sb.append("Consulta: \"").append(limpiar(consulta)).append("\"\n");
        if (vehMarca != null || vehModelo != null || vehAnio != null) {
            sb.append("Vehiculo del usuario: ")
              .append(vehMarca != null ? limpiar(vehMarca) : "?").append(' ')
              .append(vehModelo != null ? limpiar(vehModelo) : "").append(' ')
              .append(vehAnio != null ? vehAnio : "").append(".\n");
        }
        if (resumenInventarioLocal != null && !resumenInventarioLocal.isBlank()) {
            sb.append("Inventario interno Vendex (codigo|nombre|stock) — compara con lo encontrado:\n")
              .append(resumenInventarioLocal).append('\n');
        }
        sb.append("Reglas estrictas (spec Vendexi §8):\n");
        sb.append("1. Prioriza catalogos de fabricantes, distribuidores reconocidos y fuentes tecnicas fiables.\n");
        sb.append("2. Presenta enlaces verificables; no inventes codigos, imagenes, enlaces, precios ni compatibilidades.\n");
        sb.append("3. Diferencia explicitamente: COMPAT_CONFIRMADA (fuente tecnica oficial/fabricante) vs COINCIDENCIA_PROBABLE (coincidencia parcial o no oficial) vs SIN_EVIDENCIA.\n");
        sb.append("4. Si el inventario interno tiene el codigo, indicarlo y comparar stock vs hallazgos web (el stock es SOLO el del inventario).\n");
        sb.append("5. Si no encuentras evidencia suficiente, di claramente 'No se encontro evidencia suficiente'.\n");
        sb.append("6. Responde en espanol, maximo ~250 palabras, claro y accionable.\n");
        return sb.toString();
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace('"', '\'').replace("\n", " ").trim();
    }

    private static String cortar(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
