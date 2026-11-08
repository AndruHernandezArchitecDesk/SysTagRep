package com.vendex.util;

import javafx.application.Platform;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Refresca las vistas abiertas solo después de confirmar cambios de inventario. */
public final class InventarioEventos {
    private static final CopyOnWriteArrayList<WeakReference<Runnable>> OYENTES = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean PENDIENTE = new AtomicBoolean();

    private InventarioEventos() { }

    /** La vista conserva su Runnable; el bus no retiene controllers cerrados. */
    public static void escuchar(Runnable oyente) {
        OYENTES.add(new WeakReference<>(oyente));
    }

    public static void notificar() {
        if (!PENDIENTE.compareAndSet(false, true)) return;
        try {
            Platform.runLater(() -> {
                PENDIENTE.set(false);
                for (WeakReference<Runnable> referencia : OYENTES) {
                    Runnable oyente = referencia.get();
                    if (oyente == null) OYENTES.remove(referencia);
                    else {
                        try { oyente.run(); }
                        catch (RuntimeException e) {
                            java.util.logging.Logger.getLogger(InventarioEventos.class.getName())
                                    .log(java.util.logging.Level.WARNING, "No se pudo refrescar una vista de inventario", e);
                        }
                    }
                }
            });
        } catch (IllegalStateException sinInterfaz) {
            // El mismo servicio funciona en servidor headless y pruebas de BD.
            PENDIENTE.set(false);
        }
    }
}
