package com.vendex.offline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LocalOperationQueue {

    private static final Path DIRECTORIO = Path.of(System.getProperty("user.home"), ".vendex", "offline_queue");
    private static final Path ARCHIVO = DIRECTORIO.resolve("operaciones.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AtomicInteger SECUENCIAL_ID = new AtomicInteger(calcularMaxId());

    private static int calcularMaxId() {
        try {
            List<OperacionOffline> todas = leerTodas();
            return todas.stream()
                    .mapToInt(op -> op.getId())
                    .max()
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    public synchronized void encolar(OperacionOffline op) {
        try {
            List<OperacionOffline> todas = leerTodas();
            if (op.getId() == 0) {
                op.setId(SECUENCIAL_ID.incrementAndGet());
            }
            todas.add(op);
            guardar(todas);
        } catch (IOException e) {
            throw new RuntimeException("Error encolando operación offline", e);
        }
    }

    public synchronized List<OperacionOffline> listarPendientes() {
        try {
            List<OperacionOffline> todas = leerTodas();
            return todas.stream()
                    .filter(op -> op.getEstado() == OperacionOffline.Estado.PENDIENTE)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    public synchronized OperacionOffline obtenerSiguiente() {
        List<OperacionOffline> pendientes = listarPendientes();
        return pendientes.isEmpty() ? null : pendientes.get(0);
    }

    public synchronized void marcarEnviada(int id) {
        actualizarEstado(id, OperacionOffline.Estado.ENVIADA, null);
    }

    public synchronized void marcarConflicto(int id, String error) {
        actualizarEstado(id, OperacionOffline.Estado.CONFLICTO, error);
    }

    public synchronized void marcarFallida(int id, String error) {
        actualizarEstado(id, OperacionOffline.Estado.FALLIDA, error);
    }

    public synchronized int contarPendientes() {
        return listarPendientes().size();
    }

    public synchronized void limpiarEnviadas() {
        try {
            List<OperacionOffline> todas = leerTodas();
            List<OperacionOffline> filtradas = todas.stream()
                    .filter(op -> op.getEstado() != OperacionOffline.Estado.ENVIADA)
                    .collect(Collectors.toList());
            guardar(filtradas);
        } catch (IOException e) {
            // ignorar
        }
    }

    private static List<OperacionOffline> leerTodas() throws IOException {
        if (!Files.exists(ARCHIVO)) {
            return new ArrayList<>();
        }
        String json = Files.readString(ARCHIVO);
        if (json.isBlank()) {
            return new ArrayList<>();
        }
        return MAPPER.readValue(json, new TypeReference<List<OperacionOffline>>() {});
    }

    private static void guardar(List<OperacionOffline> operaciones) throws IOException {
        Files.createDirectories(DIRECTORIO);
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(operaciones);
        Files.writeString(ARCHIVO, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void actualizarEstado(int id, OperacionOffline.Estado estado, String error) {
        try {
            List<OperacionOffline> todas = leerTodas();
            for (OperacionOffline op : todas) {
                if (op.getId() == id) {
                    op.setEstado(estado);
                    op.setError(error);
                    op.setUltimoIntento(java.time.LocalDateTime.now());
                    op.setIntentos(op.getIntentos() + 1);
                    break;
                }
            }
            guardar(todas);
        } catch (IOException e) {
            throw new RuntimeException("Error actualizando operación offline", e);
        }
    }
}
