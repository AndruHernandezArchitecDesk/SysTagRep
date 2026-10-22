package com.vendex.remote;

import com.vendex.dao.PrestamoDAO;
import com.vendex.model.Prestamo;
import com.vendex.model.PrestamoCuota;
import com.vendex.model.PrestamoPago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación REST parcial del módulo de préstamos (modo multi-sucursal remoto).
 * Las escrituras aún no tienen endpoint central: se lanza UnsupportedOperationException
 * (mismo criterio que CuentaPorPagarDAORest).
 */
public class PrestamoDAORest extends RestDao<Prestamo> implements PrestamoDAO {

    public PrestamoDAORest(RestClient rest) { super(rest, "/prestamos", Prestamo.class); }

    @Override
    public List<Object[]> listarActivos() {
        try {
            return rest.get("/prestamos/activos", new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Object[]> listarCuotasPendientes() {
        try {
            return rest.get("/prestamos/cuotas-pendientes", new com.fasterxml.jackson.core.type.TypeReference<List<Object[]>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Prestamo obtenerPorId(int id) {
        try {
            return rest.get("/prestamos/" + id, Prestamo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PrestamoCuota> listarCuotas(int prestamoId) {
        try {
            return rest.get("/prestamos/" + prestamoId + "/cuotas", new com.fasterxml.jackson.core.type.TypeReference<List<PrestamoCuota>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PrestamoPago> listarPagos(int prestamoId) {
        try {
            return rest.get("/prestamos/" + prestamoId + "/pagos", new com.fasterxml.jackson.core.type.TypeReference<List<PrestamoPago>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Prestamo> listarPrestamosConMeta() {
        try {
            return rest.get("/prestamos/con-meta", new com.fasterxml.jackson.core.type.TypeReference<List<Prestamo>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int insertar(Prestamo p) { throw new UnsupportedOperationException("Prestamo.insertar rest"); }

    @Override
    public void insertarCuotas(List<PrestamoCuota> cuotas) { throw new UnsupportedOperationException("Prestamo.insertarCuotas rest"); }

    @Override
    public BigDecimal registrarAbono(int prestamoId, BigDecimal monto, Integer cuotaIdPreferida,
                                     String fuente, Integer referenciaId, Integer usuarioId, String notas) {
        throw new UnsupportedOperationException("Prestamo.registrarAbono rest");
    }

    @Override
    public void actualizarMeta(int prestamoId, String metaPeriodicidad, BigDecimal metaMonto) {
        throw new UnsupportedOperationException("Prestamo.actualizarMeta rest");
    }

    @Override
    public void actualizarEstado(int prestamoId, String estado) {
        throw new UnsupportedOperationException("Prestamo.actualizarEstado rest");
    }
}
