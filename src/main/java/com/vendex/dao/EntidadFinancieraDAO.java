package com.vendex.dao;

import com.vendex.model.EntidadFinanciera;
import java.util.List;

public interface EntidadFinancieraDAO {

    int insertar(EntidadFinanciera e);

    void actualizar(EntidadFinanciera e);

    void desactivar(int id);

    List<EntidadFinanciera> listar(boolean soloActivas);

    EntidadFinanciera obtenerPorId(int id);

}
