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
                    String error = leerRespuestaError(op);
                    cola.marcarConflicto(op.getId(), error != null ? error : "Error en servidor");
                    conflictos++;
                }
            } catch (Exception e) {
                cola.marcarConflicto(op.getId(), e.getMessage());
                conflictos++;
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
        String idempotencyKey = op.getTipo() + "-" + op.getId() + "-" + op.getCreadoEn();
        String responseBody = client.postString(endpoint, op.getPayload(), idempotencyKey);
        if (responseBody == null || responseBody.isBlank()) {
            return true;
        }
        return !responseBody.toLowerCase().contains("error");
    }

    private String leerRespuestaError(OperacionOffline op) {
        String endpoint = resolverEndpoint(op.getTipo());
        if (endpoint == null) return "Endpoint no soportado";
        try {
            String idempotencyKey = op.getTipo() + "-" + op.getId() + "-" + op.getCreadoEn();
            String body = client.postString(endpoint, op.getPayload(), idempotencyKey);
            if (body == null || body.isBlank()) return "HTTP 200";
            return body.length() > 200 ? body.substring(0, 200) : body;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String resolverEndpoint(String tipo) {
        return switch (tipo) {
            case "FACTURA" -> "/facturas/registro";
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
