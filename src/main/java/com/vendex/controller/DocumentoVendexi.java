package com.vendex.controller;

import com.vendex.model.Inventario;
import com.vendex.service.VendexiSugerenciaService;
import java.util.function.Consumer;

/** Contexto del formulario activo, conectado por MainController al único asistente. Hilo FX. */
public interface DocumentoVendexi {
    VendexiSugerenciaService.Contexto contextoVendexi();
    void conectarVendexi(ChatWidgetController asistente);

    /** null para documentos que no admiten agregar referencias ajenas (nota de crédito). */
    default Consumer<Inventario> accionAgregarVendexi() { return null; }
}
