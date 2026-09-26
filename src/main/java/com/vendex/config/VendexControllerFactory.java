package com.vendex.config;

import com.vendex.controller.*;
import javafx.util.Callback;

/**
 * Factory para FXMLLoader — resuelve controllers con dependencias inyectadas desde AppContext.
 * Centraliza el cableado JavaFX (hoy 43 controllers sin DI).
 */
public class VendexControllerFactory implements Callback<Class<?>, Object> {

    private final AppContext ctx;

    public VendexControllerFactory(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Object call(Class<?> type) {
        // Transición dual: por ahora todos los controllers usan constructor vacío (new DAOPostgres dentro).
        // Cuando un controller migre a inyección (ej. new NotaCreditoController(ctx.notaCreditoService)),
        // añadir aquí su rama antes del fallback.
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Controller no registrado en factory y sin constructor vacío: " + type.getName(), e);
        }
    }
}
