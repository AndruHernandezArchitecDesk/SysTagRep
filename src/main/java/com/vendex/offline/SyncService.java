package com.vendex.offline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SyncService {

    public static final SyncService INSTANCE = new SyncService();

    private static final long INTERVALO_SEGUNDOS = 30;
    private static final int MAX_INTENTOS = 5;

    private final LocalOperationQueue cola = new LocalOperationQueue();
    private final com.vendex.remote.RestClient client;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean sincronizando = new AtomicBoolean(false);
    private int enviadas = 0;
    private int conflictos = 0;
    private int fallidas = 0;
    private String ultimoError = null;

    private SyncService() {
        this(new com.vendex.remote.RestClient(com.vendex.remote.ApiConfig.baseUrl()));
    }

    private SyncService(com.vendex.remote.RestClient client) {
        this.client = client;
    }

    static SyncService paraTesting(com.vendex.remote.RestClient client) {
        return new SyncService(client);
    }

    public synchronized void iniciar() {
        scheduler.scheduleAtFixedRate(this::intentarSincronizacion, 0, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
    }

    public synchronized void detener() {
        scheduler.shutdownNow();
    }

    public int getPendientes() { return cola.listarPendientes().size(); }
    public int getEnviadas() { return enviadas; }
    public int getConflictos() { return conflictos; }
    public int getFallidas() { return fallidas; }

    public void intentarSincronizacion() {
        if (sincronizando.get()) return;
        if (!OfflineModeManager.INSTANCE.isOnline()) return;

        List<OperacionOffline> pendientes = cola.listarPendientes();
        if (pendientes.isEmpty()) {
            OfflineModeManager.INSTANCE.setModo(ModoOperacion.ONLINE);
            return;
        }

        sincronizando.set(true);
        OfflineModeManager.INSTANCE.setModo(ModoOperacion.SYNCING);

        enviadas = 0;
        conflictos = 0;
        fallidas = 0;

        for (OperacionOffline op : pendientes) {
            if (op.getIntentos() >= MAX_INTENTOS) {
                cola.marcarFallida(op.getId(), "Maximo de intentos alcanzado");
                fallidas++;
                continue;
            }
            try {
                boolean exito = enviarOperacion(op);
                if (exito) {
                    cola.marcarEnviada(op.getId());
                    enviadas++;
                } else {
                    // 4xx: error de negocio — estado terminal CONFLICTO, resuelto desde la cola
                    String error = ultimoError;
                    cola.marcarConflicto(op.getId(), error != null ? error : "Error de negocio en servidor");
                    conflictos++;
                }
            } catch (Exception e) {
                // Fallo de conexión o 5xx: sigue PENDIENTE y se reintenta en el próximo ciclo
                cola.registrarIntento(op.getId(), e.getMessage());
                if (op.getIntentos() + 1 >= MAX_INTENTOS) {
                    cola.marcarFallida(op.getId(), "Maximo de intentos alcanzado: " + e.getMessage());
                }
                fallidas++;
            }
        }

        List<OperacionOffline> restantes = cola.listarPendientes();
        if (restantes.isEmpty()) {
            OfflineModeManager.INSTANCE.setModo(ModoOperacion.ONLINE);
        }
        sincronizando.set(false);
    }

    private boolean enviarOperacion(OperacionOffline op) throws IOException, InterruptedException {
        String endpoint = resolverEndpoint(op.getTipo());
        if (endpoint == null) {
            throw new IOException("Tipo de operacion no soportado: " + op.getTipo());
        }
        // Clave estable entre reintentos: el server la usa para no duplicar (Idempotency-Key)
        String idempotencyKey = op.getTipo() + "-" + op.getId() + "-" + op.getCreadoEn();
        java.net.http.HttpResponse<String> resp = client.postStringRespuesta(endpoint, op.getPayload(), idempotencyKey);
        int code = resp.statusCode();
        String responseBody = resp.body();
        if (code >= 200 && code < 300) {
            return true;
        }
        if (code >= 500) {
            // Servidor/caída transitoria → excepción para reintentar (sigue PENDIENTE)
            throw new IOException("HTTP " + code + " en " + endpoint);
        }
        // 4xx: error de negocio (payload inválido, duplicado, etc.)
        ultimoError = "HTTP " + code + ": " + (responseBody != null && responseBody.length() > 500
                ? responseBody.substring(0, 500) : responseBody);
        return false;
    }

    private String leerRespuestaError(OperacionOffline op) {
        // No reenviar: para endpoints no idempotentes (emitir factura) repostear
        // crearia un segundo comprobante con nuevo secuencial.
        return ultimoError != null ? ultimoError : "Error en servidor";
    }

    private String resolverEndpoint(String tipo) {
        return switch (tipo) {
            // FACTURA por el camino real (secuencial atomico + stock + comprobante + SRI),
            // nunca por insercion directa: el payload offline ya trae clienteId, formaPago,
            // ambienteSri, descuentoValor e items, igual que el guardado remoto online.
            case "FACTURA" -> "/facturas";
            case "NOTA_CREDITO" -> "/nota-credito";
            case "NOTA_VENTA" -> "/nota-venta";
            case "NOTA_DEBITO" -> "/nota-debito";
            case "GUIA_REMISION" -> "/guia-remision";
            case "RETENCION" -> "/retencion";
            case "INVENTARIO" -> "/inventario";
            case "INVENTARIO_ELIMINAR" -> "/inventario/eliminar";
            case "INGRESO_MERCADERIA" -> "/ingreso-mercaderia";
            case "CAJA_ABRIR" -> "/caja/abrir";
            case "CAJA_CERRAR" -> "/caja/cerrar";
            case "CUENTA_POR_COBRAR" -> "/cuentas-por-cobrar/adelanto";
            case "CUENTA_POR_PAGAR" -> "/cuentas-por-pagar/adelanto";
            default -> null;
        };
    }
}
