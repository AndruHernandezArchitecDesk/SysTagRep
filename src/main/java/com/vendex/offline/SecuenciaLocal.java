package com.vendex.offline;

import com.vendex.dao.SecuenciaBloqueDAO;
import com.vendex.model.SecuenciaBloque;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecuenciaLocal {

    private final SecuenciaBloqueDAO bloqueDAO;
    private final Map<String, Deque<SecuenciaBloque>> bloquesPorTipo = new ConcurrentHashMap<>();
    private final int tamanoBloque;

    public SecuenciaLocal(SecuenciaBloqueDAO bloqueDAO, int tamanoBloque) {
        this.bloqueDAO = bloqueDAO;
        this.tamanoBloque = tamanoBloque;
    }

    public synchronized int siguiente(String tipo) {
        Deque<SecuenciaBloque> cola = bloquesPorTipo.computeIfAbsent(tipo, k -> new ArrayDeque<>());
        if (cola.isEmpty() || cola.peek().estaAgotado()) {
            solicitarBloque(tipo);
        }
        SecuenciaBloque bloque = cola.peek();
        int sec = bloque.getNumeroInicio() + bloque.getUsadoHasta() - 1;
        bloque.setUsadoHasta(bloque.getUsadoHasta() + 1);
        return sec;
    }

    private void solicitarBloque(String tipo) {
        SecuenciaBloque bloque = bloqueDAO.reservarBloque(
                com.vendex.util.SesionActual.getPuntoEmisionId(),
                tipo,
                tamanoBloque
        );
        if (bloque == null) {
            throw new IllegalStateException("No se pudo reservar bloque de secuenciales para " + tipo);
        }
        Deque<SecuenciaBloque> cola = bloquesPorTipo.computeIfAbsent(tipo, k -> new ArrayDeque<>());
        cola.offer(bloque);
    }
}
