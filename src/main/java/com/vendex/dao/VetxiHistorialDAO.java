package com.vendex.dao;

import com.vendex.model.VetxiHistorial.*;
import java.util.List;

public interface VetxiHistorialDAO {
    void guardar(Ambito ambito, Mensaje mensaje) throws Exception;
    List<Conversacion> conversaciones(Ambito ambito, int limite) throws Exception;
    List<Mensaje> mensajes(Ambito ambito, String conversacionId, int limite) throws Exception;
}
