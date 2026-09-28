package com.vendex.remote;

import com.vendex.dao.UbicacionPerchaDAO;
import com.vendex.model.UbicacionPercha;
import java.util.List;

public class UbicacionPerchaDAORest extends RestDao<UbicacionPercha> implements UbicacionPerchaDAO {
    public UbicacionPerchaDAORest(RestClient rest) { super(rest, "/ubicacion-percha", UbicacionPercha.class); }
    public void guardar(UbicacionPercha u) { throw new UnsupportedOperationException("UbicacionPercha.guardar rest"); }
    public void actualizar(UbicacionPercha u) { throw new UnsupportedOperationException("UbicacionPercha.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("UbicacionPercha.eliminar rest"); }
    public List<UbicacionPercha> listar() { throw new UnsupportedOperationException("UbicacionPercha.listar rest"); }
}