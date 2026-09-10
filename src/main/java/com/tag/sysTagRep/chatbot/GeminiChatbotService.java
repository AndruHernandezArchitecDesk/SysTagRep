package com.tag.sysTagRep.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tag.sysTagRep.config.GeminiConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chatbot con sugerencia IA.
 * - Si hay GEMINI_API_KEY (env var o ~/.systag/gemini.properties), usa Gemini para:
 *   1) extraer + sugerir repuesto (síntomas -> "pastilla freno" para "ruido al frenar")
 *   2) buscar ese término en inventario.descripcion (ILIKE)
 *   3) redactar respuesta amable sin inventar stock
 * - Si no hay key o hay error (tokens/conexión/servidor), fallback a ILIKE directo
 *   y expone EstadoIA para que la UI muestre badge "sin IA".
 */
public class GeminiChatbotService {

    private static final Logger LOGGER = Logger.getLogger(GeminiChatbotService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GEMINI_MODEL_PRIMARY = "gemini-3.1-flash-lite";
    private static final String GEMINI_MODEL_FALLBACK = "gemini-flash-latest";
    private static final String GEMINI_MODEL_FALLBACK2 = "gemini-flash-lite-latest";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    public enum EstadoIA {
        IA_ACTIVA,          // key presente y último call ok
        SIN_API_KEY,        // no hay key en env ni archivo
        SIN_IA_TOKENS,      // 429 / quota / RESOURCE_EXHAUSTED
        SIN_IA_SERVIDOR,    // 5xx
        SIN_IA_CONEXION,    // timeout / network
        SIN_IA_KEY_INVALIDA // 400/401/403
    }

    private final RepuestoDao repuestoDao;
    private final HttpClient httpClient;
    private volatile EstadoIA estado = EstadoIA.SIN_API_KEY;
    private volatile String ultimoError = "";

    public GeminiChatbotService(RepuestoDao repuestoDao) {
        this.repuestoDao = repuestoDao;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        actualizarEstadoInicial();
    }

    private void actualizarEstadoInicial() {
        String k = GeminiConfig.obtenerApiKey();
        estado = (k == null || k.isBlank()) ? EstadoIA.SIN_API_KEY : EstadoIA.IA_ACTIVA;
    }

    public EstadoIA getEstado() { return estado; }
    public String getUltimoError() { return ultimoError; }
    public boolean isIaDisponible() { return estado == EstadoIA.IA_ACTIVA; }

    public interface RepuestoDao {
        ResultadoBusqueda buscar(String descripcion, String marca, String modelo, Integer anio);
    }

    public static class ResultadoBusqueda {
        private final boolean encontrado;
        private final String codigo;
        private final String nombre;
        private final int stock;
        private final Double precio;

        public ResultadoBusqueda(boolean encontrado, String codigo, String nombre, int stock, Double precio) {
            this.encontrado = encontrado;
            this.codigo = codigo;
            this.nombre = nombre;
            this.stock = stock;
            this.precio = precio;
        }
        public static ResultadoBusqueda noEncontrado() { return new ResultadoBusqueda(false, null, null, 0, null); }
        public boolean isEncontrado() { return encontrado; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public int getStock() { return stock; }
        public Double getPrecio() { return precio; }
    }

    public static class RespuestaChat {
        public final String texto;
        public final EstadoIA estado;
        public final boolean esSinIA; // true si fallback local
        public RespuestaChat(String texto, EstadoIA estado, boolean esSinIA) {
            this.texto = texto; this.estado = estado; this.esSinIA = esSinIA;
        }
    }

    public RespuestaChat preguntarConEstado(String textoUsuario) {
        String r = preguntar(textoUsuario);
        boolean sinIA = estado != EstadoIA.IA_ACTIVA;
        // si no había key, sinIA=true; si había key pero falló, también sinIA
        // si key existe y no falló, es IA activa
        return new RespuestaChat(r, estado, sinIA && !textoUsuario.isBlank());
    }

    public String preguntar(String textoUsuario) {
        if (textoUsuario == null || textoUsuario.isBlank()) {
            return "Escribe el nombre del repuesto que buscas, ej: 'tapa radiador aveo'.";
        }
        String apiKey = GeminiConfig.obtenerApiKey();
        boolean tieneGemini = apiKey != null && !apiKey.isBlank();
        if (!tieneGemini) {
            estado = EstadoIA.SIN_API_KEY;
            ultimoError = "Sin API key: configura en Ayuda > Configurar IA o env GEMINI_API_KEY";
            return prefijoSinIA(EstadoIA.SIN_API_KEY) + preguntarSoloLocal(textoUsuario);
        }
        try {
            String r = preguntarConGemini(textoUsuario, apiKey);
            estado = EstadoIA.IA_ACTIVA;
            ultimoError = "";
            return r;
        } catch (GeminiException ge) {
            estado = ge.estado;
            ultimoError = ge.getMessage();
            LOGGER.log(Level.WARNING, "Gemini falló (" + ge.estado + "): " + ge.getMessage());
            String local = preguntarSoloLocal(textoUsuario);
            return prefijoSinIA(ge.estado) + local + "\n\n(" + ge.mensajeUsuario() + ")";
        } catch (Exception e) {
            estado = EstadoIA.SIN_IA_CONEXION;
            ultimoError = e.getMessage();
            LOGGER.log(Level.WARNING, "Error chatbot fallback local", e);
            return prefijoSinIA(EstadoIA.SIN_IA_CONEXION) + preguntarSoloLocal(textoUsuario);
        }
    }

    private String prefijoSinIA(EstadoIA e) {
        return switch (e) {
            case SIN_API_KEY -> "⚠️ sin IA — no hay API key configurada\n";
            case SIN_IA_TOKENS -> "⚠️ sin IA — sin tokens/cuota agotada (429)\n";
            case SIN_IA_SERVIDOR -> {
                if (ultimoError != null && ultimoError.contains("404")) yield "⚠️ sin IA — modelo no disponible (404)\n";
                yield "⚠️ sin IA — servidor Gemini no disponible\n";
            }
            case SIN_IA_CONEXION -> "⚠️ sin IA — sin conexión/timeout\n";
            case SIN_IA_KEY_INVALIDA -> "⚠️ sin IA — API key inválida (403)\n";
            default -> "⚠️ sin IA\n";
        };
    }

    private String preguntarSoloLocal(String texto) {
        ResultadoBusqueda r = repuestoDao.buscar(texto.trim(), null, null, null);
        return formatearRespuesta(r, texto);
    }

    private String preguntarConGemini(String texto, String apiKey) throws Exception {
        // Prompt único: extracción + sugerencia por síntoma
        String prompt = """
                Eres asesor experto de repuestos automotrices para ERP SysTagRep.
                Inventario: cada repuesto tiene descripcion con compatibilidad embedida ej: "TAPA RADIADOR CHEV AVEO/EMOTION/SPART", "FILTRO ACEITE TOYOTA HILUX".
                Tarea: del mensaje del usuario extrae/sugiere el repuesto a buscar.
                - Si menciona repuesto directo ("tapa radiador aveo") -> descripcion = ese repuesto.
                - Si menciona síntoma ("ruido al frenar", "vibra al frenar", "no enciende") -> sugiere el repuesto más probable (pastilla freno, disco, batería, etc) y explica breve motivo.
                - Extrae marca/modelo/anio si aparecen.
                Responde SOLO JSON válido sin markdown:
                {"descripcion":"pastilla freno","marca":"CHEV","modelo":"AVEO","anio":null,"motivo":"Ruido al frenar suele indicar desgaste de pastillas","esSintoma":true}
                Si es repuesto directo, motivo=null y esSintoma=false.
                Mensaje usuario: "%s"
                Ejemplo síntoma: {"descripcion":"pastilla freno","marca":null,"modelo":null,"anio":null,"motivo":"Ruido metálico al frenar indica pastillas desgastadas","esSintoma":true}
                Ejemplo directo: {"descripcion":"tapa radiador","marca":"CHEV","modelo":"AVEO","anio":2015,"motivo":null,"esSintoma":false}
                """.formatted(texto.replace("\"", "'").replace("\n", " "));

        String jsonExtraido = llamarGemini(prompt, apiKey);
        String descripcion = texto;
        String marca = null;
        String modelo = null;
        Integer anio = null;
        String motivo = null;
        boolean esSintoma = false;

        try {
            JsonNode node = MAPPER.readTree(jsonExtraido);
            if (node.has("descripcion") && !node.get("descripcion").isNull()) {
                String d = node.get("descripcion").asText().trim();
                if (!d.isBlank() && !"null".equalsIgnoreCase(d)) descripcion = d;
            }
            if (node.has("marca") && !node.get("marca").isNull()) {
                String v = node.get("marca").asText().trim();
                if (!v.isBlank() && !"null".equalsIgnoreCase(v)) marca = v;
            }
            if (node.has("modelo") && !node.get("modelo").isNull()) {
                String v = node.get("modelo").asText().trim();
                if (!v.isBlank() && !"null".equalsIgnoreCase(v)) modelo = v;
            }
            if (node.has("anio") && !node.get("anio").isNull() && node.get("anio").isNumber()) anio = node.get("anio").asInt();
            if (node.has("motivo") && !node.get("motivo").isNull()) {
                String m = node.get("motivo").asText().trim();
                if (!m.isBlank() && !"null".equalsIgnoreCase(m)) motivo = m;
            }
            if (node.has("esSintoma") && node.get("esSintoma").isBoolean()) esSintoma = node.get("esSintoma").asBoolean();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo parsear JSON Gemini, usando texto bruto: " + jsonExtraido, e);
        }

        String descripcionBusqueda = descripcion;
        if (marca != null) descripcionBusqueda += " " + marca;
        if (modelo != null) descripcionBusqueda += " " + modelo;

        ResultadoBusqueda r = repuestoDao.buscar(descripcionBusqueda, marca, modelo, anio);
        if (!r.isEncontrado() && !descripcionBusqueda.equals(descripcion)) r = repuestoDao.buscar(descripcion, null, null, null);
        if (!r.isEncontrado() && !descripcion.equals(texto)) r = repuestoDao.buscar(texto, null, null, null);

        String respuestaLocal = formatearRespuesta(r, descripcionBusqueda);

        // Si es síntoma, anteponer motivo IA
        if (esSintoma && motivo != null) {
            respuestaLocal = "💡 Sugerencia IA: " + motivo + " → buscando \"" + descripcion + "\"\n" + respuestaLocal;
        }

        // Redacción final breve con IA (opcional, no falla si error)
        try {
            String promptRespuesta = """
                    Eres asistente SysTagRep. Responde breve en español (máx 3 líneas).
                    Consulta: "%s"
                    Sugerencia IA: %s
                    Resultado inventario: %s
                    Si hay stock indica código, nombre, stock y precio. Si no hay, di que no hay stock pero mantén la sugerencia.
                    No inventes stock/precios.
                    """.formatted(texto.replace("\"", "'"), motivo != null ? motivo : "repuesto directo", respuestaLocal.replace("\"", "'"));
            String redaccion = llamarGemini(promptRespuesta, apiKey);
            if (redaccion != null && !redaccion.isBlank() && redaccion.length() < 700) {
                // Si era síntoma, asegurar que la redacción incluya el motivo; si no, usar redacción tal cual
                return redaccion.trim();
            }
        } catch (GeminiException ge) {
            throw ge; // propagar para badge sin IA
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Fallo redacción Gemini", e);
        }
        return respuestaLocal;
    }

    private String formatearRespuesta(ResultadoBusqueda r, String consulta) {
        if (r == null || !r.isEncontrado()) {
            return "No encontré repuestos para \"" + consulta.trim() + "\" en inventario. Prueba con código o descripción más simple ej: 'filtro aceite'.";
        }
        String precioStr = r.getPrecio() != null ? String.format("$%.2f", r.getPrecio()) : "precio no registrado";
        String stockStr = r.getStock() > 0 ? r.getStock() + " unidades" : "sin stock";
        return String.format("Encontrado: %s - %s | Stock: %s | Precio: %s",
                r.getCodigo() != null ? r.getCodigo() : "sin código", r.getNombre(), stockStr, precioStr);
    }

    private static class GeminiException extends Exception {
        final EstadoIA estado;
        GeminiException(EstadoIA estado, String msg) { super(msg); this.estado = estado; }
        String mensajeUsuario() {
            return switch (estado) {
                case SIN_IA_TOKENS -> "Cuota/tokens agotados. Revisa https://aistudio.google.com";
                case SIN_IA_SERVIDOR -> "Servidor Gemini temporalmente no disponible, intenta en minutos";
                case SIN_IA_CONEXION -> "Sin conexión a internet o timeout";
                case SIN_IA_KEY_INVALIDA -> "API key inválida o sin permisos";
                default -> getMessage();
            };
        }
    }

    private String llamarGemini(String prompt, String apiKey) throws Exception {
        try {
            return llamarGeminiConModelo(prompt, apiKey, GEMINI_MODEL_PRIMARY);
        } catch (GeminiException e) {
            boolean es404 = e.getMessage().contains("404");
            boolean es503 = e.getMessage().contains("503") || e.getMessage().contains("UNAVAILABLE");
            if (e.estado == EstadoIA.SIN_IA_SERVIDOR && (es404 || es503)) {
                LOGGER.log(Level.WARNING, "Modelo " + GEMINI_MODEL_PRIMARY + " falló (" + e.getMessage().substring(0, Math.min(80, e.getMessage().length())) + "), probando " + GEMINI_MODEL_FALLBACK);
                try {
                    return llamarGeminiConModelo(prompt, apiKey, GEMINI_MODEL_FALLBACK);
                } catch (GeminiException e2) {
                    boolean e2_404 = e2.getMessage().contains("404");
                    boolean e2_503 = e2.getMessage().contains("503") || e2.getMessage().contains("UNAVAILABLE");
                    if (e2.estado == EstadoIA.SIN_IA_SERVIDOR && (e2_404 || e2_503)) {
                        LOGGER.log(Level.WARNING, "Modelo " + GEMINI_MODEL_FALLBACK + " falló, probando " + GEMINI_MODEL_FALLBACK2);
                        return llamarGeminiConModelo(prompt, apiKey, GEMINI_MODEL_FALLBACK2);
                    }
                    throw e2;
                }
            }
            throw e;
        }
    }

    private String llamarGeminiConModelo(String prompt, String apiKey, String modelo) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo + ":generateContent?key=" + apiKey;
        com.fasterxml.jackson.databind.node.ObjectNode root = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
        com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
        content.put("role", "user");
        com.fasterxml.jackson.databind.node.ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);
        String body = MAPPER.writeValueAsString(root);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp;
        try {
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new GeminiException(EstadoIA.SIN_IA_CONEXION, "Timeout 20s: " + e.getMessage());
        } catch (java.io.IOException e) {
            throw new GeminiException(EstadoIA.SIN_IA_CONEXION, "Sin conexión: " + e.getMessage());
        }

        int code = resp.statusCode();
        String b = resp.body();
        if (code == 404) throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, "HTTP 404 modelo " + modelo + " no encontrado: " + b);
        if (code == 429) throw new GeminiException(EstadoIA.SIN_IA_TOKENS, "429 quota: " + b);
        if (code == 401 || code == 403) throw new GeminiException(EstadoIA.SIN_IA_KEY_INVALIDA, "HTTP " + code + ": " + b);
        if (code == 400) {
            String lower = b.toLowerCase();
            if (lower.contains("api_key") || lower.contains("api key") || lower.contains("permission_denied") || lower.contains("api_key_invalid")) {
                throw new GeminiException(EstadoIA.SIN_IA_KEY_INVALIDA, "HTTP 400 key: " + b);
            }
            throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, "HTTP 400 payload: " + b);
        }
        if (code >= 500) throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, "HTTP " + code + ": " + b);
        if (code != 200) throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, "HTTP " + code + ": " + b);

        JsonNode respRoot = MAPPER.readTree(b);
        JsonNode candidates = respRoot.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode respParts = candidates.get(0).path("content").path("parts");
            if (respParts.isArray() && !respParts.isEmpty()) {
                String text = respParts.get(0).path("text").asText().trim();
                if (text.startsWith("```")) {
                    int first = text.indexOf('\n');
                    int last = text.lastIndexOf("```");
                    if (first >= 0 && last > first) text = text.substring(first + 1, last).trim();
                }
                return text;
            }
        }
        throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, "Respuesta sin candidates: " + b);
    }
}
