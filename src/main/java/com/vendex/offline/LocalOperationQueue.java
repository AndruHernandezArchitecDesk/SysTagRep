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

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
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

    private static Path directorio = Path.of(System.getProperty("user.home"), ".vendex", "offline_queue");
    private static Path archivo = directorio.resolve("operaciones.json");

    /**
     * Fase 7: lock global por archivo estático — varios controllers instancian esta clase
     * y el read-modify-write del JSON debe ser atómico entre todas las instancias.
     */
    private static final Object LOCK = new Object();

    public static void setDirectorioPrueba(Path nuevoDirectorio) {
        synchronized (LOCK) {
            directorio = nuevoDirectorio;
            archivo = directorio.resolve("operaciones.json");
        }
    }

    public void encolar(OperacionOffline op) {
        synchronized (LOCK) {
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
    }

    public List<OperacionOffline> listarPendientes() {
        synchronized (LOCK) {
            try {
                List<OperacionOffline> todas = leerTodas();
                return todas.stream()
                        .filter(op -> op.getEstado() == OperacionOffline.Estado.PENDIENTE)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                return List.of();
            }
        }
    }

    /** Fase 7: todas las operaciones de la cola (para UI de resolución de conflictos). */
    public List<OperacionOffline> listarTodas() {
        synchronized (LOCK) {
            try {
                return leerTodas();
            } catch (IOException e) {
                return List.of();
            }
        }
    }

    /**
     * Fase 7: reintento sin estado terminal — solo incrementa intentos y registra el error,
     * la operación sigue PENDIENTE para el siguiente ciclo (hasta MAX_INTENTOS).
     */
    public void registrarIntento(int id, String error) {
        synchronized (LOCK) {
            try {
                List<OperacionOffline> todas = leerTodas();
                for (OperacionOffline op : todas) {
                    if (op.getId() == id) {
                        op.setError(error);
                        op.setUltimoIntento(java.time.LocalDateTime.now());
                        op.setIntentos(op.getIntentos() + 1);
                        break;
                    }
                }
                guardar(todas);
            } catch (IOException e) {
                throw new RuntimeException("Error registrando intento offline", e);
            }
        }
    }

    /** Fase 7: resolución de conflictos — CONFLICTO/FALLIDA vuelven a PENDIENTE. */
    public boolean reencolar(int id) {
        synchronized (LOCK) {
            try {
                List<OperacionOffline> todas = leerTodas();
                boolean cambio = false;
                for (OperacionOffline op : todas) {
                    if (op.getId() == id && op.getEstado() != OperacionOffline.Estado.PENDIENTE
                            && op.getEstado() != OperacionOffline.Estado.ENVIADA) {
                        op.setEstado(OperacionOffline.Estado.PENDIENTE);
                        op.setIntentos(0);
                        op.setError(null);
                        op.setUltimoIntento(java.time.LocalDateTime.now());
                        cambio = true;
                        break;
                    }
                }
                if (cambio) guardar(todas);
                return cambio;
            } catch (IOException e) {
                throw new RuntimeException("Error reencolando operación offline", e);
            }
        }
    }

    /** Fase 7: resolución de conflictos — descartar una operación (no vuelve a enviarse). */
    public boolean descartar(int id) {
        synchronized (LOCK) {
            try {
                List<OperacionOffline> todas = leerTodas();
                int antes = todas.size();
                todas.removeIf(op -> op.getId() == id);
                if (todas.size() != antes) {
                    guardar(todas);
                    return true;
                }
                return false;
            } catch (IOException e) {
                throw new RuntimeException("Error descartando operación offline", e);
            }
        }
    }

    public OperacionOffline obtenerSiguiente() {
        List<OperacionOffline> pendientes = listarPendientes();
        return pendientes.isEmpty() ? null : pendientes.get(0);
    }

    public void marcarEnviada(int id) {
        actualizarEstado(id, OperacionOffline.Estado.ENVIADA, null);
    }

    public void marcarConflicto(int id, String error) {
        actualizarEstado(id, OperacionOffline.Estado.CONFLICTO, error);
    }

    public void marcarFallida(int id, String error) {
        actualizarEstado(id, OperacionOffline.Estado.FALLIDA, error);
    }

    public int contarPendientes() {
        return listarPendientes().size();
    }

    public void limpiarEnviadas() {
        synchronized (LOCK) {
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
    }

    private static List<OperacionOffline> leerTodas() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }
        String json = Files.readString(archivo);
        if (json.isBlank()) {
            return new ArrayList<>();
        }
        return MAPPER.readValue(json, new TypeReference<List<OperacionOffline>>() {});
    }

    private static void guardar(List<OperacionOffline> operaciones) throws IOException {
        Files.createDirectories(directorio);
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(operaciones);
        Files.writeString(archivo, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void actualizarEstado(int id, OperacionOffline.Estado estado, String error) {
        synchronized (LOCK) {
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
}
