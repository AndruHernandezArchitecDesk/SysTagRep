package com.vendex.dao;

import com.vendex.model.Prestamo;
import com.vendex.model.PrestamoCuota;
import com.vendex.model.PrestamoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PrestamoDAO {

    int insertar(Prestamo p);

    void insertarCuotas(List<PrestamoCuota> cuotas);

    List<Object[]> listarActivos();

    Prestamo obtenerPorId(int id);

    List<PrestamoCuota> listarCuotas(int prestamoId);

    List<PrestamoPago> listarPagos(int prestamoId);

    /**
     * Registra un abono transaccional: inserta el pago, aplica a cuotas en orden
     * cronológico (interés primero, luego capital) y cierra el préstamo si saldo = 0.
     * @return monto efectivamente aplicado (puede ser menor si no hay saldo pendiente)
     */
    BigDecimal registrarAbono(int prestamoId, BigDecimal monto, Integer cuotaIdPreferida,
                              String fuente, Integer referenciaId, Integer usuarioId, String notas);

    List<Object[]> listarCuotasPendientes();

    List<Prestamo> listarPrestamosConMeta();

    void actualizarMeta(int prestamoId, String metaPeriodicidad, BigDecimal metaMonto);

    void actualizarEstado(int prestamoId, String estado);

}
