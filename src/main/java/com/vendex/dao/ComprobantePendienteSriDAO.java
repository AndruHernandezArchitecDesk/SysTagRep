package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.ComprobantePendienteSri;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ComprobantePendienteSriDAO {

    void encolar(String tipoComprobante, String claveAcceso, String numeroComprobante, String ambiente, String mensaje);

    void encolarFactura(String clave, String numero, String ambiente);

    void encolar(String tipoComprobante, String claveAcceso, String numeroComprobante, String ambiente);

    List<ComprobantePendienteSri> listarParaReintentar(int limite);

    List<ComprobantePendienteSri> listarTodos(int limite);

    int contarPendientes();

    void marcarResultado(int id, String estado, String mensaje, LocalDateTime proximoIntento, int intentos);

    void marcarReintento(int id, int intentos, LocalDateTime proximo, String mensaje);

    void forzarReintentoAhora(String clave);

    /** Anulación local atómica: solo cambia colas aún PENDIENTE o AGOTADA. */
    default boolean anularLocal(String claveAcceso, String mensaje) {
        throw new UnsupportedOperationException("Anulación local SRI no disponible en este DAO");
    }

    /** Revalida la fila al tomar el lock por clave para descartar listas obsoletas. */
    default boolean siguePendiente(int id) { return true; }

}
