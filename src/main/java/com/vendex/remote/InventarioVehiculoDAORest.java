package com.vendex.remote;

import com.vendex.dao.InventarioVehiculoDAO;
import java.sql.Connection;
import java.lang.Integer;
import java.util.List;

public class InventarioVehiculoDAORest implements InventarioVehiculoDAO {
    private final RestClient rest;
    public InventarioVehiculoDAORest(RestClient rest) { this.rest = rest; }
    public void asociar(int inventarioId, int vehiculoId) { try { rest.post("/inventario-vehiculo/asociar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void asociar(Connection con, int inventarioId, int vehiculoId) { try { rest.post("/inventario-vehiculo/asociar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void asociarMultiple(int inventarioId, List vehiculoIds) { try { rest.post("/inventario-vehiculo/asociarMultiple", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void limpiarPorInventario(int inventarioId) { try { rest.post("/inventario-vehiculo/limpiarPorInventario", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void limpiarPorInventario(Connection con, int inventarioId) { try { rest.post("/inventario-vehiculo/limpiarPorInventario", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public List<Integer> listarVehiculoIdsPorInventario(int inventarioId) { throw new UnsupportedOperationException("InventarioVehiculo.listarVehiculoIdsPorInventario"); }
    public List<Integer> listarInventarioIdsPorVehiculo(int vehiculoId) { throw new UnsupportedOperationException("InventarioVehiculo.listarInventarioIdsPorVehiculo"); }
    public List<com.vendex.model.Inventario> listarPorVehiculo(int vehiculoId, int sucursalId) { throw new UnsupportedOperationException("InventarioVehiculo.listarPorVehiculo"); }
    public List<com.vendex.model.Vehiculo> listarVehiculosPorInventario(int inventarioId) { throw new UnsupportedOperationException("InventarioVehiculo.listarVehiculosPorInventario"); }
}