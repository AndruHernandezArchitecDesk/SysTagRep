package com.vendex.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.config.GeminiConfig;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chatbot con sugerencia IA.
 * - Si hay GEMINI_API_KEY (env var o ~/.vendex/gemini.properties), usa Gemini para:
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
    private static final Duration TIMEOUT = Duration.ofSeconds(45);
    private static final Duration PAUSA_SERVIDOR = Duration.ofSeconds(60);
    private static final Duration PAUSA_MODELO_NO_ENCONTRADO = Duration.ofMinutes(10);

    public enum EstadoIA {
        IA_ACTIVA,          // key presente y último call ok
        SIN_API_KEY,        // no hay key en env ni archivo
        SIN_IA_TOKENS,      // 429 / quota / RESOURCE_EXHAUSTED
        SIN_IA_SERVIDOR,    // 5xx
        SIN_IA_CONEXION,    // timeout / network
        SIN_IA_KEY_INVALIDA // 400/401/403
    }

    private final RepuestoDao repuestoDao;
    private final HerramientasAgente herramientas;
    private final HttpClient httpClient;
    private final URI modelosUri;
    private final Clock reloj;
    private final java.util.Map<String, Long> modelosEnPausa = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile EstadoIA estado = EstadoIA.SIN_API_KEY;
    private volatile String ultimoError = "";
    private volatile boolean modoAhorro = true;
    private volatile com.vendex.service.VendexiConsumoService consumo;

    public GeminiChatbotService(RepuestoDao repuestoDao) {
        this(repuestoDao, null);
    }

    /** Agente IA: herramientas de lectura del ERP (null = solo búsqueda de repuestos). */
    public GeminiChatbotService(RepuestoDao repuestoDao, HerramientasAgente herramientas) {
        this(repuestoDao, herramientas, URI.create("https://generativelanguage.googleapis.com/v1beta/models/"), Clock.systemUTC());
    }

    /** Transporte HTTP local y reloj controlado para probar fallos sin llamar a Google. */
    GeminiChatbotService(RepuestoDao repuestoDao, HerramientasAgente herramientas, URI modelosUri, Clock reloj) {
        this.repuestoDao = repuestoDao;
        this.herramientas = herramientas;
        this.modelosUri = modelosUri;
        this.reloj = reloj;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        actualizarEstadoInicial();
    }

    /** Fase 3: medición/capacidad — lazy para no acoplar AppContext en tests del chatbot. */
    private com.vendex.service.VendexiConsumoService consumoService() {
        if (consumo == null) {
            synchronized (this) {
                if (consumo == null) {
                    try {
                        consumo = com.vendex.config.AppContext.getInstance().vendexiConsumoService;
                    } catch (Exception e) {
                        consumo = new com.vendex.service.VendexiConsumoService(new com.vendex.dao.VendexiConsumoDAOPostgres());
                    }
                }
            }
        }
        return consumo;
    }

    private void actualizarEstadoInicial() {
        String k = GeminiConfig.obtenerApiKey();
        estado = (k == null || k.isBlank()) ? EstadoIA.SIN_API_KEY : EstadoIA.IA_ACTIVA;
    }

    public EstadoIA getEstado() { return estado; }
    public String getUltimoError() { return ultimoError; }
    public boolean isIaDisponible() { return estado == EstadoIA.IA_ACTIVA; }
    protected boolean modoAhorro() { return modoAhorro; }

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
        /** Rol autorizado para esta respuesta; null en modo local, GENERAL o bloqueo. */
        public final String rolIA;
        /** Roles alternativos que faltan en la licencia; vacío si no hay bloqueo. */
        public final java.util.List<String> rolesBloqueados;
        public RespuestaChat(String texto, EstadoIA estado, boolean esSinIA) {
            this(texto, estado, esSinIA, null, java.util.List.of());
        }
        public RespuestaChat(String texto, EstadoIA estado, boolean esSinIA,
                             String rolIA, java.util.List<String> rolesBloqueados) {
            this.texto = texto; this.estado = estado; this.esSinIA = esSinIA;
            this.rolIA = rolIA;
            this.rolesBloqueados = java.util.List.copyOf(rolesBloqueados);
        }
    }

    /** Acciones del agente: qué herramienta local ejecutar para la consulta. */
    public enum Accion {
        BUSCAR_REPUESTO,     // flujo clásico: síntoma/repuesto -> ILIKE inventario
        CONSULTAR_PRODUCTO,  // stock/precio/costo/margen de un producto
        VENTAS_CLIENTE,      // facturas totales de un cliente
        CUENTAS_COBRAR,      // saldo pendiente de un cliente
        RENTABILIDAD,        // top margen + top vendidos
        GENERAL              // charla general, sin herramienta
    }

    static final class Clasificacion {
        Accion accion = Accion.BUSCAR_REPUESTO;
        String descripcion;
        String marca;
        String modelo;
        Integer anio;
        String motor;
        String motivo;
        boolean esSintoma;
        String cliente;
    }

    /**
     * Roles IA de la licencia (spec §4) requeridos por cada acción. Lista vacía = sin rol.
     * Repuestos/stock: acepta VENTAS <b>o</b> INVENTARIO (un vendedor también necesita
     * consultar disponibilidad); ventas de cliente: VENTAS; saldos/rentabilidad: FINANCIERO.
     */
    static java.util.List<String> rolesDeAccion(Accion a) {
        if (a == null) return java.util.List.of();
        return switch (a) {
            case BUSCAR_REPUESTO, CONSULTAR_PRODUCTO -> java.util.List.of(
                    com.vendex.util.LicenseManager.ROL_VENTAS,
                    com.vendex.util.LicenseManager.ROL_INVENTARIO);
            case VENTAS_CLIENTE -> java.util.List.of(com.vendex.util.LicenseManager.ROL_VENTAS);
            case CUENTAS_COBRAR, RENTABILIDAD -> java.util.List.of(com.vendex.util.LicenseManager.ROL_FINANCIERO);
            case GENERAL -> java.util.List.of();
        };
    }

    /** true si la licencia permite al menos uno de los roles requeridos (lista vacía = permitido). */
    protected boolean rolesPermitidos(java.util.List<String> roles) {
        if (roles == null || roles.isEmpty()) return true;
        for (String r : roles) if (rolPermitido(r)) return true;
        return false;
    }

    /** Conserva la especialidad INVENTARIO cuando está habilitada; si no, usa VENTAS. */
    String rolEfectivo(Accion accion) {
        java.util.List<String> roles = rolesDeAccion(accion);
        if (roles.contains(com.vendex.util.LicenseManager.ROL_INVENTARIO)
                && rolPermitido(com.vendex.util.LicenseManager.ROL_INVENTARIO)) {
            return com.vendex.util.LicenseManager.ROL_INVENTARIO;
        }
        for (String rol : roles) if (rolPermitido(rol)) return rol;
        return null;
    }

    /**
     * true si la licencia permite usar el rol dado. Por defecto consulta la licencia
     * almacenada (v1 / sin IA / sin roles = permitido). Protected para tests.
     */
    protected boolean rolPermitido(String rol) {
        return com.vendex.util.LicenseManager.licenciaPermiteRolIA(rol);
    }

    private String mensajeRolNoIncluido(Accion a, java.util.List<String> roles) {
        String para = switch (a) {
            case VENTAS_CLIENTE -> "consultas de ventas de clientes";
            case CUENTAS_COBRAR -> "saldos por cobrar";
            case RENTABILIDAD -> "ranking de rentabilidad";
            case CONSULTAR_PRODUCTO -> "stock y precios de repuestos";
            default -> "búsqueda de repuestos";
        };
        return "🔒 Tu licencia no incluye los roles IA [" + String.join(", ", roles) +
               "] requeridos para " + para +
               ".\nPídele al administrador que habilite uno de ellos en la licencia.";
    }

    /**
     * Parsea la respuesta JSON del clasificador. Devuelve null si el JSON es inutilizable
     * (el llamador degrada al flujo clásico de búsqueda de repuestos).
     * Si falta "accion", asume BUSCAR_REPUESTO (compatibilidad con prompts/gemini cacheados).
     */
    static Clasificacion parsearClasificacion(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = MAPPER.readTree(json);
            Clasificacion c = new Clasificacion();
            if (node.has("accion") && !node.get("accion").isNull()) {
                String a = node.get("accion").asText().trim().toUpperCase(java.util.Locale.ROOT);
                try {
                    c.accion = Accion.valueOf(a);
                } catch (IllegalArgumentException ignorada) {
                    c.accion = Accion.BUSCAR_REPUESTO;
                }
            }
            if (node.has("descripcion") && !node.get("descripcion").isNull()) {
                String d = node.get("descripcion").asText().trim();
                if (!d.isBlank() && !"null".equalsIgnoreCase(d)) c.descripcion = d;
            }
            if (node.has("marca") && !node.get("marca").isNull()) {
                String v = node.get("marca").asText().trim();
                if (!v.isBlank() && !"null".equalsIgnoreCase(v)) c.marca = v;
            }
            if (node.has("modelo") && !node.get("modelo").isNull()) {
                String v = node.get("modelo").asText().trim();
                if (!v.isBlank() && !"null".equalsIgnoreCase(v)) c.modelo = v;
            }
            if (node.has("anio") && !node.get("anio").isNull() && node.get("anio").isNumber()) {
                c.anio = node.get("anio").asInt();
            }
            if (node.has("motor") && !node.get("motor").isNull()) {
                String v = node.get("motor").asText().trim();
                if (!v.isBlank() && !"null".equalsIgnoreCase(v)) c.motor = v;
            }
            if (node.has("motivo") && !node.get("motivo").isNull()) {
                String m = node.get("motivo").asText().trim();
                if (!m.isBlank() && !"null".equalsIgnoreCase(m)) c.motivo = m;
            }
            if (node.has("esSintoma") && node.get("esSintoma").isBoolean()) c.esSintoma = node.get("esSintoma").asBoolean();
            if (node.has("cliente") && !node.get("cliente").isNull()) {
                String cl = node.get("cliente").asText().trim();
                if (!cl.isBlank() && !"null".equalsIgnoreCase(cl)) c.cliente = cl;
            }
            return c;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo parsear JSON del clasificador: " + json, e);
            return null;
        }
    }

    public RespuestaChat preguntarConEstado(String textoUsuario) {
        return preguntarConEstado(textoUsuario, null, null, null);
    }

    /**
     * @param vehMarca/modelo/anio filtros explícitos elegidos en la UI (tienen prioridad
     *                             sobre lo que extraiga Gemini del texto).
     */
    public RespuestaChat preguntarConEstado(String textoUsuario, String vehMarca, String vehModelo, Integer vehAnio) {
        if (textoUsuario == null || textoUsuario.isBlank()) {
            return new RespuestaChat("Escribe el nombre del repuesto que buscas, ej: 'tapa radiador aveo'.", estado, false);
        }
        if (com.vendex.service.InvestigacionWebService.esCodigoRepuesto(textoUsuario)) {
            return new RespuestaChat("Búsqueda local de inventario:\n"
                    + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio), estado, true);
        }
        // Fase 1: gate de licencia — si la licencia no incluye Vendexi AI, no se llama al proveedor.
        // La búsqueda local (función ERP permitida) sigue disponible.
        if (!com.vendex.util.LicenseManager.licenciaIncluyeIA()) {
            estado = EstadoIA.SIN_API_KEY;
            ultimoError = "La licencia de esta instalación no incluye Vetxi AI";
            return new RespuestaChat("ℹ️ La licencia de esta instalación no incluye Vetxi AI.\n" +
                   "Búsqueda local en inventario:\n" + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio), estado, true);
        }
        // Fase 3: gate de capacidad asignada (spec §10.2) — el ERP local sigue disponible.
        if (!consumoService().hayCapacidadDisponible()) {
            estado = EstadoIA.SIN_IA_TOKENS;
            ultimoError = com.vendex.service.VendexiConsumoService.mensajeCapacidadAgotada();
            return new RespuestaChat("⚠️ capacidad de IA agotada\n" +
                   com.vendex.service.VendexiConsumoService.mensajeCapacidadAgotada() + "\n\n" +
                   "Búsqueda local en inventario:\n" + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio), estado, true);
        }
        String apiKey = GeminiConfig.obtenerApiKey();
        boolean tieneGemini = apiKey != null && !apiKey.isBlank();
        if (!tieneGemini) {
            estado = EstadoIA.SIN_API_KEY;
            ultimoError = "Sin API key: configura en Ayuda > Configurar IA o env GEMINI_API_KEY";
            return new RespuestaChat(prefijoSinIA(EstadoIA.SIN_API_KEY) + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio), estado, true);
        }
        try {
            RespuestaChat r = preguntarConGeminiRespuesta(textoUsuario, apiKey, vehMarca, vehModelo, vehAnio);
            estado = EstadoIA.IA_ACTIVA;
            ultimoError = "";
            return r;
        } catch (GeminiException ge) {
            estado = ge.estado;
            ultimoError = ge.getMessage();
            LOGGER.log(Level.WARNING, "Gemini falló (" + ge.estado + "): " + ge.getMessage());
            String local = preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio);
            return new RespuestaChat(prefijoSinIA(ge.estado) + local + "\n\n(" + ge.mensajeUsuario() + ")", estado, true);
        } catch (Exception e) {
            estado = EstadoIA.SIN_IA_CONEXION;
            ultimoError = e.getMessage();
            LOGGER.log(Level.WARNING, "Error chatbot fallback local", e);
            return new RespuestaChat(prefijoSinIA(EstadoIA.SIN_IA_CONEXION) + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio), estado, true);
        }
    }

    public String preguntar(String textoUsuario) {
        return preguntar(textoUsuario, null, null, null);
    }

    public String preguntar(String textoUsuario, String vehMarca, String vehModelo, Integer vehAnio) {
        return preguntarConEstado(textoUsuario, vehMarca, vehModelo, vehAnio).texto;
    }

    private String prefijoSinIA(EstadoIA e) {
        return switch (e) {
            case SIN_API_KEY -> "⚠️ sin IA — no hay API key configurada\n";
            case SIN_IA_TOKENS -> "⚠️ IA limitada — revisar cuotas y capacidad\n";
            case SIN_IA_SERVIDOR -> {
                if (ultimoError != null && ultimoError.contains("404")) yield "⚠️ sin IA — modelo no disponible (404)\n";
                yield "⚠️ sin IA — servidor Gemini no disponible\n";
            }
            case SIN_IA_CONEXION -> "⚠️ sin IA — sin conexión/timeout\n";
            case SIN_IA_KEY_INVALIDA -> "⚠️ sin IA — API key inválida (403)\n";
            default -> "⚠️ sin IA\n";
        };
    }

    private String preguntarSoloLocal(String texto, String vehMarca, String vehModelo, Integer vehAnio) {
        ResultadoBusqueda r = repuestoDao.buscar(texto.trim(), vehMarca, vehModelo, vehAnio);
        if (!r.isEncontrado() && (vehMarca != null || vehModelo != null || vehAnio != null)) {
            r = repuestoDao.buscar(texto.trim(), null, null, null);
        }
        return formatearRespuesta(r, texto);
    }

    String preguntarConGemini(String texto, String apiKey, String vehMarca, String vehModelo, Integer vehAnio) throws Exception {
        return preguntarConGeminiRespuesta(texto, apiKey, vehMarca, vehModelo, vehAnio).texto;
    }

    RespuestaChat preguntarConGeminiRespuesta(String texto, String apiKey, String vehMarca, String vehModelo, Integer vehAnio) throws Exception {
        // Paso 1: clasificar la intención + extraer parámetros (un solo JSON)
        String json = llamarGemini(promptClasificacion(texto), apiKey);

        Clasificacion c = parsearClasificacion(json);
        if (c == null) {
            // JSON inutilizable: degradar al flujo clásico de búsqueda de repuestos
            c = new Clasificacion();
            c.descripcion = texto;
        } else if (c.descripcion == null || c.descripcion.isBlank()) {
            c.descripcion = texto;
        }
        // Filtros explícitos de la UI tienen prioridad sobre lo extraído por Gemini
        if (vehMarca != null && !vehMarca.isBlank()) c.marca = vehMarca;
        if (vehModelo != null && !vehModelo.isBlank()) c.modelo = vehModelo;
        if (vehAnio != null) c.anio = vehAnio;

        // Rol IA de la licencia (§4): sin rol, no se ejecuta herramienta ni se gasta redacción
        java.util.List<String> roles = rolesDeAccion(c.accion);
        String rol = rolEfectivo(c.accion);
        if (!roles.isEmpty() && rol == null) {
            return new RespuestaChat(mensajeRolNoIncluido(c.accion, roles), EstadoIA.IA_ACTIVA, false, null, roles);
        }

        // Paso 2: ejecutar la herramienta local correspondiente (datos reales del ERP)
        String respuestaLocal = ejecutarLocal(c, texto);

        if (modoAhorro() && c.accion != Accion.GENERAL && !respuestaLocal.isBlank()) {
            return new RespuestaChat(respuestaLocal, EstadoIA.IA_ACTIVA, false, rol, java.util.List.of());
        }

        // Paso 3: redacción breve con IA (best-effort, no invalida los datos locales)
        try {
            String promptRespuesta = promptRedaccion(c, texto, respuestaLocal, rol);
            String redaccion = llamarGemini(promptRespuesta, apiKey);
            if (redaccion != null && !redaccion.isBlank() && redaccion.length() < 700) {
                return new RespuestaChat(redaccion.trim(), EstadoIA.IA_ACTIVA, false, rol, java.util.List.of());
            }
        } catch (GeminiException ge) {
            LOGGER.log(Level.FINE, "Redacción Gemini falló (se usa respuesta local): " + ge.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Fallo redacción Gemini", e);
        }
        if (respuestaLocal.isBlank()) {
            // GENERAL sin redacción posible: ofrecer las capacidades reales del agente
            return new RespuestaChat("Puedo ayudarte con repuestos y síntomas, stock y precios, ventas de un cliente, " +
                   "saldos por cobrar y rentabilidad. Escribe tu consulta.", EstadoIA.IA_ACTIVA, false);
        }
        return new RespuestaChat(respuestaLocal, EstadoIA.IA_ACTIVA, false, rol, java.util.List.of());
    }

    private String promptClasificacion(String texto) {
        return """
                Eres clasificador de consultas para el ERP Vendex (asistente de repuestos automotrices).
                Inventario: descripcion con compatibilidad embedida ej: "TAPA RADIADOR CHEV AVEO/EMOTION/SPART".
                Clasifica el mensaje del usuario en UNA accion:
                - "BUSCAR_REPUESTO": busca/describe un repuesto o menciona un sintoma (ruido al frenar, no enciende...)
                - "CONSULTAR_PRODUCTO": pide stock, precio, costo, existencia o margen de un producto especifico
                - "VENTAS_CLIENTE": pide ventas, compras, totales o facturas de un cliente
                - "CUENTAS_COBRAR": pide saldo, cuanto debe, adelanto o creditos de un cliente
                - "RENTABILIDAD": pide productos mas rentables, margenes, mas vendidos o ranking
                - "GENERAL": saludos o preguntas generales ajenas a lo anterior
                Extrae ademas: descripcion (repuesto a buscar), cliente (nombre o ruc si aparece),
                marca, modelo, anio, motor (si el usuario lo menciona), motivo (si es sintoma), esSintoma.
                Responde SOLO JSON valido sin markdown:
                {"accion":"BUSCAR_REPUESTO","descripcion":"pastilla freno","cliente":null,"marca":"CHEV","modelo":"AVEO","anio":null,"motor":null,"motivo":null,"esSintoma":false}
                Ejemplo venta cliente: {"accion":"VENTAS_CLIENTE","descripcion":null,"cliente":"Juan Perez",...}
                Ejemplo stock: {"accion":"CONSULTAR_PRODUCTO","descripcion":"tapa radiador",...}
                Ejemplo compatibilidad vehicular: {"accion":"BUSCAR_REPUESTO","descripcion":"bujias","marca":"Toyota","modelo":"Corolla","anio":2015,"motor":"1.8",...}
                Ejemplo sintoma: {"accion":"BUSCAR_REPUESTO","descripcion":"pastilla freno","motivo":"Ruido al frenar suele indicar desgaste","esSintoma":true,...}
                Mensaje usuario: "%s"
                """.formatted(texto.replace("\"", "'").replace("\n", " "));
    }

    /** Personalidad del rol IA contextual según la acción clasificada (§4). */
    String rolContextual(Accion a) {
        return personalidadRol(rolEfectivo(a));
    }

    private String personalidadRol(String rol) {
        return switch (rol != null ? rol : "") {
            case "VENTAS" -> "Actúas como asesor comercial de Vendex (rol VENTAS): céntrate en repuestos, disponibilidad, precios y relación con el cliente, facturas y montos.";
            case "FINANCIERO" -> "Actúas como analista financiero de Vendex (rol FINANCIERO): céntrate en cifras, saldos y márgenes, con tono preciso.";
            case "INVENTARIO" -> "Actúas como especialista en inventario de repuestos (rol INVENTARIO): céntrate en disponibilidad, compatibilidad y precios.";
            default -> "Actúas como asistente general de Vendex: sé amable y breve.";
        };
    }

    String promptRedaccion(Clasificacion c, String texto, String datosLocales) {
        return promptRedaccion(c, texto, datosLocales, rolEfectivo(c.accion));
    }

    private String promptRedaccion(Clasificacion c, String texto, String datosLocales, String rol) {
        String regla = switch (c.accion) {
            case CONSULTAR_PRODUCTO ->
                    "Resume stock y precio. Si los datos incluyen costo registrado sin IVA, margen y proveedor, explica " +
                    "esos valores; diferencia bruta = precio registrado menos costo registrado, margen = diferencia/precio. " +
                    "No lo presentes como utilidad neta ni inventes descuentos, impuestos o componentes no incluidos en los datos.";
            case VENTAS_CLIENTE, CUENTAS_COBRAR, RENTABILIDAD ->
                    "Si hay datos, resume los montos/stock relevantes. Si no hay, di que no se encontraron. No inventes montos, stock ni precios.";
            case GENERAL -> "Responde amable y breve en español (max 3 lineas). Puedes ofrecer ayudar con repuestos, stock, ventas, saldos y rentabilidad.";
            default -> "Si hay stock indica código, nombre, stock y precio. Si no hay, di que no hay stock pero mantén la sugerencia. No inventes stock/precios. " +
                    "Si los datos incluyen complementos, menciona hasta dos con su código; son asociaciones locales, no compatibilidad técnica confirmada.";
        };
        String motivo = c.motivo != null ? c.motivo : (c.esSintoma ? "sintoma" : "consulta directa");
        return """
                Eres Vetxi, el agente de Vendex. %s Responde breve en español (max 3 lineas).
                Consulta: "%s"
                Motivo/sugerencia IA: %s
                Datos reales del ERP: %s
                %s
                """.formatted(personalidadRol(rol), texto.replace("\"", "'"), motivo,
                (datosLocales != null ? datosLocales : "").replace("\"", "'"), regla);
    }

    /**
     * Ejecuta la herramienta local de la acción clasificada y formatea la respuesta con
     * datos reales (no inventados). Package-private para tests sin red.
     */
    String ejecutarLocal(Clasificacion c, String texto) {
        java.util.List<String> roles = rolesDeAccion(c.accion);
        if (!rolesPermitidos(roles)) {
            return mensajeRolNoIncluido(c.accion, roles);
        }
        try {
            return switch (c.accion) {
                case CONSULTAR_PRODUCTO -> herramientas != null
                        ? consultarProducto(c, texto)
                        : buscarRepuesto(c, texto);
                case VENTAS_CLIENTE -> herramientas != null
                        ? formatearVentas(c)
                        : buscarRepuesto(c, texto);
                case CUENTAS_COBRAR -> herramientas != null
                        ? formatearSaldo(c)
                        : buscarRepuesto(c, texto);
                case RENTABILIDAD -> herramientas != null
                        ? formatearRentabilidad()
                        : buscarRepuesto(c, texto);
                case GENERAL -> ""; // lo redacta la IA; si falla se ofrece ayuda general
                default -> buscarRepuesto(c, texto);
            };
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Herramienta local falló (" + c.accion + ")", e);
            return buscarRepuesto(c, texto);
        }
    }

    private String consultarProducto(Clasificacion c, String texto) {
        boolean vehicle = c.anio != null
                && ((c.marca != null && !c.marca.isBlank()) || (c.modelo != null && !c.modelo.isBlank()));
        if (vehicle) {
            java.util.List<HerramientasAgente.Producto> compatibles = herramientas.compatibles(
                    c.descripcion, c.marca, c.modelo, c.anio, c.motor, 5);
            if (compatibles != null && !compatibles.isEmpty()) return formatearCompatibles(compatibles, c);
        }
        String criterio = c.descripcion != null ? c.descripcion : texto;
        String productos = formatearProductos(herramientas.productos(criterio, 5), criterio);
        return vehicle ? "No encontré asociaciones explícitas de este producto con el vehículo indicado. "
                + "Los siguientes resultados son solo coincidencias de inventario; verifica la compatibilidad antes de instalar.\n"
                + productos : productos;
    }

    /** Flujo clásico (y fallback): búsqueda de repuestos con cadena de reintentos. */
    private String buscarRepuesto(Clasificacion c, String texto) {
        String descripcion = c.descripcion != null && !c.descripcion.isBlank() ? c.descripcion : texto;
        if (herramientas != null && c.anio != null
                && ((c.marca != null && !c.marca.isBlank()) || (c.modelo != null && !c.modelo.isBlank()))) {
            try {
                java.util.List<HerramientasAgente.Producto> compatibles = herramientas.compatibles(
                        descripcion, c.marca, c.modelo, c.anio, c.motor, 5);
                if (compatibles != null && !compatibles.isEmpty()) {
                    return formatearCompatibles(compatibles, c);
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Compatibilidad estructurada no disponible; se usará la búsqueda de inventario", e);
            }
        }
        String descripcionBusqueda = descripcion;
        if (c.marca != null) descripcionBusqueda += " " + c.marca;
        if (c.modelo != null) descripcionBusqueda += " " + c.modelo;
        if (c.anio != null) descripcionBusqueda += " " + c.anio;
        if (c.motor != null) descripcionBusqueda += " " + c.motor;

        ResultadoBusqueda r = repuestoDao.buscar(descripcionBusqueda.trim(), c.marca, c.modelo, c.anio);
        if (!r.isEncontrado() && !descripcionBusqueda.equals(descripcion)) r = repuestoDao.buscar(descripcion.trim(), c.marca, c.modelo, c.anio);
        if (!r.isEncontrado() && !descripcion.equals(texto)) r = repuestoDao.buscar(texto.trim(), null, null, null);

        String respuestaLocal = formatearRespuesta(r, descripcionBusqueda);
        if (r.isEncontrado() && (c.marca != null || c.modelo != null || c.anio != null)) {
            respuestaLocal = "Resultado de búsqueda de inventario; no se verificó la compatibilidad exacta con el vehículo, año y motor. "
                    + "Confirma la aplicación en la ficha del fabricante antes de instalar.\n" + respuestaLocal;
        }
        if (r.isEncontrado() && herramientas != null) {
            try {
                java.util.List<com.vendex.model.Inventario> complementos = herramientas.complementarios(r.getCodigo(), 2);
                if (complementos != null && !complementos.isEmpty()) {
                    StringBuilder sb = new StringBuilder(respuestaLocal);
                    sb.append("\nComplementos por asociación local (compatibilidad sin confirmar):");
                    java.util.Set<String> vistos = new java.util.HashSet<>();
                    if (r.getCodigo() != null) vistos.add(r.getCodigo().trim().toUpperCase(java.util.Locale.ROOT));
                    int agregados = 0;
                    for (com.vendex.model.Inventario i : complementos) {
                        if (i == null || i.getCantidad() <= 0 || i.getCodigo() == null || i.getCodigo().isBlank()) continue;
                        if (!vistos.add(i.getCodigo().trim().toUpperCase(java.util.Locale.ROOT))) continue;
                        sb.append("\n• ").append(i.getCodigo()).append(" - ").append(i.getDescripcion())
                          .append(" | stock: ").append(i.getCantidad()).append(" | precio: ")
                          .append(i.getPrecioVenta() != null
                                  ? String.format(java.util.Locale.US, "$%.2f", i.getPrecioVenta()) : "n/d");
                        if (++agregados >= 2) break;
                    }
                    if (agregados > 0) respuestaLocal = sb.toString();
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Complementos no disponibles; se conserva el repuesto encontrado", e);
            }
        }
        if (c.esSintoma && c.motivo != null) {
            respuestaLocal = "💡 Sugerencia IA: " + c.motivo + " → buscando \"" + descripcion + "\"\n" + respuestaLocal;
        }
        return respuestaLocal;
    }

    private String formatearCompatibles(java.util.List<HerramientasAgente.Producto> productos, Clasificacion c) {
        StringBuilder sb = new StringBuilder("Vendex tiene estos repuestos asociados explícitamente al vehículo en su catálogo de compatibilidad: ")
                .append(c.marca == null ? "" : c.marca).append(' ')
                .append(c.modelo == null ? "" : c.modelo).append(' ').append(c.anio);
        if (c.motor != null) sb.append(" motor ").append(c.motor);
        sb.append(". Esta asociación proviene del catálogo Vendex; confirma la especificación del fabricante antes de instalar.");
        for (HerramientasAgente.Producto p : productos) {
            sb.append("\n• ").append(p.codigo() == null ? "sin código" : p.codigo()).append(" - ")
                    .append(p.descripcion()).append(" | stock: ").append(p.stock()).append(" | precio: ")
                    .append(p.precioVenta() == null ? "n/d" : String.format(java.util.Locale.US, "$%.2f", p.precioVenta()));
            if (p.costoSinIva() != null && p.precioVenta() != null) {
                sb.append(" | costo registrado sin IVA: ").append(String.format(java.util.Locale.US, "$%.2f", p.costoSinIva()))
                        .append(" | diferencia bruta: ").append(String.format(java.util.Locale.US, "$%.2f", p.precioVenta().subtract(p.costoSinIva())));
                if (p.margenPct() >= 0) sb.append(" | margen sobre precio: ").append(String.format(java.util.Locale.US, "%.1f%%", p.margenPct()));
            }
            if (p.proveedor() != null && !p.proveedor().isBlank()) sb.append(" | proveedor: ").append(p.proveedor());
        }
        return sb.toString();
    }

    private String formatearProductos(java.util.List<HerramientasAgente.Producto> ps, String criterio) {
        if (ps == null || ps.isEmpty()) {
            return "No encontré productos para \"" + (criterio != null ? criterio.trim() : "") + "\" en inventario. Prueba con código o descripción más simple.";
        }
        StringBuilder sb = new StringBuilder("Productos encontrados:\n");
        for (HerramientasAgente.Producto p : ps) {
            sb.append("• ").append(p.codigo() != null ? p.codigo() : "sin código")
              .append(" - ").append(p.descripcion())
              .append(" | stock: ").append(p.stock())
              .append(" | precio: ").append(p.precioVenta() != null ? String.format(java.util.Locale.US,"$%.2f", p.precioVenta()) : "n/d");
            if (p.costoSinIva() != null && p.precioVenta() != null) {
                BigDecimal diferencia = p.precioVenta().subtract(p.costoSinIva());
                sb.append(" | costo registrado sin IVA: ").append(String.format(java.util.Locale.US, "$%.2f", p.costoSinIva()))
                        .append(" | diferencia bruta precio-costo: ").append(String.format(java.util.Locale.US, "$%.2f", diferencia));
                if (p.margenPct() >= 0) sb.append(" (margen sobre precio: ").append(String.format(java.util.Locale.US,"%.1f%%", p.margenPct())).append(')');
            } else {
                sb.append(" | costo no registrado; no puedo explicar la diferencia precio-costo");
            }
            if (p.proveedor() != null && !p.proveedor().isBlank()) sb.append(" | proveedor: ").append(p.proveedor());
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private String formatearVentas(Clasificacion c) {
        HerramientasAgente.Cliente cli = resolverClienteUnico(c);
        if (cli == null) return ultimoMensajeCliente;
        HerramientasAgente.Ventas v = herramientas.ventas(cli.id());
        if (v == null || v.facturas() == 0) {
            return "El cliente \"" + cli.nombre() + "\" no tiene facturas registradas.";
        }
        return String.format(java.util.Locale.US,"%s (CI/RUC %s): %d factura(s) | total $%.2f | última compra: %s",
                cli.nombre(), cli.identificacion() != null ? cli.identificacion() : "-",
                v.facturas(), v.total(), v.ultimaFecha() != null ? v.ultimaFecha() : "s/f");
    }

    private String formatearSaldo(Clasificacion c) {
        HerramientasAgente.Cliente cli = resolverClienteUnico(c);
        if (cli == null) return ultimoMensajeCliente;
        BigDecimal saldo = herramientas.saldo(cli.id());
        if (saldo == null) return "No pude calcular el saldo de \"" + cli.nombre() + "\".";
        if (saldo.signum() <= 0) return cli.nombre() + ": sin saldo pendiente (al día).";
        return String.format(java.util.Locale.US,"%s (CI/RUC %s): debe $%.2f (cuentas por cobrar pendientes)",
                cli.nombre(), cli.identificacion() != null ? cli.identificacion() : "-", saldo);
    }

    private String formatearRentabilidad() {
        StringBuilder sb = new StringBuilder();
        java.util.List<HerramientasAgente.Top> topM = herramientas.mejorMargen(5);
        if (!topM.isEmpty()) {
            sb.append("Mejor margen ((precio-costo)/precio):\n");
            for (HerramientasAgente.Top t : topM) {
                sb.append("• ").append(t.codigo()).append(" - ").append(t.descripcion())
                  .append(" | margen: ").append(String.format(java.util.Locale.US,"%.1f%%", t.margenPct()))
                  .append(" | precio: ").append(String.format(java.util.Locale.US,"$%.2f", t.monto())).append('\n');
            }
        }
        java.util.List<HerramientasAgente.Top> topV = herramientas.masVendidos(5);
        if (!topV.isEmpty()) {
            sb.append("Más vendidos (por monto facturado):\n");
            for (HerramientasAgente.Top t : topV) {
                sb.append("• ").append(t.codigo()).append(" - ").append(t.descripcion())
                  .append(" | ").append(t.cantidad()).append(" uds")
                  .append(" | $").append(String.format(java.util.Locale.US,"%.2f", t.monto())).append('\n');
            }
        }
        return sb.isEmpty() ? "Aún no hay datos suficientes de ventas o costos para calcular rentabilidad."
                            : sb.toString().trim();
    }

    /** Resuelve el cliente único para las acciones de cliente; null si hay 0 o varios (mensaje en ultimoMensajeCliente). */
    private HerramientasAgente.Cliente resolverClienteUnico(Clasificacion c) {
        ultimoMensajeCliente = "";
        String criterio = c.cliente != null && !c.cliente.isBlank() ? c.cliente
                : (c.descripcion != null && !c.descripcion.isBlank() ? c.descripcion : "");
        if (criterio.isBlank()) {
            ultimoMensajeCliente = "¿De qué cliente hablas? Menciona su nombre o RUC.";
            return null;
        }
        java.util.List<HerramientasAgente.Cliente> cands = herramientas.clientes(criterio, 5);
        if (cands.isEmpty()) {
            ultimoMensajeCliente = "No encontré al cliente \"" + criterio + "\" en el ERP.";
            return null;
        }
        if (cands.size() > 1) {
            StringBuilder sb = new StringBuilder("Encontré " + cands.size() + " clientes, ¿a cuál te refieres?\n");
            for (HerramientasAgente.Cliente cl : cands) {
                sb.append("• ").append(cl.nombre())
                  .append(cl.identificacion() != null ? " (CI/RUC " + cl.identificacion() + ")" : "").append('\n');
            }
            ultimoMensajeCliente = sb.toString().trim();
            return null;
        }
        return cands.get(0);
    }

    /** Mensaje del último resolverClienteUnico cuando no hubo match único. */
    private String ultimoMensajeCliente = "";

    private String formatearRespuesta(ResultadoBusqueda r, String consulta) {
        if (r == null || !r.isEncontrado()) {
            return "No encontré repuestos para \"" + consulta.trim() + "\" en inventario. Prueba con código o descripción más simple ej: 'filtro aceite'.";
        }
        String precioStr = r.getPrecio() != null ? String.format(java.util.Locale.US,"$%.2f", r.getPrecio()) : "precio no registrado";
        String stockStr = r.getStock() > 0 ? r.getStock() + " unidades" : "sin stock";
        return String.format(java.util.Locale.US,"Encontrado: %s - %s | Stock: %s | Precio: %s",
                r.getCodigo() != null ? r.getCodigo() : "sin código", r.getNombre(), stockStr, precioStr);
    }

    private static class GeminiException extends Exception {
        final EstadoIA estado;
        final int codigoHttp;
        GeminiException(EstadoIA estado, String msg) { this(estado, 0, msg); }
        GeminiException(EstadoIA estado, int codigoHttp, String msg) {
            super(msg);
            this.estado = estado;
            this.codigoHttp = codigoHttp;
        }
        String mensajeUsuario() {
            return switch (estado) {
                case SIN_IA_TOKENS -> getMessage();
                case SIN_IA_SERVIDOR -> "Servidor Gemini temporalmente no disponible, intenta en minutos";
                case SIN_IA_CONEXION -> "Sin conexión a internet o timeout";
                case SIN_IA_KEY_INVALIDA -> "API key inválida o sin permisos";
                default -> getMessage();
            };
        }
    }

    /** Protected para tests con proveedor IA falso (sin red). */
    protected String llamarGemini(String prompt, String apiKey) throws Exception {
        GeminiException ultimoFallo = null;
        long proximoIntento = Long.MAX_VALUE;
        for (String modelo : java.util.List.of(GEMINI_MODEL_PRIMARY, GEMINI_MODEL_FALLBACK, GEMINI_MODEL_FALLBACK2)) {
            long ahora = reloj.millis();
            Long hasta = modelosEnPausa.get(modelo);
            if (hasta != null && hasta > ahora) {
                proximoIntento = Math.min(proximoIntento, hasta);
                continue;
            }
            try {
                String respuesta = llamarGeminiConModelo(prompt, apiKey, modelo);
                modelosEnPausa.remove(modelo);
                return respuesta;
            } catch (GeminiException e) {
                // La decisión depende del estado HTTP, no de palabras/números en el cuerpo del error.
                boolean transitorio = e.codigoHttp == 500 || e.codigoHttp == 502
                        || e.codigoHttp == 503 || e.codigoHttp == 504;
                if (e.codigoHttp != 404 && !transitorio) throw e;
                Duration pausa = e.codigoHttp == 404 ? PAUSA_MODELO_NO_ENCONTRADO : PAUSA_SERVIDOR;
                modelosEnPausa.put(modelo, reloj.millis() + pausa.toMillis());
                LOGGER.log(Level.WARNING, "Gemini {0}: HTTP {1}; modelo en pausa {2}s. Se intenta otro modelo disponible.",
                        new Object[]{modelo, e.codigoHttp, pausa.toSeconds()});
                ultimoFallo = e;
            }
        }
        if (ultimoFallo != null) throw ultimoFallo;
        long segundos = Math.max(1, (proximoIntento - reloj.millis() + 999) / 1000);
        throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR,
                "Modelos Gemini temporalmente en pausa tras errores del servidor; próximo intento en " + segundos + "s. Búsqueda local disponible.");
    }

    private String llamarGeminiConModelo(String prompt, String apiKey, String modelo) throws Exception {
        String url = modelosUri + modelo + ":generateContent?key=" + apiKey;
        com.fasterxml.jackson.databind.node.ObjectNode root = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode contents = root.putArray("contents");
        com.fasterxml.jackson.databind.node.ObjectNode content = contents.addObject();
        content.put("role", "user");
        com.fasterxml.jackson.databind.node.ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);
        HttpResponse<String> resp;
        try {
            var resultado = new com.vendex.service.GeminiCuotas(com.vendex.service.GeminiCuotas.dao())
                    .enviar(httpClient, URI.create(url), root, TIMEOUT, modelo,
                            prompt.contains("\"accion\"") ? "chat.clasificacion" : "chat.redaccion");
            resp = resultado.http();
            modoAhorro = resultado.ahorro();
        } catch (com.vendex.service.GeminiCuotas.Limite e) {
            throw new GeminiException(EstadoIA.SIN_IA_TOKENS, e.getMessage());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new GeminiException(EstadoIA.SIN_IA_CONEXION, "Timeout 45s: " + e.getMessage());
        } catch (java.io.IOException e) {
            throw new GeminiException(EstadoIA.SIN_IA_CONEXION, "Sin conexión: " + e.getMessage());
        }

        int code = resp.statusCode();
        String b = resp.body();
        if (code == 404) throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, code, "HTTP 404 modelo " + modelo + " no encontrado: " + b);
        if (code == 429) throw new GeminiException(EstadoIA.SIN_IA_TOKENS, code, "429 quota: " + b);
        if (code == 401 || code == 403) throw new GeminiException(EstadoIA.SIN_IA_KEY_INVALIDA, code, "HTTP " + code + ": " + b);
        if (code == 400) {
            String lower = b.toLowerCase();
            if (lower.contains("api_key") || lower.contains("api key") || lower.contains("permission_denied") || lower.contains("api_key_invalid")) {
                throw new GeminiException(EstadoIA.SIN_IA_KEY_INVALIDA, code, "HTTP 400 key: " + b);
            }
            throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, code, "HTTP 400 payload: " + b);
        }
        if (code != 200) throw new GeminiException(EstadoIA.SIN_IA_SERVIDOR, code, "HTTP " + code + ": " + b);

        JsonNode respRoot = MAPPER.readTree(b);
        // Fase 3: medición de tokens (best-effort; spec §10.1)
        try {
            JsonNode usage = respRoot.path("usageMetadata");
            if (!usage.isMissingNode()) {
                long tin = usage.path("promptTokenCount").asLong(0);
                long tout = com.vendex.service.GeminiCuotas.salidaContabilizada(usage);
                consumoService().registrarTokens("chatbot", "chat", modelo, tin, tout);
            }
        } catch (Exception ignored) {}
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
