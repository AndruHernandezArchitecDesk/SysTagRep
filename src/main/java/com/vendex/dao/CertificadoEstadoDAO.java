package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface CertificadoEstadoDAO {

    void guardarEstado(String rutaP12, String titular, LocalDate emision, LocalDate expiracion, int dias, String nivel);

    Timestamp obtenerUltimoEmailEnviado(String rutaP12);

    void actualizarUltimoEmailEnviado(String rutaP12);

    boolean debeEnviarCorreo(String rutaP12, String nivel, Timestamp ultimoEnvio);

}
