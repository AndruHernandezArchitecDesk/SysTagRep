package com.vendex.offline;

import com.vendex.remote.ApiConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class OfflineModeManager {

    public static final OfflineModeManager INSTANCE = new OfflineModeManager();

    private static final int MAX_FALLOS = 3;
    private static final long MONITOREO_SEGUNDOS = 15;

    private final AtomicReference<ModoOperacion> modo = new AtomicReference<>(ModoOperacion.ONLINE);
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
    private int fallosConsecutivos = 0;
    private volatile boolean monitoreando = false;
    private final com.vendex.remote.RestClient healthClient = new com.vendex.remote.RestClient(com.vendex.remote.ApiConfig.baseUrl() + "/health");

    private OfflineModeManager() {}

    public synchronized void iniciarMonitoreo() {
        if (monitoreando) return;
        monitoreando = true;
        monitor.scheduleAtFixedRate(this::verificarConectividad, 0, MONITOREO_SEGUNDOS, TimeUnit.SECONDS);
    }

    public synchronized void detenerMonitoreo() {
        monitoreando = false;
        monitor.shutdownNow();
    }

    public ModoOperacion getModo() {
        return modo.get();
    }

    public boolean isOnline() {
        return modo.get() == ModoOperacion.ONLINE || modo.get() == ModoOperacion.SYNCING;
    }

    public synchronized void setModo(ModoOperacion nuevoModo) {
        modo.set(nuevoModo);
    }

    private void verificarConectividad() {
        try {
            int code = healthClient.status("");
            if (code >= 200 && code < 300) {
                fallosConsecutivos = 0;
                if (modo.get() == ModoOperacion.OFFLINE) {
                    modo.set(ModoOperacion.SYNCING);
                }
            } else {
                registrarFallo();
            }
        } catch (Exception e) {
            registrarFallo();
        }
    }

    private void registrarFallo() {
        fallosConsecutivos++;
        if (fallosConsecutivos >= MAX_FALLOS && modo.get() == ModoOperacion.ONLINE) {
            modo.set(ModoOperacion.OFFLINE);
        }
    }
}
