package com.vendex.dao;

import com.vendex.model.LicenciaEmitida;
import java.util.List;
import java.util.Optional;

/** Fase 6b: persistencia de la administración central de licencias. */
public interface LicenciaCentralDAO {
    void insertarEmitida(LicenciaEmitida e);
    List<LicenciaEmitida> listarEmitidas(int limite);
    Optional<LicenciaEmitida> buscarPorClaveHash(String claveHash);
    void revocar(String machineCode, String motivo, Integer usuarioId);
    boolean estaRevocada(String machineCode);
    List<String> listarRevocadas(int limite);
}
