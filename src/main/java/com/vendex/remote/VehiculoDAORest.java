package com.vendex.remote;

import com.vendex.dao.VehiculoDAO;
import com.vendex.model.Vehiculo;
import java.sql.Connection;
import java.lang.Integer;
import java.util.List;
import java.util.Optional;
import java.lang.String;

public class VehiculoDAORest extends RestDao<Vehiculo> implements VehiculoDAO {
    public VehiculoDAORest(RestClient rest) { super(rest, "/vehiculos", Vehiculo.class); }
    public List<Vehiculo> listar() { throw new UnsupportedOperationException("Vehiculo.listar rest"); }
    public List<String> listarMarcas() { throw new UnsupportedOperationException("Vehiculo.listarMarcas rest"); }
    public List<String> listarModelosPorMarca(String marca) { throw new UnsupportedOperationException("Vehiculo.listarModelosPorMarca rest"); }
    public List<Integer> listarAniosPorModelo(String marca, String modelo) { throw new UnsupportedOperationException("Vehiculo.listarAniosPorModelo rest"); }
    public Optional<Vehiculo> obtenerPorId(int id) { throw new UnsupportedOperationException("Vehiculo.obtenerPorId rest"); }
    public Optional<Vehiculo> buscarPorVin(String vin17) { throw new UnsupportedOperationException("Vehiculo.buscarPorVin rest"); }
    public int guardar(Vehiculo v) { throw new UnsupportedOperationException("Vehiculo.guardar rest"); }
    public int guardar(Connection con, Vehiculo v) { throw new UnsupportedOperationException("Vehiculo.guardar rest"); }
    public void actualizar(Vehiculo v) { throw new UnsupportedOperationException("Vehiculo.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Vehiculo.eliminar rest"); }
    public List<Vehiculo> buscar(String marca, String modelo, Integer anio) { throw new UnsupportedOperationException("Vehiculo.buscar rest"); }
}