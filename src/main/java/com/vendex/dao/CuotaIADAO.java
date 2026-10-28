package com.vendex.dao;

import com.vendex.model.CuotaIA.*;

public interface CuotaIADAO {
    Permiso reservar(Reserva reserva) throws Exception;
    void finalizar(Fin fin) throws Exception;
    Panel panel() throws Exception;
    void guardar(Config config) throws Exception;
}
