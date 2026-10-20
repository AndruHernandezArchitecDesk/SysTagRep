package com.vendex.dao;

import com.vendex.model.VendexiConsumo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Fase 3 Vendexi — persistencia de medición de consumo, periodo y sesiones. */
public interface VendexiConsumoDAO {

    void insertar(VendexiConsumo consumo);

    /** Suma de tokens (entrada+salida) registrados desde el inicio del periodo. */
    long sumarTokensDesde(LocalDateTime desde);

    /** Suma del costo estimado registrado desde el inicio del periodo. */
    BigDecimal sumarCostoDesde(LocalDateTime desde);

    List<VendexiConsumo> listarReciente(int limite);

    LocalDateTime obtenerInicioPeriodo();

    void actualizarInicioPeriodo(LocalDateTime inicio);

    long iniciarSesion(String modulo, String tipo);

    void finalizarSesion(long idSesion);
}
