package com.vendex.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.config.GeminiConfig;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private volatile EstadoIA estado = EstadoIA.SIN_API_KEY;
    private volatile String ultimoError = "";
    private volatile com.vendex.service.VendexiConsumoService consumo;

    public GeminiChatbotService(RepuestoDao repuestoDao) {
        this(repuestoDao, null);
    }

    /** Agente IA: herramientas de lectura del ERP (null = solo búsqueda de repuestos). */
    public GeminiChatbotService(RepuestoDao repuestoDao, HerramientasAgente herramientas) {
        this.repuestoDao = repuestoDao;
        this.herramientas = herramientas;
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
        String r = preguntar(textoUsuario, vehMarca, vehModelo, vehAnio);
        boolean sinIA = estado != EstadoIA.IA_ACTIVA;
        // si no había key, sinIA=true; si había key pero falló, también sinIA
        // si key existe y no falló, es IA activa
        return new RespuestaChat(r, estado, sinIA && !textoUsuario.isBlank());
    }

    public String preguntar(String textoUsuario) {
        return preguntar(textoUsuario, null, null, null);
    }

    public String preguntar(String textoUsuario, String vehMarca, String vehModelo, Integer vehAnio) {
        if (textoUsuario == null || textoUsuario.isBlank()) {
            return "Escribe el nombre del repuesto que buscas, ej: 'tapa radiador aveo'.";
        }
        // Fase 1: gate de licencia — si la licencia no incluye Vendexi AI, no se llama al proveedor.
        // La búsqueda local (función ERP permitida) sigue disponible.
        if (!com.vendex.util.LicenseManager.licenciaIncluyeIA()) {
            estado = EstadoIA.SIN_API_KEY;
            ultimoError = "La licencia de esta instalación no incluye Vendexi AI";
            return "ℹ️ La licencia de esta instalación no incluye Vendexi AI.\n" +
                   "Búsqueda local en inventario:\n" + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio);
        }
        // Fase 3: gate de capacidad asignada (spec §10.2) — el ERP local sigue disponible.
        if (!consumoService().hayCapacidadDisponible()) {
            estado = EstadoIA.SIN_IA_TOKENS;
            ultimoError = com.vendex.service.VendexiConsumoService.mensajeCapacidadAgotada();
            return "⚠️ capacidad de IA agotada\n" +
                   com.vendex.service.VendexiConsumoService.mensajeCapacidadAgotada() + "\n\n" +
                   "Búsqueda local en inventario:\n" + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio);
        }
        String apiKey = GeminiConfig.obtenerApiKey();
        boolean tieneGemini = apiKey != null && !apiKey.isBlank();
        if (!tieneGemini) {
            estado = EstadoIA.SIN_API_KEY;
            ultimoError = "Sin API key: configura en Ayuda > Configurar IA o env GEMINI_API_KEY";
            return prefijoSinIA(EstadoIA.SIN_API_KEY) + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio);
        }
        try {
            String r = preguntarConGemini(textoUsuario, apiKey, vehMarca, vehModelo, vehAnio);
            estado = EstadoIA.IA_ACTIVA;
            ultimoError = "";
            return r;
        } catch (GeminiException ge) {
            estado = ge.estado;
            ultimoError = ge.getMessage();
            LOGGER.log(Level.WARNING, "Gemini falló (" + ge.estado + "): " + ge.getMessage());
            String local = preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio);
            return prefijoSinIA(ge.estado) + local + "\n\n(" + ge.mensajeUsuario() + ")";
        } catch (Exception e) {
            estado = EstadoIA.SIN_IA_CONEXION;
            ultimoError = e.getMessage();
            LOGGER.log(Level.WARNING, "Error chatbot fallback local", e);
            return prefijoSinIA(EstadoIA.SIN_IA_CONEXION) + preguntarSoloLocal(textoUsuario, vehMarca, vehModelo, vehAnio);
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

    private String preguntarSoloLocal(String texto, String vehMarca, String vehModelo, Integer vehAnio) {
        ResultadoBusqueda r = repuestoDao.buscar(texto.trim(), vehMarca, vehModelo, vehAnio);
        if (!r.isEncontrado() && (vehMarca != null || vehModelo != null || vehAnio != null)) {
            r = repuestoDao.buscar(texto.trim(), null, null, null);
        }
        return formatearRespuesta(r, texto);
    }

    String preguntarConGemini(String texto, String apiKey, String vehMarca, String vehModelo, Integer vehAnio) throws Exception {
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
        boolean rolOk = rolesPermitidos(roles);

        // Paso 2: ejecutar la herramienta local correspondiente (datos reales del ERP)
        String respuestaLocal = ejecutarLocal(c, texto);
        if (!rolOk) {
            return respuestaLocal; // mensaje de rol no incluido, sin llamada de redacción
        }

        // Paso 3: redacción breve con IA (best-effort, no invalida los datos locales)
        try {
            String promptRespuesta = promptRedaccion(c, texto, respuestaLocal);
            String redaccion = llamarGemini(promptRespuesta, apiKey);
            if (redaccion != null && !redaccion.isBlank() && redaccion.length() < 700) {
                return redaccion.trim();
            }
        } catch (GeminiException ge) {
            LOGGER.log(Level.FINE, "Redacción Gemini falló (se usa respuesta local): " + ge.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Fallo redacción Gemini", e);
        }
        if (respuestaLocal.isBlank()) {
            // GENERAL sin redacción posible: ofrecer las capacidades reales del agente
            return "Puedo ayudarte con repuestos y síntomas, stock y precios, ventas de un cliente, " +
                   "saldos por cobrar y rentabilidad. Escribe tu consulta.";
        }
        return respuestaLocal;
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
                marca, modelo, anio, motivo (si es sintoma), esSintoma.
                Responde SOLO JSON valido sin markdown:
                {"accion":"BUSCAR_REPUESTO","descripcion":"pastilla freno","cliente":null,"marca":"CHEV","modelo":"AVEO","anio":null,"motivo":null,"esSintoma":false}
                Ejemplo venta cliente: {"accion":"VENTAS_CLIENTE","descripcion":null,"cliente":"Juan Perez",...}
                Ejemplo stock: {"accion":"CONSULTAR_PRODUCTO","descripcion":"tapa radiador",...}
                Ejemplo sintoma: {"accion":"BUSCAR_REPUESTO","descripcion":"pastilla freno","motivo":"Ruido al frenar suele indicar desgaste","esSintoma":true,...}
                Mensaje usuario: "%s"
                """.formatted(texto.replace("\"", "'").replace("\n", " "));
    }

    /** Personalidad del rol IA contextual según la acción clasificada (§4). */
    String rolContextual(Accion a) {
        if (a == null) a = Accion.GENERAL;
        return switch (a) {
            case VENTAS_CLIENTE -> "Actúas como asesor comercial de Vendex (rol VENTAS): céntrate en la relación con el cliente, facturas y montos.";
            case CUENTAS_COBRAR, RENTABILIDAD -> "Actúas como analista financiero de Vendex (rol FINANCIERO): céntrate en cifras, saldos y márgenes, con tono preciso.";
            case CONSULTAR_PRODUCTO, BUSCAR_REPUESTO -> "Actúas como especialista en inventario de repuestos (rol INVENTARIO): céntrate en disponibilidad, compatibilidad y precios.";
            default -> "Actúas como asistente general de Vendex: sé amable y breve.";
        };
    }

    String promptRedaccion(Clasificacion c, String texto, String datosLocales) {
        String regla = switch (c.accion) {
            case VENTAS_CLIENTE, CUENTAS_COBRAR, RENTABILIDAD, CONSULTAR_PRODUCTO ->
                    "Si hay datos, resume los montos/stock relevantes. Si no hay, di que no se encontraron. No inventes montos, stock ni precios.";
            case GENERAL -> "Responde amable y breve en español (max 3 lineas). Puedes ofrecer ayudar con repuestos, stock, ventas, saldos y rentabilidad.";
            default -> "Si hay stock indica código, nombre, stock y precio. Si no hay, di que no hay stock pero mantén la sugerencia. No inventes stock/precios. " +
                    "Si los datos incluyen complementos, menciona hasta dos con su código; son asociaciones locales, no compatibilidad técnica confirmada.";
        };
        String motivo = c.motivo != null ? c.motivo : (c.esSintoma ? "sintoma" : "consulta directa");
        return """
                Eres asistente Vendex. %s Responde breve en español (max 3 lineas).
                Consulta: "%s"
                Motivo/sugerencia IA: %s
                Datos reales del ERP: %s
                %s
                """.formatted(rolContextual(c.accion), texto.replace("\"", "'"), motivo,
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
                        ? formatearProductos(herramientas.productos(c.descripcion, 5), c.descripcion)
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

    /** Flujo clásico (y fallback): búsqueda de repuestos con cadena de reintentos. */
    private String buscarRepuesto(Clasificacion c, String texto) {
        String descripcion = c.descripcion != null && !c.descripcion.isBlank() ? c.descripcion : texto;
        String descripcionBusqueda = descripcion;
        if (c.marca != null) descripcionBusqueda += " " + c.marca;
        if (c.modelo != null) descripcionBusqueda += " " + c.modelo;
        if (c.anio != null) descripcionBusqueda += " " + c.anio;

        ResultadoBusqueda r = repuestoDao.buscar(descripcionBusqueda.trim(), c.marca, c.modelo, c.anio);
        if (!r.isEncontrado() && !descripcionBusqueda.equals(descripcion)) r = repuestoDao.buscar(descripcion.trim(), c.marca, c.modelo, c.anio);
        if (!r.isEncontrado() && !descripcion.equals(texto)) r = repuestoDao.buscar(texto.trim(), null, null, null);

        String respuestaLocal = formatearRespuesta(r, descripcionBusqueda);
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
            if (p.margenPct() >= 0) sb.append(" | margen: ").append(String.format(java.util.Locale.US,"%.1f%%", p.margenPct()));
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

    /** Protected para tests con proveedor IA falso (sin red). */
    protected String llamarGemini(String prompt, String apiKey) throws Exception {
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
            throw new GeminiException(EstadoIA.SIN_IA_CONEXION, "Timeout 45s: " + e.getMessage());
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
        // Fase 3: medición de tokens (best-effort; spec §10.1)
        try {
            JsonNode usage = respRoot.path("usageMetadata");
            long tin = usage.path("promptTokenCount").asLong(0);
            long tout = usage.path("candidatesTokenCount").asLong(0);
            consumoService().registrarTokens("chatbot", "chat", modelo, tin, tout);
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
